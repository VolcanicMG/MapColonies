package com.volcanicmg.mapcolonies;

import com.volcanicmg.mapcolonies.client.ClientColonies;
import com.volcanicmg.mapcolonies.net.SyncPayload;
import com.volcanicmg.mapcolonies.server.ServerSync;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Common entrypoint: server config, payload registration and server-side sync. */
@Mod(MapColonies.MODID)
public final class MapColonies {
    public static final String MODID = "mapcolonies";

    public MapColonies(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        modBus.addListener(MapColonies::registerPayloads);
        NeoForge.EVENT_BUS.addListener(ServerSync::onTick);
        NeoForge.EVENT_BUS.addListener(ServerSync::onLogin);
        NeoForge.EVENT_BUS.addListener(ServerSync::onLogout);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Handler only ever runs on a client; lambda keeps ClientColonies unloaded on servers.
        event.registrar("1").optional().playToClient(SyncPayload.TYPE, SyncPayload.CODEC,
                (payload, ctx) -> ClientColonies.apply(payload));
    }
}
