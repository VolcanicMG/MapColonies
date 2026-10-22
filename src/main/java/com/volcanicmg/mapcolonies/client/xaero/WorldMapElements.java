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
import xaero.map.WorldMap;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

/**
 * Hut icons and labels on Xaero's World Map, through its element renderer system (the one waypoints and
 * player trackers use). Registered with the public {@link MapElementRenderHandler#add}, no mixin needed.
 */
public final class WorldMapElements {
    // Element space is roughly half a GUI pixel; Xaero's own trackers draw at 2x.
    private static final float BASE_SCALE = 2f;

    private WorldMapElements() {
    }

    /** @return true once registered; false while Xaero has not built its handler yet */
    public static boolean tryRegister() {
        MapElementRenderHandler handler = WorldMap.mapElementRenderHandler;
        if (handler == null) return false;
        handler.add(new Renderer());
        return true;
    }

    static final class Context {
        List<Marker> markers = List.of();
        int next;
    }

    static final class Renderer extends ElementRenderer<Marker, Context, Renderer> {
        Renderer() {
            super(new Context(), new Provider(), new Reader());
        }

        @Override
        public void preRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                              MultiTextureRenderTypeRendererProvider renderers, boolean shadow) {
            context.markers = ClientColonies.iconSettings().world() && info.mapDimension != null
                    ? ClientColonies.markers(info.mapDimension.location()) : List.of();
        }

        @Override
        public void postRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                               MultiTextureRenderTypeRendererProvider renderers, boolean shadow) {
        }

        @Override
        public void renderElementShadow(Marker m, boolean hovered, float optionalScale, double partialX,
                                        double partialY, ElementRenderInfo info, GuiGraphics g,
                                        MultiBufferSource.BufferSource buffers,
                                        MultiTextureRenderTypeRendererProvider renderers) {
        }

        @Override
        public boolean renderElement(Marker m, boolean hovered, double depth, float optionalScale, double partialX,
                                     double partialY, ElementRenderInfo info, GuiGraphics g,
                                     MultiBufferSource.BufferSource buffers,
                                     MultiTextureRenderTypeRendererProvider renderers) {
            ClientColonies.IconSettings s = ClientColonies.iconSettings();
            boolean label = switch (s.labelWorld()) {
                case ALWAYS -> info.scale >= s.labelMinZoom();
                case HOVER -> hovered;
                case NONE -> false;
            };
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(partialX, partialY, depth);
            float scale = BASE_SCALE * s.scale();
            pose.scale(scale, scale, 1);
            MarkerPainter.draw(g, m, label, s);
            pose.popPose();
            return false;
        }

        @Override
        public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
            return location == ElementRenderLocation.WORLD_MAP && !shadow;
        }

        // Markers are already in the viewed map's coordinates.
        @Override
        public boolean shouldBeDimScaled() {
            return false;
        }
    }

    static final class Provider extends ElementRenderProvider<Marker, Context> {
        @Override
        public void begin(ElementRenderLocation location, Context ctx) {
            ctx.next = 0;
        }

        @Override
        public boolean hasNext(ElementRenderLocation location, Context ctx) {
            return ctx.next < ctx.markers.size();
        }

        @Override
        public Marker getNext(ElementRenderLocation location, Context ctx) {
            return ctx.markers.get(ctx.next++);
        }

        @Override
        public void end(ElementRenderLocation location, Context ctx) {
        }
    }

    static final class Reader extends ElementReader<Marker, Context, Renderer> {
        @Override
        public boolean isHidden(Marker m, Context ctx) {
            return false;
        }

        @Override
        public double getRenderX(Marker m, Context ctx, float partialTicks) {
            return m.x();
        }

        @Override
        public double getRenderZ(Marker m, Context ctx, float partialTicks) {
            return m.z();
        }

        // Hover only matters for HOVER labels; this also keeps Xaero's click handling off the icons otherwise.
        @Override
        public boolean isInteractable(ElementRenderLocation location, Marker m) {
            return ClientColonies.iconSettings().labelWorld() == Config.LabelMode.HOVER;
        }

        @Override
        public boolean isRightClickValid(Marker m) {
            return false;
        }

        @Override
        public int getInteractionBoxLeft(Marker m, Context ctx, float partialTicks) {
            return -16;
        }

        @Override
        public int getInteractionBoxRight(Marker m, Context ctx, float partialTicks) {
            return 16;
        }

        @Override
        public int getInteractionBoxTop(Marker m, Context ctx, float partialTicks) {
            return -16;
        }

        @Override
        public int getInteractionBoxBottom(Marker m, Context ctx, float partialTicks) {
            return 16;
        }

        // Generous so labels are not culled while the icon is near the screen edge.
        @Override
        public int getRenderBoxLeft(Marker m, Context ctx, float partialTicks) {
            return -128;
        }

        @Override
        public int getRenderBoxRight(Marker m, Context ctx, float partialTicks) {
            return 128;
        }

        @Override
        public int getRenderBoxTop(Marker m, Context ctx, float partialTicks) {
            return -32;
        }

        @Override
        public int getRenderBoxBottom(Marker m, Context ctx, float partialTicks) {
            return 64;
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
