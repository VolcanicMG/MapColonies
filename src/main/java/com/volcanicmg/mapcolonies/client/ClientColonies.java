package com.volcanicmg.mapcolonies.client;

import com.volcanicmg.mapcolonies.Config;
import com.volcanicmg.mapcolonies.client.xaero.MinimapRefresh;
import com.volcanicmg.mapcolonies.client.xaero.WorldMapRefresh;
import com.volcanicmg.mapcolonies.net.ColonyData;
import com.volcanicmg.mapcolonies.net.ColonyKey;
import com.volcanicmg.mapcolonies.net.SyncPayload;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;

/**
 * Client copy of the server's colonies plus a render index the Xaero highlighters query. Mutated on the
 * main thread only; Xaero's threads read the immutable {@link Index} through a volatile swap.
 * Colours use Xaero's highlight packing: {@code B<<24 | G<<16 | R<<8 | A}.
 */
public final class ClientColonies {
    private static final Map<ColonyKey, ColonyData> COLONIES = new HashMap<>();
    private static volatile Index index = Index.EMPTY;

    /** Pre-coloured claim shared by all chunks of one colony, so neighbour checks are a reference compare. */
    private record Claim(int fill, int border, Component name) {
    }

    /** Hut rectangle in block coords with its colours and hover label (null = no label). */
    private record Footprint(int minX, int minZ, int maxX, int maxZ, int fill, int border, Component label) {
        boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    /**
     * Hut icon drawn by the Xaero element renderers.
     *
     * @param x     footprint centre X in block coords
     * @param z     footprint centre Z
     * @param icon  the hut's block item, or empty to draw a lettered square
     * @param letter fallback square letter (first letter of the type id)
     * @param color fallback square colour as 0xRRGGBB
     * @param label text under the icon, or null for none
     */
    public record Marker(double x, double z, ItemStack icon, String letter, int color, Component label) {
    }

    private record Dim(Long2ObjectOpenHashMap<Claim> claims, Long2IntOpenHashMap claimRegions,
                       Long2ObjectOpenHashMap<List<Footprint>> buildings, Long2IntOpenHashMap buildingRegions,
                       List<Marker> markers) {
        Dim() {
            this(new Long2ObjectOpenHashMap<>(), new Long2IntOpenHashMap(), new Long2ObjectOpenHashMap<>(),
                    new Long2IntOpenHashMap(), new ArrayList<>());
        }
    }

    /** Icon/label settings read by the element renderers each frame. */
    public record IconSettings(boolean world, boolean mini, Config.LabelMode labelWorld, Config.LabelMode labelMini,
                               double labelMinZoom, float scale, boolean background, float labelScale) {
        static final IconSettings OFF = new IconSettings(false, false, Config.LabelMode.NONE, Config.LabelMode.NONE, 0, 1,
                true, 1);
    }

    private record Index(Map<ResourceLocation, Dim> dims, boolean coloniesWorld, boolean coloniesMini,
                         boolean buildingsWorld, boolean buildingsMini, IconSettings icons, int settingsHash) {
        static final Index EMPTY = new Index(Map.of(), false, false, false, false, IconSettings.OFF, 0);

        boolean colonies(boolean worldMap) {
            return worldMap ? coloniesWorld : coloniesMini;
        }

        boolean buildings(boolean worldMap) {
            return worldMap ? buildingsWorld : buildingsMini;
        }
    }

    private ClientColonies() {
    }

    /** Applies a server packet. Runs on the client main thread (NeoForge's default handler thread). */
    public static void apply(SyncPayload payload) {
        if (payload.full()) COLONIES.clear();
        payload.removals().forEach(COLONIES::remove);
        payload.upserts().forEach(c -> COLONIES.put(c.key(), c));
        rebuild();
    }

    /** Drops everything, e.g. on disconnect. */
    public static void clear() {
        COLONIES.clear();
        rebuild();
    }

