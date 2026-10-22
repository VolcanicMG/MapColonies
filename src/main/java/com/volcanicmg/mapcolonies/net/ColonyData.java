package com.volcanicmg.mapcolonies.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * One colony as seen by one player: claimed chunks plus the huts they may see.
 *
 * @param dim       dimension id
 * @param id        MineColonies colony id (unique per dimension)
 * @param name      colony name
 * @param color     team colour as 0xRRGGBB
 * @param member    whether the receiving player is a colony member
 * @param chunks    claimed chunks as sorted {@code ChunkPos.toLong()} values
 * @param buildings huts; empty when the server withholds them from this player
 */
public record ColonyData(ResourceLocation dim, int id, String name, int color, boolean member, long[] chunks,
                         List<Building> buildings) {

    /**
     * One hut.
     *
     * @param pos      hut block position as {@code BlockPos.asLong()}
     * @param type     building type registry id, e.g. {@code minecolonies:builder}
     * @param name     custom name, or the type's translation key
     * @param level    current level (0 = placed, not built)
     * @param maxLevel max level for this type
     */
    public record Building(long pos, ResourceLocation type, String name, int level, int maxLevel,
                           int minX, int minZ, int maxX, int maxZ) {
        void write(FriendlyByteBuf buf) {
            buf.writeLong(pos);
            buf.writeResourceLocation(type);
            buf.writeUtf(name);
            buf.writeVarInt(level);
            buf.writeVarInt(maxLevel);
            buf.writeInt(minX);
            buf.writeInt(minZ);
            buf.writeInt(maxX);
            buf.writeInt(maxZ);
        }

        static Building read(FriendlyByteBuf buf) {
            return new Building(buf.readLong(), buf.readResourceLocation(), buf.readUtf(), buf.readVarInt(),
                    buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
        }
    }

    /** @return the key this colony is diffed and stored under */
    public ColonyKey key() {
        return new ColonyKey(dim, id);
    }

    /** @return rough wire weight in longs, used to split packets */
    public int weight() {
        return chunks.length + buildings.size() * 12;
    }

    void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dim);
        buf.writeVarInt(id);
        buf.writeUtf(name);
        buf.writeInt(color);
        buf.writeBoolean(member);
        buf.writeLongArray(chunks);
        buf.writeVarInt(buildings.size());
        buildings.forEach(b -> b.write(buf));
    }

    static ColonyData read(FriendlyByteBuf buf) {
        ResourceLocation dim = buf.readResourceLocation();
        int id = buf.readVarInt();
        String name = buf.readUtf();
        int color = buf.readInt();
        boolean member = buf.readBoolean();
        long[] chunks = buf.readLongArray();
        int n = buf.readVarInt();
        List<Building> buildings = new ArrayList<>(Math.min(n, 4096));
        for (int i = 0; i < n; i++) buildings.add(Building.read(buf));
        return new ColonyData(dim, id, name, color, member, chunks, buildings);
    }

    // Records compare arrays by reference; the diff needs content equality.
    @Override
    public boolean equals(Object o) {
        return o instanceof ColonyData c && id == c.id && color == c.color && member == c.member
                && dim.equals(c.dim) && name.equals(c.name) && Arrays.equals(chunks, c.chunks)
                && buildings.equals(c.buildings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dim, id, name, color, member, Arrays.hashCode(chunks), buildings);
    }
}
