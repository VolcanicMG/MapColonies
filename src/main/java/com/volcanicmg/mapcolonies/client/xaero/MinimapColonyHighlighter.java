package com.volcanicmg.mapcolonies.client.xaero;

import com.volcanicmg.mapcolonies.client.ClientColonies;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.highlight.ChunkHighlighter;
import xaero.hud.minimap.info.render.compile.InfoDisplayCompiler;

/** Colony claims and hut footprints for Xaero's Minimap. */
public final class MinimapColonyHighlighter extends ChunkHighlighter {
    public MinimapColonyHighlighter() {
        super(true);
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dim, int rx, int rz) {
        return ClientColonies.regionHasHighlights(dim.location(), rx, rz, false);
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dim, int cx, int cz) {
        return ClientColonies.chunkIsHighlit(dim.location(), cx, cz, false);
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dim, int cx, int cz) {
        return ClientColonies.claimColors(resultStore, dim.location(), cx, cz, false) ? resultStore : null;
    }

    @Override
    public int[] getChunkHighlitColor(ResourceKey<Level> dim, int cx, int cz) {
        return ClientColonies.overlayBuildings(super.getChunkHighlitColor(dim, cx, cz), resultStore,
                dim.location(), cx, cz, false);
    }

    @Override
    public void addBlockHighlightTooltips(InfoDisplayCompiler compiler, ResourceKey<Level> dim, int x, int z, int width) {
        Component c = ClientColonies.tooltip(dim.location(), x, z, false);
        if (c != null) compiler.addLine(c);
    }

    @Override
    public void addChunkHighlightTooltips(InfoDisplayCompiler compiler, ResourceKey<Level> dim, int cx, int cz, int width) {
        // Unused: addBlockHighlightTooltips is overridden for per-block hut labels.
    }
}
