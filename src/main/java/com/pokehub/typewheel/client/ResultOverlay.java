package com.pokehub.typewheel.client;

import com.pokehub.typewheel.TypeWheelBlockEntity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** The on-screen "landed on TYPE" banner animation. */
public final class ResultOverlay {
    private static final long TOTAL = 4500L;
    private static long start = -1L;
    private static int typeIndex;
    private static String spinner = "";

    private ResultOverlay() {}

    public static void show(int type, String name) {
        if (type < 0 || type >= TypeWheelBlockEntity.TYPES.length) return;
        typeIndex = type;
        spinner = name == null ? "" : name;
        start = Util.getMillis();
    }

    private static float outCubic(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float i = 1.0F - t;
        return 1.0F - i * i * i;
    }

    private static float outBack(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float c1 = 1.70158F, c3 = c1 + 1.0F, i = t - 1.0F;
        return 1.0F + c3 * i * i * i + c1 * i * i;
    }

    private static int argb(float alpha, int rgb) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int shade(int rgb, float f) {
        int r = Mth.clamp((int) (((rgb >> 16) & 255) * f), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 255) * f), 0, 255);
        int b = Mth.clamp((int) ((rgb & 255) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    public static void render(GuiGraphics g) {
        if (start < 0L) return;
        long ms = Util.getMillis() - start;
        if (ms > TOTAL) {
            start = -1L;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        int color = TypeWheelBlockEntity.COLORS[typeIndex];
        String type = TypeWheelBlockEntity.TYPES[typeIndex].toUpperCase();

        float fade = 1.0F - Mth.clamp((ms - (TOTAL - 600L)) / 600.0F, 0.0F, 1.0F);
        if (fade < 0.04F) return;
        float open = outCubic(ms / 300.0F);            // band sweeps out from the middle
        float pop = outBack((ms - 180L) / 420.0F);     // type name pops in with a bounce
        float sub = outCubic((ms - 450L) / 350.0F);    // name line slides in
        float pulse = 1.0F + 0.04F * Mth.sin(ms / 140.0F);

        int cy = (int) (h * 0.30F);
        int half = (int) (w * 0.5F * open);
        int cx = w / 2;
        int bandH = 26;

        // dark backing, coloured band, bright edge stripes
        g.fill(cx - half, cy - bandH - 14, cx + half, cy + bandH + 4, argb(0.55F * fade, 0x000000));
        g.fill(cx - half, cy - bandH, cx + half, cy + bandH, argb(0.92F * fade, shade(color, 0.85F)));
        g.fill(cx - half, cy - bandH, cx + half, cy - bandH + 2, argb(fade, shade(color, 1.35F)));
        g.fill(cx - half, cy + bandH - 2, cx + half, cy + bandH, argb(fade, shade(color, 1.35F)));
        // white flash at the very start
        float flash = 1.0F - Mth.clamp(ms / 350.0F, 0.0F, 1.0F);
        if (flash > 0.02F) g.fill(cx - half, cy - bandH, cx + half, cy + bandH, argb(0.8F * flash, 0xFFFFFF));

        // big type name
        if (ms > 180L && pop > 0.01F) {
            float scale = 4.0F * pop * pulse;
            // keep long names on screen
            float max = (w * 0.9F) / Math.max(1, font.width(type));
            if (scale > max) scale = max;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, type, -font.width(type) / 2, -font.lineHeight / 2 + 1, argb(Math.max(fade, 0.05F), 0xFFFFFF), true);
            g.pose().popPose();
        }

        // "<player> spun the Type Wheel!"
        if (ms > 450L && sub * fade > 0.05F) {
            String line = (spinner.isEmpty() ? "Someone" : spinner) + " spun the Type Wheel!";
            float a = sub * fade;
            int y = cy - bandH - 11 + (int) ((1.0F - sub) * 6.0F);
            g.drawString(font, line, cx - font.width(line) / 2, y, argb(a, 0xFFE27A), true);
        }
    }
}
