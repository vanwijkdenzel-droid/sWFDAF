package dev.cpvp.gui;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.module.ModuleManager;
import dev.cpvp.module.impl.InterfaceModule;
import dev.cpvp.setting.BooleanSetting;
import dev.cpvp.setting.NumberSetting;
import dev.cpvp.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

import static dev.cpvp.gui.RenderUtil.*;

/**
 * "Fish v1" ClickGUI - every category is its own window, side by side.
 *  LMB module = toggle | RMB module (or dots) = settings | RMB header = collapse column
 *  Gear button (top right) = Settings window (theme colour etc.).
 */
public final class ClickGuiScreen extends Screen {
    private static final int HEADER_H = 26, ROW_H = 22, GAP = 6, TOP = 44;

    private static final List<Category> COLS = new ArrayList<>();
    static { for (Category c : Category.values()) if (c != Category.SETTINGS) COLS.add(c); }

    private static final Set<Category> COLLAPSED = new HashSet<>();
    private static final Map<Category, Float> COL_ANIM = new HashMap<>();
    private static final Map<Category, float[]> SCROLL = new HashMap<>();   // {target, current}
    private static final Map<Module, Float> EXPAND = new HashMap<>();
    private static final Set<Module> OPEN = new HashSet<>();
    private static boolean showSettings;
    private static Category selected = Category.COMBAT;   // vape layout

    private record Hit(int x, int y, int w, int h, int clipTop, int clipBottom, IntConsumer click) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private record Bounds(int x, int y, int w, int h, Category cat) {}

    private final List<Hit> hits = new ArrayList<>();
    private final List<Bounds> bounds = new ArrayList<>();
    private Module listening;
    private NumberSetting dragging;
    private int dragX, dragW;
    private double clickX;
    private float kAnim;
    private long lastFrame = System.nanoTime();

    public ClickGuiScreen() { super(Text.literal("Fish v1")); }

    @Override public boolean shouldPause() { return false; }

    private InterfaceModule ui() { return ModuleManager.INSTANCE.get(InterfaceModule.class); }

    @Override
    public void render(DrawContext c, int mx, int my, float tickDelta) {
        ACCENT = ui().accent();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        kAnim = 1f - (float) Math.exp(-dt * ui().animSpeed.get());
        float kScroll = 1f - (float) Math.exp(-dt * 16f);
        hits.clear();
        bounds.clear();

        // The game stays visible - only a faint neutral dim, no blue fill.
        c.fill(0, 0, width, height, 0x30000000);

        if (ui().vapeLayout.get()) { renderVape(c, mx, my, kScroll); return; }

        // logo (top left)
        text(c, "Fish", 14, 10, TEXT, 2.0f);
        text(c, "v1", 14 + width("Fish", 2.0f) + 5, 19, ACCENT, 1.1f);

        // settings button (top right)
        int bx = width - 34, by = 9;
        boolean hov = mx >= bx && mx < bx + 24 && my >= by && my < by + 24;
        roundRect(c, bx, by, bx + 24, by + 24, 6, showSettings ? alpha(ACCENT, 0.3f) : (hov ? ROW_HOV : NAV));
        icon(c, Category.SETTINGS.ordinal(), bx + 5, by + 5, showSettings || hov ? ACCENT : TEXT);
        hits.add(new Hit(bx, by, 24, 24, Integer.MIN_VALUE, Integer.MAX_VALUE, b -> showSettings = !showSettings));

        // columns
        int n = COLS.size();
        int colW = MathHelper.clamp((width - 28 - (n - 1) * GAP) / n, 100, 150);
        int total = n * colW + (n - 1) * GAP, startX = (width - total) / 2;
        int maxBody = height - TOP - 16 - HEADER_H;
        for (int i = 0; i < n; i++) drawColumn(c, COLS.get(i), startX + i * (colW + GAP), TOP, colW, maxBody, mx, my, kScroll, false);

        if (showSettings) drawSettingsWindow(c, width - 14 - 190, TOP, 190);
    }

