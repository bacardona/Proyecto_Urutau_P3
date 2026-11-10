package com.pokemon.battle.ui;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Pixel art background of the battle field: sky with clouds, hills, a line of
 * trees, grass and the two platforms where the Pokemon stand.
 * <p>
 * The picture is drawn once at a small size (one pixel = one "big pixel") and
 * then scaled up without smoothing, so it looks like the old games.
 */
public final class BattleBackground {

    /** How many screen pixels each pixel of the picture takes. */
    public static final int SCALE = 4;

    // Paleta estilo GBA
    private static final int SKY_TOP = 0x70C0F0;
    private static final int SKY_MID = 0x90D0F8;
    private static final int SKY_LOW = 0xC0E8F8;
    private static final int CLOUD = 0xF8F8F8;
    private static final int CLOUD_SHADE = 0xD0E4F0;
    private static final int HILL = 0x88C8B0;
    private static final int HILL_DARK = 0x70B098;
    private static final int TREE = 0x48A058;
    private static final int TREE_LIGHT = 0x68B868;
    private static final int TREE_DARK = 0x307840;
    private static final int GRASS = 0xA8E088;
    private static final int GRASS_BAND = 0x98D478;
    private static final int TUFT = 0x70B058;
    private static final int FLOWER = 0xF8F0A0;
    private static final int PLATFORM_RIM = 0x588840;
    private static final int PLATFORM = 0x90C868;
    private static final int PLATFORM_TOP = 0xB8E090;
    private static final int PLATFORM_DIRT = 0xC8B070;

    private static BufferedImage cached;

    private BattleBackground() {
    }

    /**
     * Builds the picture the first time and then reuses it.
     *
     * @param width        width of the field on screen
     * @param height       height of the field on screen
     * @param rivalX       center of the rival platform on screen (relative to the field)
     * @param rivalFeetY   where the feet of the rival are (relative to the field)
     * @param playerX      center of the player platform on screen (relative to the field)
     * @param playerFeetY  where the feet of the player are (relative to the field)
     * @return the small picture; draw it scaled by {@link #SCALE}
     */
    public static synchronized BufferedImage get(int width, int height, int rivalX, int rivalFeetY,
                                                 int playerX, int playerFeetY) {
        int w = (width + SCALE - 1) / SCALE;
        int h = (height + SCALE - 1) / SCALE;
        if (cached == null || cached.getWidth() != w || cached.getHeight() != h) {
            cached = build(w, h, rivalX / SCALE, rivalFeetY / SCALE, playerX / SCALE, playerFeetY / SCALE);
        }
        return cached;
    }

    private static BufferedImage build(int w, int h, int rx, int ry, int px, int py) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int horizon = (int) (h * 0.38);
        Random random = new Random(7); // semilla fija: el fondo siempre sale igual

