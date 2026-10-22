package com.volcanicmg.mapcolonies.client;

import com.volcanicmg.mapcolonies.Config;
import com.volcanicmg.mapcolonies.MapColonies;
import com.volcanicmg.mapcolonies.client.xaero.MinimapElements;
import com.volcanicmg.mapcolonies.client.xaero.WorldMapElements;
import com.volcanicmg.mapcolonies.client.xaero.XaeroSettingsScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only entrypoint: client config and its screen, config hot-reload, Xaero element registration,
 * the settings button on Xaero's screens, clearing on disconnect.
 */
@Mod(value = MapColonies.MODID, dist = Dist.CLIENT)
public final class MapColoniesClient {
    private static boolean worldMapRegistered;
    private static boolean minimapRegistered;

    public MapColoniesClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        // Mods list > MapColonies > Config.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // Reloading fires on the config watcher thread; the index is main-thread state.
        modBus.addListener(ModConfigEvent.Reloading.class, e -> {
            if (e.getConfig().getSpec() == Config.CLIENT_SPEC) Minecraft.getInstance().execute(ClientColonies::rebuild);
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> ClientColonies.clear());
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> registerElements());
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, e -> {
            if (isXaeroSettings(e.getScreen())) ConfigButton.add(e, container);
        });
    }

    private static boolean isXaeroSettings(Screen screen) {
        return ModList.get().isLoaded("xaerominimap") && XaeroSettingsScreens.Minimap.is(screen)
                || ModList.get().isLoaded("xaeroworldmap") && XaeroSettingsScreens.WorldMap.is(screen);
    }

    // Xaero builds its element handlers late in its own setup, so poll until they exist, then add ours once.
    private static void registerElements() {
        if (!worldMapRegistered)
            worldMapRegistered = !ModList.get().isLoaded("xaeroworldmap") || WorldMapElements.tryRegister();
        if (!minimapRegistered)
            minimapRegistered = !ModList.get().isLoaded("xaerominimap") || MinimapElements.tryRegister();
    }
}
