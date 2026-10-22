package com.volcanicmg.mapcolonies.client.xaero;

import net.minecraft.client.gui.screens.Screen;
import xaero.common.gui.GuiMinimapMain;
import xaero.map.gui.GuiWorldMapSettings;

/**
 * Recognises Xaero's top-level settings screens. Each check lives in its own nested class so the JVM only
 * resolves the Xaero class of a mod that is actually installed.
 */
public final class XaeroSettingsScreens {
    private XaeroSettingsScreens() {
    }

    /** Minimap main settings page (what its keybind and Mods-list Config button open). */
    public static final class Minimap {
        public static boolean is(Screen screen) {
            return screen instanceof GuiMinimapMain;
        }
    }

    /** World Map settings screen (what its Mods-list Config button opens). */
    public static final class WorldMap {
        public static boolean is(Screen screen) {
            return screen instanceof GuiWorldMapSettings;
        }
    }
}
