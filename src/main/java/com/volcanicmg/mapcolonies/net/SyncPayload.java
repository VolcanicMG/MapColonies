package com.volcanicmg.mapcolonies.net;

import com.volcanicmg.mapcolonies.MapColonies;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client colony sync.
 *
 * @param full      client drops everything it knows before applying this packet
 * @param upserts   colonies added or changed
 * @param removals  colonies the player should no longer see
 */
public record SyncPayload(boolean full, List<ColonyData> upserts, List<ColonyKey> removals)
        implements CustomPacketPayload {

    public static final Type<SyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MapColonies.MODID, "sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC =
            CustomPacketPayload.codec(SyncPayload::write, SyncPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(full);
        buf.writeVarInt(upserts.size());
        upserts.forEach(c -> c.write(buf));
        buf.writeVarInt(removals.size());
        removals.forEach(k -> {
            buf.writeResourceLocation(k.dim());
            buf.writeVarInt(k.id());
        });
    }

    private static SyncPayload read(FriendlyByteBuf buf) {
        boolean full = buf.readBoolean();
        int n = buf.readVarInt();
        List<ColonyData> upserts = new ArrayList<>(Math.min(n, 1024));
        for (int i = 0; i < n; i++) upserts.add(ColonyData.read(buf));
        int r = buf.readVarInt();
        List<ColonyKey> removals = new ArrayList<>(Math.min(r, 1024));
        for (int i = 0; i < r; i++) removals.add(new ColonyKey(buf.readResourceLocation(), buf.readVarInt()));
        return new SyncPayload(full, upserts, removals);
    }

    @Override
    public Type<SyncPayload> type() {
        return TYPE;
    }
}
