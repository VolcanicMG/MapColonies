package com.volcanicmg.mapcolonies.net;

import net.minecraft.resources.ResourceLocation;

/** Colony identity: MineColonies ids are only unique within a dimension. */
public record ColonyKey(ResourceLocation dim, int id) {
}
