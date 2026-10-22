package com.volcanicmg.mapcolonies.client;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Resolves a building type id to its hut block item via MineColonies' building registry. Main thread only. */
final class HutIcons {
    private static final Map<ResourceLocation, ItemStack> CACHE = new HashMap<>();

    private HutIcons() {
    }

    /** @return the hut item, or {@link ItemStack#EMPTY} if the type or its block is unknown */
    static ItemStack get(ResourceLocation type) {
        return CACHE.computeIfAbsent(type, t -> {
            BuildingEntry entry = IMinecoloniesAPI.getInstance().getBuildingRegistry().get(t);
            if (entry == null || entry.getBuildingBlock() == null) return ItemStack.EMPTY;
            return new ItemStack(entry.getBuildingBlock().asItem());
        });
    }
}
