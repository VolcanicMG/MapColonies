package com.volcanicmg.mapcolonies.client.xaero;

import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;

/** Asks Xaero's Minimap to redraw its chunk highlights (it has no hash cache, only a refresh flag). */
public final class MinimapRefresh {
    private MinimapRefresh() {
    }

    public static void run() {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        MinimapProcessor processor = session == null ? null : session.getProcessor();
        if (processor == null || processor.getMinimapWriter() == null) return;
        DimensionHighlighterHandler handler = processor.getMinimapWriter().getDimensionHighlightHandler();
        if (handler != null) handler.requestRefresh();
    }
}
