package com.cs2knifemenu.screen;

import com.cs2knifemenu.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact knife picker. Input is polled through GLFW (see Input) so the screen does not depend on the
 * key / mouse event classes that changed between Minecraft versions.
 */
public final class KnifeMenuScreen extends Screen {
    // palette
    private static final int PANEL = 0xF2101319, PANEL_EDGE = 0xFF262B36, TILE = 0xFF1A1E27, TILE_HOVER = 0xFF262C39;
    private static final int ACCENT = 0xFFFF8A3D, TEXT = 0xFFE9ECF3, MUTED = 0xFF8790A3, CHIP = 0xFF1E232E, CHIP_HOVER = 0xFF2B3342;

    private static final int PW = 306, PH = 212, COLS = 5, TW = 54, TH = 40, GAP = 4;
    private static final String[] TABS = {"Knives", "Skins", "Tune"};
    private static final String[] SLIDERS = {"Right", "Up", "Depth", "Yaw", "Pitch", "Size", "Volume"};
    private static final float[][] RANGE = {{-0.7f, 0.9f}, {-0.9f, 0.3f}, {-1.6f, -0.2f}, {-60f, 60f}, {-60f, 60f}, {0.5f, 3.0f}, {0f, 1f}};

    private int tab;
    private float scrollTarget, scroll, panelX = Float.NaN;
    private int listening; // 0 none, 1 menu key, 2 inspect key
    private int dragging = -1;
    private boolean pDown = Input.mouse(0), menuKeyHeld = true, pAnyKey = true;
    private final long openedAt = System.nanoTime();
    private final Config cfg = Config.get();

