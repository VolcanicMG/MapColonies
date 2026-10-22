package com.volcanicmg.mapcolonies.client.xaero;

import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.world.MapWorld;

/** Drops Xaero World Map's cached region highlight hashes so changed regions re-render. */
public final class WorldMapRefresh {
    private WorldMapRefresh() {
    }

    public static void run() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) return;
        MapProcessor processor = session.getMapProcessor();
        MapWorld world = processor == null ? null : processor.getMapWorld();
        if (world != null) world.clearAllCachedHighlightHashes();
    }
}