    private void drawColumn(DrawContext c, Category cat, int x, int y0, int colW, int maxBody, int mx, int my, float kScroll, boolean vape) {
        List<Module> mods = ModuleManager.INSTANCE.byCategory(cat);

        float ca = COL_ANIM.getOrDefault(cat, 1f);
        float goal = (!vape && COLLAPSED.contains(cat)) ? 0f : 1f;
        ca += (goal - ca) * kAnim;
        if (Math.abs(goal - ca) < 0.01f) ca = goal;
        COL_ANIM.put(cat, ca);

        float content = 2;
        for (Module m : mods) {
            float e = EXPAND.getOrDefault(m, 0f), g = OPEN.contains(m) ? 1f : 0f;
            e += (g - e) * kAnim;
            if (Math.abs(g - e) < 0.01f) e = g;
            EXPAND.put(m, e);
            content += ROW_H + Math.round(settingsHeight(m, true) * e) + 2;
        }
        content += 4;
        int bodyFull = vape ? maxBody : (int) Math.min(content, maxBody);
        int bodyH = Math.round(bodyFull * ca);

        float[] sc = SCROLL.computeIfAbsent(cat, k -> new float[2]);
        sc[0] = MathHelper.clamp(sc[0], 0, Math.max(0, content - maxBody));
        sc[1] += (sc[0] - sc[1]) * kScroll;
        if (Math.abs(sc[0] - sc[1]) < 0.05f) sc[1] = sc[0];

        roundRect(c, x, y0, x + colW, y0 + HEADER_H + bodyH, 7, PANEL);
        roundRect(c, x, y0, x + colW, y0 + HEADER_H, 7, HEADER);
        if (bodyH > 0) c.fill(x, y0 + HEADER_H - 7, x + colW, y0 + HEADER_H, HEADER);
        icon(c, cat.ordinal(), x + 9, y0 + 6, ACCENT);
        text(c, cat.label, x + 30, y0 + 8, TEXT, 0.95f);
        if (COLLAPSED.contains(cat)) chevronRight(c, x + colW - 15, y0 + 10, DIM);
        else chevronDown(c, x + colW - 17, y0 + 11, DIM);
        hits.add(new Hit(x, y0, colW, HEADER_H, Integer.MIN_VALUE, Integer.MAX_VALUE, b -> {
            if (!vape && b == 1 && !COLLAPSED.remove(cat)) COLLAPSED.add(cat);
        }));
        bounds.add(new Bounds(x, y0, colW, HEADER_H + bodyH, cat));
        if (bodyH <= 0) return;

        final int clipTop = y0 + HEADER_H, clipBottom = y0 + HEADER_H + bodyH;
        c.enableScissor(x, clipTop, x + colW, clipBottom);
        float y = clipTop + 2 - sc[1];
        for (Module m : mods) {
            float e = EXPAND.getOrDefault(m, 0f);
            final int rx = x + 4, rw = colW - 8, top = Math.round(y);
            int setH = Math.round(settingsHeight(m, true) * e);

            if (top + ROW_H + setH > clipTop && top < clipBottom) {
                boolean hv = mx >= rx && mx < rx + rw && my >= top && my < top + ROW_H && my >= clipTop && my <= clipBottom;
                int bg = m.isEnabled() ? alpha(ACCENT, 0.18f) : (hv ? ROW_HOV : 0);
                if (bg != 0) roundRect(c, rx, top, rx + rw, top + ROW_H, 5, bg);
                if (m.isEnabled()) roundRect(c, rx, top + 5, rx + 2, top + ROW_H - 5, 1, ACCENT);
                text(c, m.getName(), rx + 8, top + 6, m.isEnabled() ? ACCENT : TEXT, 0.9f);
                if (m.getKey() >= 0) {
                    String kn = keyName(m.getKey());
                    text(c, kn, rx + rw - 18 - width(kn, 0.7f), top + 8, DIM, 0.7f);
                }
                dots(c, rx + rw - 11, top + 5, hv || OPEN.contains(m) ? TEXT : DIM);

                hits.add(new Hit(rx, top, rw - 18, ROW_H, clipTop, clipBottom, b -> {
                    if (b == 0) m.toggle(); else if (b == 1) toggleOpen(m);
                }));
                hits.add(new Hit(rx + rw - 18, top, 18, ROW_H, clipTop, clipBottom, b -> toggleOpen(m)));
                if (setH > 1) drawSettings(c, m, rx, rw, top + ROW_H, setH, e > 0.95f, clipTop, clipBottom, true);
            }
            y += ROW_H + setH + 2;
        }
        c.disableScissor();

        float max = Math.max(0, content - maxBody);
        if (max > 0) {
            int barH = Math.max(14, (int) (bodyH * (bodyH / content)));
            int barY = clipTop + (int) ((bodyH - barH) * (sc[1] / max));
            roundRect(c, x + colW - 4, barY, x + colW - 2, barY + barH, 1, 0xFF2A3560);
        }
    }

