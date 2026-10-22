package com.volcanicmg.mapcolonies.mixin;

import com.volcanicmg.mapcolonies.client.xaero.WorldMapColonyHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.highlight.HighlighterRegistry;

/** Xaero only registers its own highlighters; add ours just before the per-session registry is frozen. */
@Mixin(value = HighlighterRegistry.class, remap = false)
public abstract class WorldMapRegistryMixin {
    @Inject(method = "end", at = @At("HEAD"))
    private void mapcolonies$register(CallbackInfo ci) {
        ((HighlighterRegistry) (Object) this).register(new WorldMapColonyHighlighter());
    }
}