        drawSky(img, w, horizon);
        drawCloud(img, w / 6, horizon / 4, 9);
        drawCloud(img, w / 2 + 6, horizon / 6, 12);
        drawCloud(img, w - w / 7, horizon / 3, 7);
        drawHills(img, w, horizon);
        drawTrees(img, w, horizon, random);
        drawGrass(img, w, h, horizon, random);
        // El rival queda parado un poco arriba del centro de su plataforma
        drawPlatform(img, rx, ry + 1, 48, 9);
        drawPlatform(img, px, py, 56, 10);
        return img;
    }

    // Cielo en franjas con transicion en tablero de ajedrez (dithering)
    private static void drawSky(BufferedImage img, int w, int horizon) {
        int band1 = horizon / 3;
        int band2 = horizon * 2 / 3;
        for (int y = 0; y < horizon; y++) {
            for (int x = 0; x < w; x++) {
                int color;
                if (y < band1) {
                    color = SKY_TOP;
                } else if (y == band1) {
                    color = (x + y) % 2 == 0 ? SKY_TOP : SKY_MID;
                } else if (y < band2) {
                    color = SKY_MID;
                } else if (y == band2) {
                    color = (x + y) % 2 == 0 ? SKY_MID : SKY_LOW;
                } else {
                    color = SKY_LOW;
                }
                img.setRGB(x, y, color);
            }
        }
    }

    // Nube hecha con tres bultos redondos y sombra abajo
    private static void drawCloud(BufferedImage img, int cx, int cy, int size) {
        int[][] puffs = {{0, 0, size / 2}, {-size / 2, 1, size / 3}, {size / 2, 1, size / 3}};
        for (int[] p : puffs) {
            fillEllipse(img, cx + p[0], cy + p[1], p[2] + 1, p[2] / 2 + 1, CLOUD_SHADE);
        }
        for (int[] p : puffs) {
            fillEllipse(img, cx + p[0], cy + p[1] - 1, p[2], p[2] / 2, CLOUD);
        }
    }

    // Colinas lejanas con borde ondulado
    private static void drawHills(BufferedImage img, int w, int horizon) {
        for (int x = 0; x < w; x++) {
            double wave = Math.sin(x / 11.0) * 2.5 + Math.sin(x / 4.3) * 1.0;
            int top = horizon - 9 + (int) Math.round(wave);
            for (int y = Math.max(0, top); y < horizon; y++) {
                img.setRGB(x, y, y == top ? HILL_DARK : HILL);
            }
        }
    }

    // Fila de arboles redondos justo en el horizonte
    private static void drawTrees(BufferedImage img, int w, int horizon, Random random) {
        for (int x = -4; x < w + 4; x += 7) {
            int r = 3 + random.nextInt(2);
            int cy = horizon - 2 - random.nextInt(2);
            fillEllipse(img, x, cy, r + 1, r, TREE_DARK);
            fillEllipse(img, x, cy, r, r - 1, TREE);
            fillEllipse(img, x - 1, cy - 1, r / 2, r / 2 - 1, TREE_LIGHT);
        }
        for (int x = 0; x < w; x++) {
            setSafe(img, x, horizon, TREE_DARK);
        }
    }

    // Pasto con franjas suaves, matas y algunas flores
    private static void drawGrass(BufferedImage img, int w, int h, int horizon, Random random) {
        for (int y = horizon + 1; y < h; y++) {
            int color = ((y - horizon) / 6) % 2 == 0 ? GRASS : GRASS_BAND;
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, color);
            }
        }
        int tufts = w * (h - horizon) / 40;
        for (int i = 0; i < tufts; i++) {
            int x = random.nextInt(w);
            int y = horizon + 3 + random.nextInt(Math.max(1, h - horizon - 3));
            if (random.nextInt(9) == 0) {
                setSafe(img, x, y, FLOWER);
            } else {
                // Mata en forma de "v": tres pixeles
                setSafe(img, x, y, TUFT);
                setSafe(img, x - 1, y - 1, TUFT);
                setSafe(img, x + 1, y - 1, TUFT);
            }
        }
    }

    // Plataforma ovalada: borde oscuro, cuerpo, brillo arriba y tierra abajo
    private static void drawPlatform(BufferedImage img, int cx, int cy, int rx, int ry) {
        fillEllipse(img, cx, cy + 2, rx + 1, ry + 1, PLATFORM_DIRT);
        fillEllipse(img, cx, cy, rx + 1, ry + 1, PLATFORM_RIM);
        fillEllipse(img, cx, cy, rx, ry, PLATFORM);
        fillEllipse(img, cx, cy - 1, rx - 4, ry - 2, PLATFORM_TOP);
    }

    private static void fillEllipse(BufferedImage img, int cx, int cy, int rx, int ry, int color) {
        if (rx <= 0 || ry <= 0) {
            return;
        }
        for (int y = -ry; y <= ry; y++) {
            for (int x = -rx; x <= rx; x++) {
                double d = (x * x) / (double) (rx * rx) + (y * y) / (double) (ry * ry);
                if (d <= 1.0) {
                    setSafe(img, cx + x, cy + y, color);
                }
            }
        }
    }

    private static void setSafe(BufferedImage img, int x, int y, int color) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, color);
        }
    }
}