    // ================= Vape V4 layout: sidebar + one module panel =================
    private void renderVape(DrawContext c, int mx, int my, float kScroll) {
        final int NAV_W = 132, PAN_W = 214;
        int H = Math.min(340, height - 30);
        int x0 = (width - (NAV_W + 10 + PAN_W)) / 2, y0 = (height - H) / 2;

        roundRect(c, x0, y0, x0 + NAV_W, y0 + H, 7, NAV);
        text(c, "FISH", x0 + 16, y0 + 12, TEXT, 1.6f);
        text(c, "v1", x0 + 16 + width("FISH", 1.6f) + 5, y0 + 19, ACCENT, 1.0f);

        int ny = y0 + 44;
        for (Category cat : COLS) ny = navRow(c, cat, x0, ny, NAV_W, mx, my);
        ny += 6;
        c.fill(x0 + 14, ny + 6, x0 + 28, ny + 7, 0xFF141D3C);
        text(c, "MISC", x0 + 34, ny + 2, DIM, 0.7f);
        c.fill(x0 + 58, ny + 6, x0 + NAV_W - 14, ny + 7, 0xFF141D3C);
        navRow(c, Category.SETTINGS, x0, ny + 16, NAV_W, mx, my);

        int px = x0 + NAV_W + 10;
        if (selected == Category.SETTINGS) drawSettingsWindow(c, px, y0, PAN_W);
        else drawColumn(c, selected, px, y0, PAN_W, H - HEADER_H, mx, my, kScroll, true);
    }

    private int navRow(DrawContext c, Category cat, int x0, int y, int w, int mx, int my) {
        boolean sel = cat == selected;
        boolean hov = mx >= x0 && mx < x0 + w && my >= y && my < y + 28;
        if (sel) roundRect(c, x0 + 6, y, x0 + w - 6, y + 26, 5, ROW_SEL);
        else if (hov) roundRect(c, x0 + 6, y, x0 + w - 6, y + 26, 5, ROW_HOV);
        icon(c, cat.ordinal(), x0 + 16, y + 6, sel ? ACCENT : DIM);
        text(c, cat.label, x0 + 40, y + 7, sel ? ACCENT : TEXT, 0.95f);
        chevronRight(c, x0 + w - 20, y + 10, sel ? ACCENT : DIM);
        hits.add(new Hit(x0, y, w, 28, Integer.MIN_VALUE, Integer.MAX_VALUE, b -> { selected = cat; listening = null; }));
        return y + 28;
    }

