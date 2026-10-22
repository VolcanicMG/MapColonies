package com.volcanicmg.mapcolonies.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Draws one hut marker centred on the current pose origin: item icon (or lettered square) and optional label. */
public final class MarkerPainter {
    private MarkerPainter() {
    }

    public static void draw(GuiGraphics g, ClientColonies.Marker m, boolean label, ClientColonies.IconSettings s) {
        Font font = Minecraft.getInstance().font;
        // Black rounded badge with a white outline, so the icon stands out on any terrain.
        if (s.background()) {
            badge(g, 11, 0xFFFFFFFF);
            badge(g, 10, 0xFF111111);
        }
        if (!m.icon().isEmpty()) {
            g.renderItem(m.icon(), -8, -8);
        } else {
            g.drawCenteredString(font, m.letter(), 0, -4, 0xFF000000 | m.color());
        }
        if (label && m.label() != null) {
            int w = font.width(m.label());
            // Dark backing so the label reads on any terrain.
            g.pose().pushPose();
            g.pose().translate(0, s.background() ? 13 : 9, 0);
            g.pose().scale(s.labelScale(), s.labelScale(), 1);
            g.fill(-w / 2 - 2, 0, w - w / 2 + 2, 10, 0xA0000000);
            g.drawString(font, m.label(), -w / 2, 1, 0xFFFFFFFF, false);
            g.pose().popPose();
        }
        g.flush();
    }

    /** Square of half-size r centred on the origin, corners rounded by 2px. */
    private static void badge(GuiGraphics g, int r, int argb) {
        g.fill(-r + 2, -r, r - 2, r, argb);
        g.fill(-r, -r + 2, r, r - 2, argb);
        g.fill(-r + 1, -r + 1, r - 1, r - 1, argb);
    }
}
