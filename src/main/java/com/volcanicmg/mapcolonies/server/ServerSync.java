package com.volcanicmg.mapcolonies.server;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.claim.IChunkClaimData;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.colony.permissions.Rank;
import com.mojang.logging.LogUtils;
import com.volcanicmg.mapcolonies.Config;
import com.volcanicmg.mapcolonies.net.ColonyData;
import com.volcanicmg.mapcolonies.net.ColonyKey;
import com.volcanicmg.mapcolonies.net.SyncPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/**
 * Server-authoritative colony sync. Every interval it snapshots all colonies once, builds each player's view
 * (membership-scoped) and sends only what changed since that player's last packet.
 */
public final class ServerSync {
    private static final Logger LOG = LogUtils.getLogger();
    // ~480 KB of longs; vanilla caps a clientbound custom payload at 1 MiB.
    private static final int MAX_WEIGHT_PER_PACKET = 60_000;

    private static final Map<UUID, Map<ColonyKey, ColonyData>> SENT = new HashMap<>();
    private static int ticks;
    private static boolean failureLogged;

    /** Colony state shared by every player's view this cycle. */
    private record Snapshot(IColony colony, ColonyKey key, String name, int color, long[] chunks,
                            List<ColonyData.Building> buildings) {
    }

    private ServerSync() {
    }

    public static void onTick(ServerTickEvent.Post event) {
        if (++ticks < Config.SYNC_INTERVAL_SECONDS.get() * 20) return;
        ticks = 0;
        List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        guarded(() -> {
            List<Snapshot> snap = snapshot();
            for (ServerPlayer p : players) sync(p, snap, false);
        });
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) guarded(() -> sync(p, snapshot(), true));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    // A MineColonies hiccup or API drift must never take the server tick down with it.
    private static void guarded(Runnable r) {
        try {
            r.run();
            failureLogged = false;
        } catch (RuntimeException | LinkageError e) {
            if (!failureLogged) LOG.error("MapColonies sync failed; will keep retrying quietly", e);
            failureLogged = true;
        }
    }

    // isColonyMember() counts any listed player, Hostile and Neutral ranks included.
    private static boolean isMember(IColony colony, ServerPlayer player) {
        IPermissions perms = colony.getPermissions();
        Rank rank = perms.getRank(player);
        return rank != null && !rank.isHostile() && !rank.equals(perms.getRankNeutral());
    }

    private static List<Snapshot> snapshot() {
        IColonyManager manager = IColonyManager.getInstance();
        List<IColony> colonies = manager.getAllColonies();
        Map<ResourceKey<Level>, Int2ObjectOpenHashMap<LongArrayList>> claims = new HashMap<>();
        Set<ResourceKey<Level>> dims = new HashSet<>();
        for (IColony c : colonies) dims.add(c.getDimension());
        for (ResourceKey<Level> dim : dims) {
            Int2ObjectOpenHashMap<LongArrayList> byColony = new Int2ObjectOpenHashMap<>();
            for (Map.Entry<ChunkPos, IChunkClaimData> e : manager.getClaimData(dim).entrySet()) {
                int owner = e.getValue().getOwningColony();
                if (owner > 0) byColony.computeIfAbsent(owner, k -> new LongArrayList()).add(e.getKey().toLong());
            }
            claims.put(dim, byColony);
        }

        List<Snapshot> out = new ArrayList<>(colonies.size());
        for (IColony c : colonies) {
            LongArrayList list = claims.get(c.getDimension()).get(c.getID());
            long[] chunks = list == null ? new long[0] : list.toLongArray();
            Arrays.sort(chunks);
            ChatFormatting team = c.getTeamColonyColor();
            Integer rgb = team == null ? null : team.getColor();
            out.add(new Snapshot(c, new ColonyKey(c.getDimension().location(), c.getID()),
                    Objects.requireNonNullElse(c.getName(), "Colony"), rgb == null ? 0xFFFFFF : rgb, chunks,
                    buildings(c)));
        }
        return out;
    }

    private static List<ColonyData.Building> buildings(IColony colony) {
        List<ColonyData.Building> out = new ArrayList<>();
        for (IBuilding b : colony.getServerBuildingManager().getBuildings().values()) {
            if (b.getBuildingType() == null) continue;
            BlockPos pos = b.getPosition();
            Tuple<BlockPos, BlockPos> corners = b.getCorners();
            BlockPos a = corners == null ? pos : corners.getA();
            BlockPos z = corners == null ? pos : corners.getB();
            out.add(new ColonyData.Building(pos.asLong(), b.getBuildingType().getRegistryName(),
                    Objects.requireNonNullElse(b.getBuildingDisplayName(), ""), b.getBuildingLevel(),
                    b.getMaxBuildingLevel(), Math.min(a.getX(), z.getX()), Math.min(a.getZ(), z.getZ()),
                    Math.max(a.getX(), z.getX()), Math.max(a.getZ(), z.getZ())));
        }
        // Manager map order is not stable; sort so the diff only fires on real changes.
        out.sort(Comparator.comparingLong(ColonyData.Building::pos));
        return out;
    }

    private static void sync(ServerPlayer player, List<Snapshot> snap, boolean full) {
        // Optional channel: clients without the mod just get nothing.
        if (!player.connection.hasChannel(SyncPayload.TYPE)) return;
        boolean showAll = Config.SHOW_ALL_COLONIES.get();
        boolean otherBuildings = Config.SHOW_OTHER_COLONIES_BUILDINGS.get();
        Map<ColonyKey, ColonyData> now = new HashMap<>();
        for (Snapshot s : snap) {
            boolean member = isMember(s.colony(), player);
            if (!member && !showAll) continue;
            now.put(s.key(), new ColonyData(s.key().dim(), s.key().id(), s.name(), s.color(), member, s.chunks(),
                    member || otherBuildings ? s.buildings() : List.of()));
        }

        Map<ColonyKey, ColonyData> prev = full ? Map.of() : SENT.getOrDefault(player.getUUID(), Map.of());
        List<ColonyData> upserts = new ArrayList<>();
        for (ColonyData c : now.values()) if (!c.equals(prev.get(c.key()))) upserts.add(c);
        List<ColonyKey> removals = new ArrayList<>();
        for (ColonyKey k : prev.keySet()) if (!now.containsKey(k)) removals.add(k);
        SENT.put(player.getUUID(), now);
        if (!full && upserts.isEmpty() && removals.isEmpty()) return;

        // ponytail: splits per colony, so one colony over the cap still goes as a single packet.
        List<ColonyData> batch = new ArrayList<>();
        int weight = 0;
        boolean first = true;
        for (ColonyData c : upserts) {
            if (!batch.isEmpty() && weight + c.weight() > MAX_WEIGHT_PER_PACKET) {
                PacketDistributor.sendToPlayer(player, new SyncPayload(first && full, batch, first ? removals : List.of()));
                first = false;
                batch = new ArrayList<>();
                weight = 0;
            }
            batch.add(c);
            weight += c.weight();
        }
        PacketDistributor.sendToPlayer(player, new SyncPayload(first && full, batch, first ? removals : List.of()));
    }
}