    private void drawSettingsWindow(DrawContext c, int x, int y, int w) {
        Module ifc = ui();
        int bodyH = settingsHeight(ifc, false) + 4;
        roundRect(c, x, y, x + w, y + HEADER_H + bodyH, 7, PANEL);
        roundRect(c, x, y, x + w, y + HEADER_H, 7, HEADER);
        c.fill(x, y + HEADER_H - 7, x + w, y + HEADER_H, HEADER);
        icon(c, Category.SETTINGS.ordinal(), x + 9, y + 6, ACCENT);
        text(c, "Settings", x + 30, y + 8, TEXT, 0.95f);
        drawSettings(c, ifc, x + 4, w - 8, y + HEADER_H + 2, bodyH, true, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
        bounds.add(new Bounds(x, y, w, HEADER_H + bodyH, Category.SETTINGS));
    }

    private void toggleOpen(Module m) { if (!OPEN.remove(m)) OPEN.add(m); }

    private void drawSettings(DrawContext c, Module m, int rx, int rw, int startY, int setH, boolean interact,
                              int clipTop, int clipBottom, boolean keybind) {
        c.enableScissor(rx, startY, rx + rw, startY + setH);
        roundRect(c, rx + 1, startY, rx + rw - 1, startY + setH - 2, 5, INNER);
        int sy = startY + 2;
        final int ix = rx + 9, iw = rw - 18;

        if (keybind) {
            text(c, "Keybind", ix, sy + 5, DIM, 0.8f);
            String kt = listening == m ? "press a key..." : (m.getKey() < 0 ? "NONE" : keyName(m.getKey()));
            int kw = width(kt, 0.75f) + 10, kx = ix + iw - kw;
            roundRect(c, kx, sy + 2, kx + kw, sy + 16, 4, TRACK);
            text(c, kt, kx + 5, sy + 5, listening == m ? ACCENT : TEXT, 0.75f);
            if (interact) hits.add(new Hit(kx, sy + 2, kw, 14, clipTop, clipBottom, b -> listening = (listening == m) ? null : m));
            sy += 20;
        }

        for (Setting s : m.getSettings()) {
            if (s instanceof BooleanSetting bs) {
                text(c, s.getName(), ix, sy + 5, TEXT, 0.8f);
                checkbox(c, ix + iw - 13, sy + 3, 13, bs.get());
                if (interact) hits.add(new Hit(ix, sy, iw, 20, clipTop, clipBottom, b -> bs.toggle()));
                sy += 20;
            } else if (s instanceof NumberSetting ns) {
                text(c, s.getName(), ix, sy + 3, TEXT, 0.8f);
                String v = ns.display();
                text(c, v, ix + iw - width(v, 0.8f), sy + 3, ACCENT, 0.8f);
                final int tx = ix, tw = iw, ty = sy + 17;
                double f = (ns.get() - ns.min()) / (ns.max() - ns.min());
                int fw = (int) (tw * f);
                roundRect(c, tx, ty, tx + tw, ty + 4, 2, TRACK);
                if (fw > 0) roundRect(c, tx, ty, tx + Math.max(fw, 4), ty + 4, 2, ACCENT);
                roundRect(c, tx + fw - 3, ty - 2, tx + fw + 3, ty + 6, 3, 0xFFFFFFFF);
                if (interact) hits.add(new Hit(tx - 3, ty - 6, tw + 6, 16, clipTop, clipBottom, b -> {
                    dragging = ns; dragX = tx; dragW = tw; updateSlider(clickX);
                }));
                sy += 27;
            }
        }
        c.disableScissor();
    }

    private int settingsHeight(Module m, boolean keybind) {
        int h = keybind ? 24 : 4;
        for (Setting s : m.getSettings()) h += (s instanceof NumberSetting) ? 27 : 20;
        return h + 4;
    }

    private void updateSlider(double mouseX) {
        if (dragging == null) return;
        double f = MathHelper.clamp((mouseX - dragX) / (double) dragW, 0, 1);
        dragging.set(dragging.min() + f * (dragging.max() - dragging.min()));
    }

    private static String keyName(int key) {
        return InputUtil.fromKeyCode(key, -1).getLocalizedText().getString().toUpperCase(Locale.ROOT);
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        clickX = mx;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (my < h.clipTop() || my > h.clipBottom()) continue;
            if (h.contains(mx, my)) { h.click().accept(button); return true; }
        }
        listening = null;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { updateSlider(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        for (Bounds b : bounds) {
            if (b.cat() == Category.SETTINGS || mx < b.x() || mx >= b.x() + b.w() || my < b.y() || my >= b.y() + b.h()) continue;
            float[] sc = SCROLL.computeIfAbsent(b.cat(), k -> new float[2]);
            sc[0] -= (float) (vertical * ui().scrollSpeed.get());
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (listening != null) {
            boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE;
            listening.setKey(clear ? -1 : key);
            listening = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, scancode, mods);
    }
}
