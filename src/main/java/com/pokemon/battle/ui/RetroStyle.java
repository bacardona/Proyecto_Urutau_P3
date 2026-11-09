package com.pokemon.battle.ui;

import com.pokemon.battle.model.MoveCategory;
import com.pokemon.battle.model.Type;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Shared look of the retro screens: colors, the Jersey 10 font and the drawing
 * helpers that give depth to panels, frames, badges and texts.
 */
public final class RetroStyle {

    public static final Color CREAM = new Color(0xF7F3E0);
    public static final Color GRID = new Color(0xE6DFC6);
    public static final Color DARK = new Color(0x2D3A52);
    public static final Color OUTLINE = new Color(0x1A1F2B);
    public static final Color RED = new Color(0xD3352F);
    public static final Color RED_DARK = new Color(0x9E2420);
    public static final Color PINK = new Color(0xF59A92);
    public static final Color BLUE = new Color(0x3D6FD1);
    public static final Color BLUE_DARK = new Color(0x24479A);
    public static final Color WHITE = Color.WHITE;
    public static final Color PANEL_BOTTOM = new Color(0xE4E0D2);
    public static final Color SHADOW_LIGHT = new Color(0xB9B4A2);

    private static final Map<Type, Color> TYPE_COLORS = new EnumMap<>(Type.class);
    private static final Map<Type, String> TYPE_NAMES = new EnumMap<>(Type.class);
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    private static Font baseFont;

    static {
        registerType(Type.NORMAL, 0xA8A878, "NORMAL");
        registerType(Type.FIRE, 0xF08030, "FUEGO");
        registerType(Type.WATER, 0x6890F0, "AGUA");
        registerType(Type.ELECTRIC, 0xE8B820, "ELECTRICO");
        registerType(Type.GRASS, 0x78C850, "PLANTA");
        registerType(Type.ICE, 0x78C8C8, "HIELO");
        registerType(Type.FIGHTING, 0xC03028, "LUCHA");
        registerType(Type.POISON, 0xA040A0, "VENENO");
        registerType(Type.GROUND, 0xD0A850, "TIERRA");
        registerType(Type.FLYING, 0xA890F0, "VOLADOR");
        registerType(Type.PSYCHIC, 0xF85888, "PSIQUICO");
        registerType(Type.BUG, 0xA8B820, "BICHO");
        registerType(Type.ROCK, 0xB8A038, "ROCA");
        registerType(Type.GHOST, 0x705898, "FANTASMA");
        registerType(Type.DRAGON, 0x7038F8, "DRAGON");
        registerType(Type.DARK, 0x705848, "SINIESTRO");
        registerType(Type.STEEL, 0x9898B8, "ACERO");
        registerType(Type.FAIRY, 0xE890A8, "HADA");
        registerType(Type.NONE, 0x9AA0AA, "---");
    }

    private RetroStyle() {
    }

    private static void registerType(Type type, int rgb, String name) {
        TYPE_COLORS.put(type, new Color(rgb));
        TYPE_NAMES.put(type, name);
    }

    /**
     * Returns the Jersey 10 font in the given size. When the file can not be
     * loaded a normal monospaced font is used, so the game never crashes.
     *
     * @param size size in points
     * @return the font
     */
    public static Font font(float size) {
        try {
            if (baseFont == null) {
                try (InputStream in = RetroStyle.class.getResourceAsStream("/fonts/Jersey10-Regular.ttf")) {
                    baseFont = Font.createFont(Font.TRUETYPE_FONT, in);
                }
            }
            return baseFont.deriveFont(size);
        } catch (Exception e) {
            return new Font(Font.MONOSPACED, Font.BOLD, (int) size);
        }
    }

    /**
     * @param type a Pokemon type
     * @return the color used for its badge
     */
    public static Color typeColor(Type type) {
        return TYPE_COLORS.getOrDefault(type, TYPE_COLORS.get(Type.NONE));
    }

    /**
     * @param type a Pokemon type
     * @return its name in Spanish, in capital letters
     */
    public static String typeName(Type type) {
        return TYPE_NAMES.getOrDefault(type, type.name());
    }

    /**
     * @param category category of a move
     * @return its name in Spanish
     */
    public static String categoryName(MoveCategory category) {
        switch (category) {
            case PHYSICAL:
                return "FISICO";
            case SPECIAL:
                return "ESPECIAL";
            default:
                return "ESTADO";
        }
    }

    /**
     * Paints the notebook grid used as background.
     */
    public static void drawGrid(Graphics2D g, int width, int height, int cell) {
        g.setColor(CREAM);
        g.fillRect(0, 0, width, height);
        g.setColor(GRID);
        for (int x = 0; x < width; x += cell) {
            g.fillRect(x, 0, 2, height);
        }
        for (int y = 0; y < height; y += cell) {
            g.fillRect(0, y, width, 2);
        }
    }