    /** Rebuilds the render index from current data and client config, then asks Xaero to redraw. */
    public static void rebuild() {
        index = build(COLONIES.values());
        if (ModList.get().isLoaded("xaeroworldmap")) WorldMapRefresh.run();
        if (ModList.get().isLoaded("xaerominimap")) MinimapRefresh.run();
    }

    private static Index build(Iterable<ColonyData> colonies) {
        if (!Config.CLIENT_SPEC.isLoaded() || !Config.ENABLED.get()) return Index.EMPTY;
        boolean outline = Config.OUTLINE_ONLY.get();
        int fillOwn = outline ? 0 : alpha(Config.FILL_OPACITY_OWN.get());
        int fillOther = outline ? 0 : alpha(Config.FILL_OPACITY_OTHER.get());
        int borderOwn = alpha(Config.BORDER_OPACITY_OWN.get());
        int borderOther = alpha(Config.BORDER_OPACITY_OTHER.get());
        boolean showBuildings = Config.SHOW_BUILDINGS.get();
        Config.Labels labels = Config.BUILDING_LABELS.get();
        boolean onlyMine = Config.ONLY_MY_COLONIES.get();
        int minLevel = Config.MIN_BUILDING_LEVEL.get();
        boolean typeColors = Config.BUILDING_COLOR_MODE.get() == Config.BuildingColor.TYPE;
        int bFill = alpha(Config.BUILDING_FILL_OPACITY.get());
        int bBorder = alpha(Config.BUILDING_BORDER_OPACITY.get());
        Set<String> hidden = new HashSet<>(Config.HIDDEN_BUILDING_TYPES.get());
        boolean icons = showBuildings && Config.BUILDING_ICONS.get();
        IconSettings iconSettings = new IconSettings(icons && Config.ICONS_ON_WORLD_MAP.get(),
                icons && Config.ICONS_ON_MINIMAP.get(), Config.LABEL_MODE_WORLD_MAP.get(),
                Config.LABEL_MODE_MINIMAP.get(), Config.WORLD_MAP_LABEL_MIN_ZOOM.get(),
                Config.ICON_SCALE.get().floatValue(), Config.ICON_BACKGROUND.get(), Config.LABEL_SCALE.get().floatValue());

        Map<ResourceLocation, Dim> dims = new HashMap<>();
        for (ColonyData c : colonies) {
            Dim d = dims.computeIfAbsent(c.dim(), k -> new Dim());
            Claim claim = new Claim(pack(c.color(), c.member() ? fillOwn : fillOther),
                    pack(c.color(), c.member() ? borderOwn : borderOther), Component.literal(c.name()));
            int seed = Objects.hash(c.id(), c.color(), c.member(), c.name());
            for (long chunk : c.chunks()) {
                d.claims().put(chunk, claim);
                int cx = ChunkPos.getX(chunk), cz = ChunkPos.getZ(chunk);
                int h = mix(chunk, seed);
                // Border pixels depend on neighbours, so edge chunks also dirty the adjacent region.
                for (long region : edgeRegions(cx, cz)) d.claimRegions().addTo(region, h);
            }
            if (!showBuildings || (onlyMine && !c.member())) continue;
            for (ColonyData.Building b : c.buildings()) {
                if (b.level() < minLevel || hidden.contains(b.type().toString()) || hidden.contains(b.type().getPath()))
                    continue;
                int rgb = typeColors ? Mth.hsvToRgb((b.type().hashCode() & 0xFFFF) / 65535f, 0.7f, 1f) : c.color();
                Component text = isTownHall(b) && labels != Config.Labels.NONE ? Component.literal(c.name())
                        : label(b, labels);
                Footprint f = new Footprint(b.minX(), b.minZ(), b.maxX(), b.maxZ(), pack(rgb, bFill),
                        pack(rgb, bBorder), text);
                if (icons) d.markers().add(new Marker((b.minX() + b.maxX() + 1) / 2.0, (b.minZ() + b.maxZ() + 1) / 2.0,
                        HutIcons.get(b.type()), letter(b), rgb, text));
                int h = Objects.hash(b, rgb);
                for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++)
                    for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                        d.buildings().computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(f);
                        d.buildingRegions().addTo(ChunkPos.asLong(cx >> 5, cz >> 5), mix(ChunkPos.asLong(cx, cz), h));
                    }
            }
        }
        int settings = Objects.hash(fillOwn, fillOther, borderOwn, borderOther, labels, onlyMine, minLevel,
                typeColors, bFill, bBorder, hidden, showBuildings);
        return new Index(dims, Config.SHOW_ON_WORLD_MAP.get(), Config.SHOW_ON_MINIMAP.get(),
                showBuildings && Config.BUILDINGS_ON_WORLD_MAP.get(),
                showBuildings && Config.BUILDINGS_ON_MINIMAP.get(), iconSettings, settings);
    }

    private static Component label(ColonyData.Building b, Config.Labels labels) {
        if (labels == Config.Labels.NONE) return null;
        // Display name is a custom name or a MineColonies translation key; translatable() handles both.
        MutableComponent name = Component.translatable(b.name());
        return labels == Config.Labels.NAME_LEVEL ? name.append(" " + b.level()) : name;
    }

    private static boolean isTownHall(ColonyData.Building b) {
        return b.type().getPath().equals("townhall");
    }

    private static String letter(ColonyData.Building b) {
        String path = b.type().getPath();
        return path.isEmpty() ? "?" : path.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }

    private static long[] edgeRegions(int cx, int cz) {
        int rx = cx >> 5, rz = cz >> 5, lx = cx & 31, lz = cz & 31;
        long[] out = new long[5];
        int n = 0;
        out[n++] = ChunkPos.asLong(rx, rz);
        if (lx == 0) out[n++] = ChunkPos.asLong(rx - 1, rz);
        if (lx == 31) out[n++] = ChunkPos.asLong(rx + 1, rz);
        if (lz == 0) out[n++] = ChunkPos.asLong(rx, rz - 1);
        if (lz == 31) out[n++] = ChunkPos.asLong(rx, rz + 1);
        return Arrays.copyOf(out, n);
    }

    private static int mix(long chunk, int seed) {
        return (int) HashCommon.mix(chunk * 31 + seed);
    }

    private static int alpha(int percent) {
        return 255 * percent / 100;
    }

    static int pack(int rgb, int alpha) {
        return (rgb & 0xFF) << 24 | (rgb >> 8 & 0xFF) << 16 | (rgb >> 16 & 0xFF) << 8 | alpha;
    }

    // ---- queries used by the Xaero highlighters (any thread) ----

    public static boolean regionHasHighlights(ResourceLocation dim, int rx, int rz, boolean worldMap) {
        Index i = index;
        Dim d = i.dims().get(dim);
        if (d == null) return false;
        long key = ChunkPos.asLong(rx, rz);
        return i.colonies(worldMap) && d.claimRegions().containsKey(key)
                || i.buildings(worldMap) && d.buildingRegions().containsKey(key);
    }

    public static int regionHash(ResourceLocation dim, int rx, int rz, boolean worldMap) {
        Index i = index;
        Dim d = i.dims().get(dim);
        if (d == null) return 0;
        long key = ChunkPos.asLong(rx, rz);
        int h = i.colonies(worldMap) ? d.claimRegions().get(key) : 0;
        h = h * 31 + (i.buildings(worldMap) ? d.buildingRegions().get(key) : 0);
        return h * 31 + i.settingsHash();
    }

    public static boolean chunkIsHighlit(ResourceLocation dim, int cx, int cz, boolean worldMap) {
        Index i = index;
        Dim d = i.dims().get(dim);
        if (d == null) return false;
        long key = ChunkPos.asLong(cx, cz);
        return i.colonies(worldMap) && d.claims().containsKey(key)
                || i.buildings(worldMap) && d.buildings().containsKey(key);
    }

    /**
     * Fills Xaero's 5-slot chunk colour array: centre, then north, east, south, west edge colours.
     *
     * @return false when the chunk has no claim to draw
     */
    public static boolean claimColors(int[] out, ResourceLocation dim, int cx, int cz, boolean worldMap) {
        Index i = index;
        Dim d = i.dims().get(dim);
        if (d == null || !i.colonies(worldMap)) return false;
        Claim c = d.claims().get(ChunkPos.asLong(cx, cz));
        if (c == null) return false;
        out[0] = c.fill();
        out[1] = d.claims().get(ChunkPos.asLong(cx, cz - 1)) == c ? c.fill() : c.border();
        out[2] = d.claims().get(ChunkPos.asLong(cx + 1, cz)) == c ? c.fill() : c.border();
        out[3] = d.claims().get(ChunkPos.asLong(cx, cz + 1)) == c ? c.fill() : c.border();
        out[4] = d.claims().get(ChunkPos.asLong(cx - 1, cz)) == c ? c.fill() : c.border();
        return true;
    }

    /**
     * Paints hut footprints over a chunk's 16x16 pixel array (index {@code z<<4 | x}).
     *
     * @param base  claim pixels from Xaero, or null when the chunk is not claimed
     * @param store the highlighter's own 256-int scratch array, used when base is null
     * @return the array to render, or null if nothing to draw
     */
    public static int[] overlayBuildings(int[] base, int[] store, ResourceLocation dim, int cx, int cz,
                                         boolean worldMap) {
        Index i = index;
        Dim d = i.dims().get(dim);
        if (d == null || !i.buildings(worldMap)) return base;
        List<Footprint> list = d.buildings().get(ChunkPos.asLong(cx, cz));
        if (list == null) return base;
        if (base == null) {
            Arrays.fill(store, 0);
            base = store;
        }
        int x0 = cx << 4, z0 = cz << 4;
        for (Footprint f : list) {
            for (int x = Math.max(f.minX(), x0); x <= Math.min(f.maxX(), x0 + 15); x++)
                for (int z = Math.max(f.minZ(), z0); z <= Math.min(f.maxZ(), z0 + 15); z++) {
                    boolean edge = x == f.minX() || x == f.maxX() || z == f.minZ() || z == f.maxZ();
                    base[(z - z0) << 4 | (x - x0)] = edge ? f.border() : f.fill();
                }
        }
        return base;
    }

    /** @return hut icons for a dimension (empty if none); the list is immutable once published */
    public static List<Marker> markers(ResourceLocation dim) {
        Dim d = index.dims().get(dim);
        return d == null ? List.of() : d.markers();
    }

    public static IconSettings iconSettings() {
        return index.icons();
    }

    /**
     * @return hover text at a block: a hut label if one is under it and that map's label mode is HOVER,
     *         else the colony name; null if none
     */
    public static Component tooltip(ResourceLocation dim, int x, int z, boolean worldMap) {
        Index i = index;
        Dim d = i.dims().get(dim);
        if (d == null) return null;
        long chunk = ChunkPos.asLong(x >> 4, z >> 4);
        Config.LabelMode mode = worldMap ? i.icons().labelWorld() : i.icons().labelMini();
        // With icons off, the per-block hut tooltip is the only label, so keep it.
        boolean iconsOn = worldMap ? i.icons().world() : i.icons().mini();
        if (i.buildings(worldMap) && (mode == Config.LabelMode.HOVER || !iconsOn)) {
            List<Footprint> list = d.buildings().get(chunk);
            if (list != null)
                for (Footprint f : list) if (f.label() != null && f.contains(x, z)) return f.label();
        }
        if (!i.colonies(worldMap)) return null;
        Claim c = d.claims().get(chunk);
        return c == null ? null : c.name();
    }
}
