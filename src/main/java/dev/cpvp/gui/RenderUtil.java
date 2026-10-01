package dev.cpvp.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** 2D helpers: rounded rects, TTF text (assets/cpvpclient/font/ui.json), pixel icons. */
public final class RenderUtil {
    /** Theme colour - updated every frame from the Interface module (Other > Interface). */
    public static int ACCENT = 0xFF2E7BFF;

    public static final int NAV      = 0xF4050916;
    public static final int PANEL    = 0xF0070C1B;
    public static final int HEADER   = 0xFF0B1230;
    public static final int ROW_HOV  = 0xFF0E1635;
    public static final int ROW_SEL  = 0xFF101A3D;
    public static final int INNER    = 0xFF050A18;
    public static final int TEXT     = 0xFFE4EBFF;
    public static final int DIM      = 0xFF5C6A92;
    public static final int TRACK    = 0xFF131C3B;

    private static final Style FONT = Style.EMPTY.withFont(Identifier.of("cpvpclient", "ui"));

    private RenderUtil() {}

    public static int alpha(int argb, float mul) {
        int a = Math.round(((argb >>> 24) & 0xFF) * mul);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static void roundRect(DrawContext c, int x, int y, int x2, int y2, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min((x2 - x) / 2, (y2 - y) / 2)));
        if (r == 0) { c.fill(x, y, x2, y2, color); return; }
        c.fill(x, y + r, x2, y2 - r, color);
        int soft = alpha(color, 0.5f);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.floor(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset + 1, y + i, x2 - inset - 1, y + i + 1, color);
            c.fill(x + inset + 1, y2 - i - 1, x2 - inset - 1, y2 - i, color);
            c.fill(x + inset, y + i, x + inset + 1, y + i + 1, soft);
            c.fill(x2 - inset - 1, y + i, x2 - inset, y + i + 1, soft);
            c.fill(x + inset, y2 - i - 1, x + inset + 1, y2 - i, soft);
            c.fill(x2 - inset - 1, y2 - i - 1, x2 - inset, y2 - i, soft);
        }
    }

    // ---- text (smooth TTF) ----
    private static Text styled(String s) { return Text.literal(s).setStyle(FONT); }

    public static void text(DrawContext c, String s, int x, int y, int color, float scale) {
        var m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        c.drawText(MinecraftClient.getInstance().textRenderer, styled(s), 0, 0, color, false);
        m.pop();
    }

    public static int width(String s, float scale) {
        return Math.round(MinecraftClient.getInstance().textRenderer.getWidth(styled(s)) * scale);
    }

    public static void centerText(DrawContext c, String s, int cx, int y, int color, float scale) {
        text(c, s, cx - width(s, scale) / 2, y, color, scale);
    }

    // ---- icons: 7x7 bitmaps drawn at 2x (order matches Category ordinal) ----
    private static final String[][] ICONS = {
        { "....###", "...###.", "#.###..", ".###...", "..##...", ".#.#...", "#......" }, // combat
        { "...#...", "....#..", ".......", "#######", ".......", "....#..", "...#..." }, // movement
        { ".......", "..###..", ".#####.", "##.#.##", ".#####.", "..###..", "......." }, // render
        { ".##....", "####...", ".###...", "..###..", "...###.", "....###", ".....##" }, // utility
        { "..###..", "..###..", "..###..", ".......", "#######", ".#####.", "..#.#.." }, // player
        { "..###..", ".#.#.#.", "#.###.#", "#######", "#.###.#", ".#.#.#.", "..###.." }, // world
        { "..#.#..", ".#####.", "###.###", ".#...#.", "###.###", ".#####.", "..#.#.." }  // settings (gear)
    };

    public static void icon(DrawContext c, int id, int x, int y, int color) { bitmap(c, ICONS[id], x, y, 2, color); }

    private static void bitmap(DrawContext c, String[] rows, int x, int y, int px, int color) {
        for (int j = 0; j < rows.length; j++)
            for (int i = 0; i < rows[j].length(); i++)
                if (rows[j].charAt(i) == '#') c.fill(x + i * px, y + j * px, x + (i + 1) * px, y + (j + 1) * px, color);
    }

    private static final String[] CHEVRON_R = { "#..", ".#.", "..#", ".#.", "#.." };
    private static final String[] CHEVRON_D = { "#...#", ".#.#.", "..#.." };
    private static final String[] CHECK = {
        "........", "......#.", ".....##.", "#...##..", "##.##...", ".###....", "..#.....", "........" };

    public static void chevronRight(DrawContext c, int x, int y, int color) { bitmap(c, CHEVRON_R, x, y, 1, color); }
    public static void chevronDown(DrawContext c, int x, int y, int color)  { bitmap(c, CHEVRON_D, x, y, 1, color); }

    /** Vertical three-dot "more" icon. */
    public static void dots(DrawContext c, int x, int y, int color) {
        for (int i = 0; i < 3; i++) c.fill(x, y + i * 4, x + 2, y + i * 4 + 2, color);
    }

    public static void checkbox(DrawContext c, int x, int y, int size, boolean checked) {
        roundRect(c, x, y, x + size, y + size, 3, checked ? ACCENT : TRACK);
        if (checked) bitmap(c, CHECK, x + (size - 8) / 2, y + (size - 8) / 2, 1, 0xFF050814);
    }
}
