package com.volcanicmg.mapcolonies.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws hut markers (item icon or lettered square, optional label) centred on each marker's recorded pose.
 * Markers are collected during Xaero's element loop and painted in two flushes per map per frame instead of
 * two per hut: per-hut flushes cost more than the drawing itself.
 */
public final class MarkerPainter {
    private MarkerPainter() {
    }

    /** One map's markers for the current frame. Not thread-safe; render thread only. */
    public static final class Batch {
        private record Entry(PoseStack.Pose pose, ClientColonies.Marker marker, boolean label) {
        }

        private final List<Entry> entries = new ArrayList<>();
        private GuiGraphics graphics;

        /** Records a marker at the current pose origin. */
        public void add(GuiGraphics g, ClientColonies.Marker m, boolean label) {
            graphics = g;
            entries.add(new Entry(g.pose().last().copy(), m, label));
        }

        /**
         * Paints everything recorded since the last call: badges and icons first, then labels, so labels stay
         * on top of other huts' badges.
         */
        public void paint(ClientColonies.IconSettings s) {
            GuiGraphics g = graphics;
            graphics = null;
            if (g == null || entries.isEmpty()) {
                entries.clear();
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            PoseStack pose = g.pose();
            List<Entry> flat = new ArrayList<>();
            for (Entry e : entries) {
                pose.pushPose();
                restore(pose, e.pose());
                if (s.background()) {
                    // Black rounded badge with a white outline, so the icon stands out on any terrain.
                    badge(g, 11, 0xFFFFFFFF);
                    badge(g, 10, 0xFF111111);
                }
                ItemStack icon = e.marker().icon();
                BakedModel model = icon.isEmpty() ? null : model(mc, icon);
                if (model == null) {
                    g.drawCenteredString(font, e.marker().letter(), 0, -4, 0xFF000000 | e.marker().color());
                } else if (model.usesBlockLight()) {
                    icon(g, mc, icon, model);
                } else {
                    flat.add(e);
                }
                pose.popPose();
            }
            g.flush();
            // Flat item models need flat lighting; drawn after the flush so block icons keep 3D lighting.
            if (!flat.isEmpty()) {
                Lighting.setupForFlatItems();
                for (Entry e : flat) {
                    pose.pushPose();
                    restore(pose, e.pose());
                    icon(g, mc, e.marker().icon(), model(mc, e.marker().icon()));
                    pose.popPose();
                }
                g.flush();
                Lighting.setupFor3DItems();
            }
            for (Entry e : entries) {
                if (!e.label() || e.marker().label() == null) continue;
                pose.pushPose();
                restore(pose, e.pose());
                int w = font.width(e.marker().label());
                // Dark backing so the label reads on any terrain.
                pose.translate(0, s.background() ? 13 : 9, 0);
                pose.scale(s.labelScale(), s.labelScale(), 1);
                g.fill(-w / 2 - 2, 0, w - w / 2 + 2, 10, 0xA0000000);
                g.drawString(font, e.marker().label(), -w / 2, 1, 0xFFFFFFFF, false);
                pose.popPose();
            }
            g.flush();
            entries.clear();
        }
    }

    private static void restore(PoseStack pose, PoseStack.Pose saved) {
        pose.last().pose().set(saved.pose());
        pose.last().normal().set(saved.normal());
    }

    private static BakedModel model(Minecraft mc, ItemStack stack) {
        return mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
    }

    /**
     * Same as {@link GuiGraphics#renderItem(ItemStack, int, int)} at (-8, -8) but without its lighting switch and
     * trailing flush, so icons batch; the caller sets lighting and flushes.
     */
    private static void icon(GuiGraphics g, Minecraft mc, ItemStack stack, BakedModel model) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, 150);
        pose.scale(16, -16, 16);
        mc.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, pose, g.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }

    /** Square of half-size r centred on the origin, corners rounded by 2px. */
    private static void badge(GuiGraphics g, int r, int argb) {
        g.fill(-r + 2, -r, r - 2, r, argb);
        g.fill(-r, -r + 2, r, r - 2, argb);
        g.fill(-r + 1, -r + 1, r - 1, r - 1, argb);
    }
}
