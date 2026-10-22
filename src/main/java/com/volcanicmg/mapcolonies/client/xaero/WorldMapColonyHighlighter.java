package com.volcanicmg.mapcolonies.client.xaero;

import com.volcanicmg.mapcolonies.client.ClientColonies;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.highlight.ChunkHighlighter;

/** Colony claims and hut footprints for Xaero's World Map. */
public final class WorldMapColonyHighlighter extends ChunkHighlighter {
    public WorldMapColonyHighlighter() {
        super(true);
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dim, int rx, int rz) {
        return ClientColonies.regionHash(dim.location(), rx, rz, true);
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dim, int rx, int rz) {
        return ClientColonies.regionHasHighlights(dim.location(), rx, rz, true);
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dim, int cx, int cz) {
        return ClientColonies.chunkIsHighlit(dim.location(), cx, cz, true);
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dim, int cx, int cz) {
        return ClientColonies.claimColors(resultStore, dim.location(), cx, cz, true) ? resultStore : null;
    }

    @Override
    public int[] getChunkHighlitColor(ResourceKey<Level> dim, int cx, int cz) {
        return ClientColonies.overlayBuildings(super.getChunkHighlitColor(dim, cx, cz), resultStore,
                dim.location(), cx, cz, true);
    }

    // Block-level overrides so hovering a hut shows its label instead of the colony name.
    @Override
    public Component getBlockHighlightSubtleTooltip(ResourceKey<Level> dim, int x, int z) {
        return ClientColonies.tooltip(dim.location(), x, z, true);
    }

    @Override
    public Component getBlockHighlightBluntTooltip(ResourceKey<Level> dim, int x, int z) {
        return ClientColonies.tooltip(dim.location(), x, z, true);
    }

    @Override
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dim, int cx, int cz) {
        return ClientColonies.tooltip(dim.location(), (cx << 4) + 8, (cz << 4) + 8, true);
    }

    @Override
    public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dim, int cx, int cz) {
        return getChunkHighlightSubtleTooltip(dim, cx, cz);
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> list, ResourceKey<Level> dim, int x, int z, int width) {
        Component c = ClientColonies.tooltip(dim.location(), x, z, false);
        if (c != null) list.add(c);
    }
}