    public KnifeMenuScreen() { super(Component.literal("Knife Menu")); }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return listening == 0; }
    @Override public void onClose() { cfg.save(); super.onClose(); }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (tab < 2) scrollTarget -= (float) sy * 22f;
        return true;
    }

    // ------------------------------------------------------------------ helpers
    private static int al(int argb, float a) {
        return ((int) (((argb >>> 24) & 255) * Math.max(0, Math.min(1, a))) << 24) | (argb & 0xFFFFFF);
    }
    private static boolean in(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private static void rrect(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + w, y + h - 1, c);
    }
    private static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 1, y, x + w - 1, y + 1, c); g.fill(x + 1, y + h - 1, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c); g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }
    private static int skinColor(char code) {
        return switch (code) {
            case 'd' -> 0xFFFF6FE0; case '9' -> 0xFF6F8CFF; case '5' -> 0xFFB066FF; case 'c' -> 0xFFFF5C5C;
            default -> 0;
        };
    }

    private void click() { Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }

    /** Draws a chip and returns true if it was clicked this frame. */
    private boolean chip(GuiGraphics g, int x, int y, int w, String label, boolean active, int mx, int my, boolean clicked, float a) {
        boolean hov = in(mx, my, x, y, w, 14);
        rrect(g, x, y, w, 14, al(active ? ACCENT : hov ? CHIP_HOVER : CHIP, a));
        String s = label;
        g.drawString(font, s, x + (w - font.width(s)) / 2, y + 3, al(active ? 0xFF14161C : hov ? TEXT : MUTED, a), false);
        if (hov && clicked) { click(); return true; }
        return false;
    }
    private int chipW(String s) { return font.width(s) + 14; }

    private float get(int i) {
        return switch (i) { case 0 -> cfg.x; case 1 -> cfg.y; case 2 -> cfg.z; case 3 -> cfg.yaw; case 4 -> cfg.pitch; case 5 -> cfg.scale; default -> cfg.volume; };
    }
    private void set(int i, float v) {
        switch (i) { case 0 -> cfg.x = v; case 1 -> cfg.y = v; case 2 -> cfg.z = v; case 3 -> cfg.yaw = v; case 4 -> cfg.pitch = v; case 5 -> cfg.scale = v; default -> cfg.volume = v; }
    }

    // ------------------------------------------------------------------ render
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float t = Math.min(1f, (System.nanoTime() - openedAt) / 1e9f / 0.2f);
        float a = 1f - (1f - t) * (1f - t) * (1f - t);

        boolean down = Input.mouse(0), clicked = down && !pDown;
        pDown = down;

        // close on menu key / handle key rebinding
        boolean mk = Input.key(cfg.menuKey);
        if (listening == 0) {
            if (mk && !menuKeyHeld) { onClose(); return; }
        } else {
            int k = Input.anyKey();
            if (k > 0 && !pAnyKey) {
                if (k != GLFW.GLFW_KEY_ESCAPE) { if (listening == 1) cfg.menuKey = k; else cfg.inspectKey = k; cfg.save(); }
                listening = 0; mk = true; // swallow so the new menu key doesn't close the screen at once
            }
            pAnyKey = k > 0;
        }
        menuKeyHeld = mk;

        // panel position: slides left on the Tune tab so the knife stays visible
        float targetX = tab == 2 ? 10 : (width - PW) / 2f;
        panelX = Float.isNaN(panelX) ? targetX : panelX + (targetX - panelX) * 0.25f;
        int px = Math.round(panelX), py = (height - PH) / 2 + Math.round((1 - a) * 10);
        float panelA = (tab == 2 ? 0.88f : 1f) * a;

        g.fill(0, 0, width, height, al(0x66000000, a * (tab == 2 ? 0.4f : 1f)));
        rrect(g, px - 1, py - 1, PW + 2, PH + 2, al(PANEL_EDGE, a));
        rrect(g, px, py, PW, PH, al(PANEL, panelA));

        // header
        g.fill(px + 10, py + 8, px + 13, py + 19, al(ACCENT, a));
        g.drawString(font, "KNIFE MENU", px + 18, py + 10, al(TEXT, a), false);
        int cx = px + PW - 10;
        for (int i = TABS.length - 1; i >= 0; i--) {
            int w = chipW(TABS[i]); cx -= w;
            if (chip(g, cx, py + 7, w, TABS[i], tab == i, mx, my, clicked, a)) { tab = i; scrollTarget = scroll = 0; }
            cx -= 4;
        }

        int gx = px + 10, gy = py + 30, gw = PW - 20, gh = 128;
        KnifeRegistry.Info hover = null;
        if (tab < 2) hover = drawGrid(g, gx, gy, gw, gh, mx, my, clicked, a);
        else drawTune(g, gx, gy, mx, my, clicked, down, a);

        // bottom bar
        int by = py + PH - 48;
        g.fill(px + 10, by - 2, px + PW - 10, by - 1, al(PANEL_EDGE, a));
        KnifeRegistry.Info sel = KnifeRegistry.get(cfg.knife);
        KnifeRegistry.Info shown = hover != null ? hover : sel;
        int nameColor = shown == sel ? ACCENT : TEXT;
        if (shown != null) g.drawString(font, shown.name, px + 10, by + 5, al(nameColor, a), false);
        if (shown == sel) g.drawString(font, "EQUIPPED", px + 14 + font.width(shown.name), by + 5, al(MUTED, a), false);

        int rx = px + PW - 10;
        String onOff = cfg.enabled ? "On" : "Off";
        rx -= chipW(onOff);
        if (chip(g, rx, by + 2, chipW(onOff), onOff, cfg.enabled, mx, my, clicked, a)) { cfg.enabled = !cfg.enabled; cfg.save(); }
        rx -= 4 + chipW("Inspect");
        if (chip(g, rx, by + 2, chipW("Inspect"), "Inspect", false, mx, my, clicked, a)) KnifeController.I.previewInspect();

        int x2 = px + 10, y2 = by + 26;
        String show = "Show: " + Config.SHOW_MODES[cfg.showMode];
        if (chip(g, x2, y2, chipW(show), show, false, mx, my, clicked, a)) { cfg.showMode = (cfg.showMode + 1) % Config.SHOW_MODES.length; cfg.save(); }
        x2 += chipW(show) + 4;
        String mkS = listening == 1 ? "Press a key..." : "Menu: " + Input.keyName(cfg.menuKey);
        if (chip(g, x2, y2, chipW(mkS), mkS, listening == 1, mx, my, clicked, a)) { listening = 1; pAnyKey = true; }
        x2 += chipW(mkS) + 4;
        String ikS = listening == 2 ? "Press a key..." : "Inspect: " + Input.keyName(cfg.inspectKey);
        if (chip(g, x2, y2, chipW(ikS), ikS, listening == 2, mx, my, clicked, a)) { listening = 2; pAnyKey = true; }
    }

    private KnifeRegistry.Info drawGrid(GuiGraphics g, int gx, int gy, int gw, int gh, int mx, int my, boolean clicked, float a) {
        List<KnifeRegistry.Info> list = new ArrayList<>();
        for (KnifeRegistry.Info i : KnifeRegistry.all()) if (i.skin == (tab == 1)) list.add(i);
        int rows = (list.size() + COLS - 1) / COLS;
        float content = rows * (TH + GAP) - GAP;
        float max = Math.max(0, content - gh);
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.3f;

        KnifeRegistry.Info hover = null;
        g.enableScissor(gx, gy, gx + gw, gy + gh);
        for (int n = 0; n < list.size(); n++) {
            KnifeRegistry.Info k = list.get(n);
            int tx = gx + (n % COLS) * (TW + GAP), ty = gy + (n / COLS) * (TH + GAP) - Math.round(scroll);
            if (ty + TH < gy || ty > gy + gh) continue;
            boolean hov = in(mx, my, tx, ty, TW, TH) && in(mx, my, gx, gy, gw, gh);
            boolean sel = k.id.equals(cfg.knife);
            if (hov) hover = k;
            rrect(g, tx, ty, TW, TH, al(sel ? 0xFF2A2018 : hov ? TILE_HOVER : TILE, a));
            if (sel) outline(g, tx, ty, TW, TH, al(ACCENT, a));
            int sc = skinColor(k.color);
            if (sc != 0) g.fill(tx + 6, ty + TH - 3, tx + TW - 6, ty + TH - 2, al(sc, a));
            if (k.hasIcon) {
                int is = 32, ix = tx + (TW - is) / 2, iy = ty + (TH - is) / 2 - (hov ? 1 : 0);
                g.blit(RenderPipelines.GUI_TEXTURED, k.icon, ix, iy, 0f, 0f, is, is, k.iconW, k.iconH, k.iconW, k.iconH);
                if (!hov && !sel) g.fill(ix, iy, ix + is, iy + is, al(0x40101319, a));
            } else {
                g.drawString(font, k.name.substring(0, 1), tx + TW / 2 - 2, ty + TH / 2 - 4, al(TEXT, a), false);
            }
            if (hov && clicked && !sel) { click(); cfg.knife = k.id; cfg.save(); }
        }
        g.disableScissor();
        if (max > 0) { // slim scrollbar
            int bh = Math.max(14, (int) (gh * gh / content)), bx = gx + gw - 2;
            int by2 = gy + (int) ((gh - bh) * (scroll / max));
            g.fill(bx, by2, bx + 2, by2 + bh, al(MUTED, a * 0.6f));
        }
        return hover;
    }

    private void drawTune(GuiGraphics g, int gx, int gy, int mx, int my, boolean clicked, boolean down, float a) {
        int cw = 140, ch = 28;
        if (!down) { if (dragging >= 0) cfg.save(); dragging = -1; }
        for (int i = 0; i < SLIDERS.length; i++) {
            int x = gx + (i % 2) * (cw + 6), y = gy + (i / 2) * ch + 2;
            boolean hov = in(mx, my, x, y, cw, ch - 4);
            if (hov && clicked && dragging < 0) dragging = i;
            float lo = RANGE[i][0], hi = RANGE[i][1], v = get(i);
            if (dragging == i) {
                v = lo + (hi - lo) * Math.max(0f, Math.min(1f, (mx - (x + 2f)) / (cw - 4f)));
                set(i, v);
            }
            String val = i == 6 ? Math.round(v * 100) + "%" : (i == 3 || i == 4) ? Math.round(v) + "\u00B0" : String.format("%.2f", v);
            g.drawString(font, SLIDERS[i], x, y, al(hov || dragging == i ? TEXT : MUTED, a), false);
            g.drawString(font, val, x + cw - font.width(val), y, al(TEXT, a), false);
            int ty = y + 13;
            g.fill(x, ty, x + cw, ty + 3, al(CHIP, a));
            int fillW = Math.round((cw - 4) * (v - lo) / (hi - lo));
            g.fill(x, ty, x + 2 + fillW, ty + 3, al(ACCENT, a));
            g.fill(x + fillW, ty - 2, x + fillW + 4, ty + 5, al(TEXT, a));
        }
        int x = gx + (SLIDERS.length % 2) * (cw + 6), y = gy + (SLIDERS.length / 2) * ch + 4;
        String mir = cfg.mirror ? "Left hand: On" : "Left hand: Off";
        if (chip(g, x, y, chipW(mir), mir, cfg.mirror, mx, my, clicked, a)) { cfg.mirror = !cfg.mirror; cfg.save(); }
        if (chip(g, x + chipW(mir) + 4, y, chipW("Reset"), "Reset", false, mx, my, clicked, a)) { cfg.resetPlacement(); cfg.save(); }
    }
}
