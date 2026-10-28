package com.volcanicmg.mapcolonies.client.xaero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.volcanicmg.mapcolonies.Config;
import com.volcanicmg.mapcolonies.client.ClientColonies;
import com.volcanicmg.mapcolonies.client.ClientColonies.Marker;
import com.volcanicmg.mapcolonies.client.MarkerPainter;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.HudMod;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.over.MinimapElementOverMapRendererHandler;

/**
 * Hut icons and labels on Xaero's Minimap, through its over-map element renderer handler (the one tracked
 * players use). Registered with the public {@code add}, no mixin needed.
 */
public final class MinimapElements {
    // Minimap elements are drawn in minimap pixels; a full 16 px item would swamp a small map.
    private static final float BASE_SCALE = 0.6f;

    private MinimapElements() {
    }

    /** @return true once registered; false while Xaero has not built its minimap yet */
    public static boolean tryRegister() {
        HudMod mod = HudMod.INSTANCE;
        Minimap minimap = mod == null ? null : mod.getMinimap();
        MinimapElementOverMapRendererHandler handler = minimap == null ? null : minimap.getOverMapRendererHandler();
        if (handler == null) return false;
        handler.add(new Renderer());
        return true;
    }

    static final class Context {
        List<Marker> markers = List.of();
        int next;
        final MarkerPainter.Batch batch = new MarkerPainter.Batch();
    }

    static final class Renderer extends MinimapElementRenderer<Marker, Context> {
        Renderer() {
            super(new Reader(), new Provider(), new Context());
        }

        @Override
        public void preRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                              MultiTextureRenderTypeRendererProvider renderers) {
            ResourceKey<Level> dim = info.mapDimension != null ? info.mapDimension : info.renderEntityDimension;
            context.markers = ClientColonies.iconSettings().mini() && dim != null
                    ? ClientColonies.markers(dim.location()) : List.of();
        }

        @Override
        public void postRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                               MultiTextureRenderTypeRendererProvider renderers) {
            context.batch.paint(ClientColonies.iconSettings());
        }

        @Override
        public boolean renderElement(Marker m, boolean highlighted, boolean outOfBounds, double depth,
                                     float optionalScale, double partialX, double partialY,
                                     MinimapElementRenderInfo info, GuiGraphics g,
                                     MultiBufferSource.BufferSource buffers) {
            // Xaero pins out-of-range elements to the minimap edge; huts should just disappear.
            if (outOfBounds) return false;
            ClientColonies.IconSettings s = ClientColonies.iconSettings();
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(partialX, partialY, depth);
            float scale = BASE_SCALE * optionalScale * s.scale();
            pose.scale(scale, scale, 1);
            context.batch.add(g, m, s.labelMini() == Config.LabelMode.ALWAYS);
            pose.popPose();
            return false;
        }

        @Override
        public boolean shouldRender(MinimapElementRenderLocation location) {
            return location == MinimapElementRenderLocation.OVER_MINIMAP;
        }
    }

    static final class Provider extends MinimapElementRenderProvider<Marker, Context> {
        @Override
        public void begin(MinimapElementRenderLocation location, Context ctx) {
            ctx.next = 0;
        }

        @Override
        public boolean hasNext(MinimapElementRenderLocation location, Context ctx) {
            return ctx.next < ctx.markers.size();
        }

        @Override
        public Marker getNext(MinimapElementRenderLocation location, Context ctx) {
            return ctx.markers.get(ctx.next++);
        }

        @Override
        public void end(MinimapElementRenderLocation location, Context ctx) {
        }
    }

    static final class Reader extends MinimapElementReader<Marker, Context> {
        @Override
        public boolean isHidden(Marker m, Context ctx) {
            return false;
        }

        @Override
        public double getRenderX(Marker m, Context ctx, float partialTicks) {
            return m.x();
        }

        @Override
        public double getRenderY(Marker m, Context ctx, float partialTicks) {
            return 64;
        }

        @Override
        public double getRenderZ(Marker m, Context ctx, float partialTicks) {
            return m.z();
        }

        @Override
        public int getInteractionBoxLeft(Marker m, Context ctx, float partialTicks) {
            return -8;
        }

        @Override
        public int getInteractionBoxRight(Marker m, Context ctx, float partialTicks) {
            return 8;
        }

        @Override
        public int getInteractionBoxTop(Marker m, Context ctx, float partialTicks) {
            return -8;
        }

        @Override
        public int getInteractionBoxBottom(Marker m, Context ctx, float partialTicks) {
            return 8;
        }

        @Override
        public int getRenderBoxLeft(Marker m, Context ctx, float partialTicks) {
            return -64;
        }

        @Override
        public int getRenderBoxRight(Marker m, Context ctx, float partialTicks) {
            return 64;
        }

        @Override
        public int getRenderBoxTop(Marker m, Context ctx, float partialTicks) {
            return -16;
        }

        @Override
        public int getRenderBoxBottom(Marker m, Context ctx, float partialTicks) {
            return 32;
        }

        @Override
        public int getLeftSideLength(Marker m, Minecraft mc) {
            return 0;
        }

        @Override
        public String getMenuName(Marker m) {
            return m.label() == null ? m.letter() : m.label().getString();
        }

        @Override
        public String getFilterName(Marker m) {
            return getMenuName(m);
        }

        @Override
        public int getMenuTextFillLeftPadding(Marker m) {
            return 0;
        }

        @Override
        public int getRightClickTitleBackgroundColor(Marker m) {
            return 0;
        }

        @Override
        public boolean shouldScaleBoxWithOptionalScale() {
            return false;
        }
    }
}