    /**
     * White panel with black outline, a light line on top and a soft dark
     * gradient at the bottom, so it does not look flat.
     */
    public static void drawPanel(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setPaint(new GradientPaint(x, y + h * 0.6f, WHITE, x, y + h, PANEL_BOTTOM));
        g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 10, 10);
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(x + 8, y + 5, w - 16, 2);
    }

    /**
     * Red frame with a pink line inside, like the boxes of the old games.
     * The inside is left white.
     */
    public static void drawRedFrame(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w, h, 16, 16);
        g.setColor(RED);
        g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 12, 12);
        g.setColor(PINK);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x + 6, y + 6, w - 13, h - 13, 10, 10);
        g.setPaint(new GradientPaint(x, y + h * 0.5f, WHITE, x, y + h, PANEL_BOTTOM));
        g.fillRoundRect(x + 10, y + 10, w - 20, h - 20, 8, 8);
        g.setStroke(new BasicStroke(1));
    }

    /**
     * Draws a text with a second color one pixel step below and to the right,
     * so the letters look embossed.
     */
    public static void shadowText(Graphics2D g, String text, int x, int y, Color main, Color shadow) {
        g.setColor(shadow);
        g.drawString(text, x + 2, y + 2);
        g.setColor(main);
        g.drawString(text, x, y);
    }

    /**
     * Draws a type badge: colored box with outline and white embossed text.
     *
     * @return the width used, to place the next badge
     */
    public static int drawTypeBadge(Graphics2D g, Type type, int x, int y, int w, int h) {
        // Si existe la imagen del tipo (resources/tipos/AGUA.png...) se usa esa
        BufferedImage image = image("/tipos/" + typeName(type) + ".png");
        if (image != null) {
            drawPixelImage(g, image, x, y, w, h);
            return w;
        }
        Color color = typeColor(type);
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(color);
        g.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 6, 6);
        g.setColor(color.brighter());
        g.fillRect(x + 5, y + 4, w - 10, 2);
        g.setColor(color.darker());
        g.fillRect(x + 4, y + h - 6, w - 8, 3);
        String name = typeName(type);
        int textWidth = g.getFontMetrics().stringWidth(name);
        int textY = y + (h + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
        shadowText(g, name, x + (w - textWidth) / 2, textY, WHITE, OUTLINE);
        return w;
    }

    /**
     * Draws the icon of the category of a move (resources/categorias/FISICO.png...).
     * When the image is missing, the name is written instead.
     */
    public static void drawCategory(Graphics2D g, MoveCategory category, int x, int y, int w, int h) {
        BufferedImage image = image("/categorias/" + categoryName(category) + ".png");
        if (image != null) {
            drawPixelImage(g, image, x, y, w, h);
        } else {
            shadowText(g, categoryName(category), x, centeredBaseline(g, y, h), DARK, SHADOW_LIGHT);
        }
    }

    /**
     * Color of a base stat bar according to its value:
     * 1-29 red, 30-59 orange, 60-89 yellow, 90-119 lime green,
     * 120-149 dark green and 150 or more turquoise.
     *
     * @param value value of the stat
     * @return {fill color, border color}
     */
    public static Color[] statColors(int value) {
        if (value < 30) {
            return new Color[]{new Color(0xFF5A5A), new Color(0xD93030)};
        } else if (value < 60) {
            return new Color[]{new Color(0xFFAA5A), new Color(0xE87A20)};
        } else if (value < 90) {
            return new Color[]{new Color(0xF8D452), new Color(0xD4A017)};
        } else if (value < 120) {
            return new Color[]{new Color(0xA6EB4F), new Color(0x6DB52A)};
        } else if (value < 150) {
            return new Color[]{new Color(0x34B85F), new Color(0x1F8540)};
        }
        return new Color[]{new Color(0x45CFE0), new Color(0x1E97AE)};
    }

    /**
     * Draws a stat bar with rounded ends: colored fill and a darker border.
     */
    public static void drawStatBar(Graphics2D g, int x, int y, int maxWidth, int h, int value, int maxValue) {
        int width = Math.max(h, (int) (maxWidth * Math.min(value, maxValue) / (double) maxValue));
        Color[] colors = statColors(value);
        g.setColor(colors[1]);
        g.fillRoundRect(x, y, width, h, h, h);
        g.setColor(colors[0]);
        g.fillRoundRect(x + 2, y + 2, width - 4, h - 4, h - 4, h - 4);
        g.setColor(new Color(255, 255, 255, 110));
        g.fillRoundRect(x + 5, y + 4, Math.max(0, width - 10), 3, 3, 3);
    }

    /**
     * Draws a pixel-art image scaled to the box without blurring it.
     */
    public static void drawPixelImage(Graphics2D g, BufferedImage image, int x, int y, int w, int h) {
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, x, y, w, h, null);
        if (old != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        }
    }

    // Carga una imagen de resources una sola vez; devuelve null si no existe
    private static synchronized BufferedImage image(String path) {
        if (IMAGES.containsKey(path)) {
            return IMAGES.get(path);
        }
        BufferedImage image = null;
        URL url = RetroStyle.class.getResource(path);
        if (url != null) {
            try {
                image = ImageIO.read(url);
            } catch (Exception e) {
                image = null;
            }
        }
        IMAGES.put(path, image);
        return image;
    }

    /**
     * Frame of a list: black outline, red strip on the left and white inside.
     */
    public static void drawListFrame(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setPaint(new GradientPaint(x, y, RED, x + 26, y, RED_DARK));
        g.fillRoundRect(x + 3, y + 3, 30, h - 6, 10, 10);
        g.setPaint(new GradientPaint(0, y + h * 0.7f, WHITE, 0, y + h, PANEL_BOTTOM));
        g.fillRect(x + 30, y + 3, w - 33, h - 6);
    }

    /**
     * Highlight of the selected row: red box that sticks out a little, with a
     * light pink inside.
     */
    public static void drawSelectedRow(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(RED);
        g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 10, 10);
        g.setColor(new Color(0xFDECEA));
        g.fillRoundRect(x + 8, y + 8, w - 16, h - 16, 6, 6);
    }

    /**
     * Round marker at the start of a list row; filled in red when selected.
     */
    public static void drawListDot(Graphics2D g, int cx, int cy, boolean selected) {
        g.setColor(OUTLINE);
        g.fillOval(cx - 8, cy - 8, 16, 16);
        g.setColor(selected ? RED : WHITE);
        g.fillOval(cx - 5, cy - 5, 10, 10);
    }

    /**
     * Vertical scroll bar. Only the track is drawn when everything fits.
     *
     * @param first   first visible row
     * @param visible how many rows fit
     * @param total   total rows
     */
    public static void drawScrollBar(Graphics2D g, int x, int top, int height, int first, int visible, int total) {
        g.setColor(new Color(0xD9D4C2));
        g.fillRoundRect(x, top, 8, height, 8, 8);
        if (total > visible) {
            int thumbH = Math.max(30, height * visible / total);
            int thumbY = top + (height - thumbH) * first / (total - visible);
            g.setColor(BLUE_DARK);
            g.fillRoundRect(x - 2, thumbY, 12, thumbH, 8, 8);
            g.setColor(BLUE);
            g.fillRoundRect(x, thumbY + 2, 6, thumbH - 4, 6, 6);
        }
    }

    /**
     * Draws a small key hint button of the bottom bar, like "ESC VOLVER".
     */
    public static void drawKeyButton(Graphics2D g, String text, int x, int y, int w, int h) {
        drawButtonBox(g, x, y, w, h);
        int textWidth = g.getFontMetrics().stringWidth(text);
        shadowText(g, text, x + (w - textWidth) / 2, centeredBaseline(g, y, h), DARK, SHADOW_LIGHT);
    }

    /**
     * Key hint button with two arrows drawn as triangles (the pixel font has no
     * arrow characters), for example the up/down arrows followed by "MOVER".
     *
     * @param vertical true for up/down arrows, false for left/right arrows
     */
    public static void drawArrowKeyButton(Graphics2D g, boolean vertical, String text, int x, int y, int w, int h) {
        drawButtonBox(g, x, y, w, h);
        int textWidth = g.getFontMetrics().stringWidth(text);
        int iconWidth = 30;
        int startX = x + (w - iconWidth - 10 - textWidth) / 2;
        int cy = y + h / 2;
        g.setColor(DARK);
        if (vertical) {
            g.fillPolygon(triangle(startX + 7, cy, true, true));
            g.fillPolygon(triangle(startX + 23, cy, true, false));
        } else {
            g.fillPolygon(triangle(startX + 7, cy, false, true));
            g.fillPolygon(triangle(startX + 23, cy, false, false));
        }
        shadowText(g, text, startX + iconWidth + 10, centeredBaseline(g, y, h), DARK, SHADOW_LIGHT);
    }

    // Triangulo de 12 px centrado en (cx, cy): arriba/abajo o izquierda/derecha
    private static Polygon triangle(int cx, int cy, boolean vertical, boolean first) {
        int s = 6;
        if (vertical) {
            return first
                    ? new Polygon(new int[]{cx - s, cx + s, cx}, new int[]{cy + s / 2, cy + s / 2, cy - s}, 3)
                    : new Polygon(new int[]{cx - s, cx + s, cx}, new int[]{cy - s / 2, cy - s / 2, cy + s}, 3);
        }
        return first
                ? new Polygon(new int[]{cx + s / 2, cx + s / 2, cx - s}, new int[]{cy - s, cy + s, cy}, 3)
                : new Polygon(new int[]{cx - s / 2, cx - s / 2, cx + s}, new int[]{cy - s, cy + s, cy}, 3);
    }

    private static void drawButtonBox(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(OUTLINE);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setPaint(new GradientPaint(x, y, WHITE, x, y + h, PANEL_BOTTOM));
        g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 8, 8);
    }

    private static int centeredBaseline(Graphics2D g, int y, int h) {
        return y + (h + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
    }
}
