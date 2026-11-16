package com.pokemon.battle.fighting;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Stage of the free fight: volcano cave (resources/fondos/volcan.png) with moving parts
 * drawn on top: two lava falls, bubbles in the pools, the glowing crater, embers that
 * rise and a heat glow at the bottom. Everything depends only on the time.
 */
final class VolcanoStage {

    private static final int W = 1280;
    private static final int H = 720;
    private static final int PX = 4;
    // Cascadas de lava: x izquierda del canal (pantalla), arriba y abajo
    private static final int[] FALLS_X = {256, 976};
    private static final int FALL_TOP = 16;
    private static final int FALL_BOTTOM = 540;
    private static final Color[] LAVA = {new Color(0xA8281A), new Color(0xF0601A),
            new Color(0xFFAA30), new Color(0xFFE88C)};

    private static BufferedImage background;
    private static boolean loaded;

    private VolcanoStage() {
    }

    private static synchronized BufferedImage background() {
        if (!loaded) {
            loaded = true;
            try (InputStream in = VolcanoStage.class.getResourceAsStream("/fondos/volcan.png")) {
                background = in == null ? null : ImageIO.read(in);
            } catch (Exception e) {
                background = null;
            }
        }
        return background;
    }

    static boolean available() {
        return background() != null;
    }

    /** Background and things behind the Pokemon. */
    static void drawBack(Graphics2D g, double time) {
        BufferedImage bg = background();
        if (bg == null) return;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(bg, -20, -12, W + 40, H + 24, null);
        Composite old = g.getComposite();
        crater(g, time);
        for (int i = 0; i < FALLS_X.length; i++) fall(g, FALLS_X[i], time, i);
        embers(g, time, 0.55f);
        g.setComposite(old);
    }

    /** Heat at the bottom and a few embers in front of the Pokemon. */
    static void drawFront(Graphics2D g, double time) {
        if (background() == null) return;
        Composite old = g.getComposite();
        // Calor del borde de lava que sube y baja, en franjas
        double pulse = 0.5 + 0.5 * Math.sin(time * 1.7);
        for (int band = 0; band < 4; band++) {
            float a = (float) ((0.10 + 0.05 * pulse) * (4 - band) / 4.0);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.setColor(new Color(255, 110, 30));
            g.fillRect(-20, H - 12 - band * 12, W + 40, 12);
        }
        embers(g, time + 37, 0.9f);
        g.setComposite(old);
    }

    private static void fall(Graphics2D g, int x0, double time, int seed) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        int columns = 9;   // 36 px de ancho, en columnas de 4
        for (int c = 0; c < columns; c++) {
            double speed = 220 + (c * 37 + seed * 53) % 90;
            double offset = (time * speed + c * 61 + seed * 140) % 64;
            for (int y = FALL_TOP; y < FALL_BOTTOM; y += PX) {
                double v = ((y - offset) % 64 + 64) % 64;
                int k;
                if (c == 0 || c == columns - 1) k = v < 40 ? 0 : 1;
                else if (c == 1 || c == columns - 2) k = v < 24 ? 1 : v < 52 ? 2 : 1;
                else k = v < 10 ? 3 : v < 36 ? 2 : 1;
                g.setColor(LAVA[k]);
                g.fillRect(x0 + c * PX, y, PX, PX);
            }
        }
        // Salpicaduras y burbujas en el charco de abajo
        int cx = x0 + columns * PX / 2;
        for (int i = 0; i < 6; i++) {
            double life = (time * 1.3 + i * 0.37 + seed * 0.21) % 1.0;
            double dir = (i % 2 == 0 ? -1 : 1) * (8 + i * 6);
            int sx = (int) (cx + dir * life * 2.2) / PX * PX;
            int sy = (int) (FALL_BOTTOM - Math.sin(life * Math.PI) * (20 + i * 5)) / PX * PX;
            g.setColor(LAVA[life < 0.5 ? 3 : 2]);
            g.fillRect(sx, sy, PX, PX);
        }
        for (int i = 0; i < 3; i++) {
            double life = (time * 0.8 + i * 0.33 + seed * 0.5) % 1.0;
            int bx = cx - 40 + i * 32 + seed * 8;
            int size = life < 0.8 ? PX * 2 : PX * 3;
            g.setColor(LAVA[life < 0.8 ? 2 : 3]);
            g.fillRect(bx, FALL_BOTTOM + 8 - size / 2, size, size);
        }
    }

    private static void crater(Graphics2D g, double time) {
        double pulse = 0.5 + 0.5 * Math.sin(time * 2.1);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.25 + 0.2 * pulse)));
        g.setColor(new Color(255, 140, 40));
        g.fillRect(584, 236, 116, 12);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.12 + 0.1 * pulse)));
        g.fillRect(560, 224, 164, 8);
        // Chorritos de lava que saltan del crater
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        for (int i = 0; i < 5; i++) {
            double life = (time * 0.7 + i * 0.2) % 1.0;
            double vx = (i - 2) * 22;
            int x = (int) (640 + vx * life) / PX * PX;
            int y = (int) (236 - 90 * life + 120 * life * life) / PX * PX;
            g.setColor(LAVA[life < 0.4 ? 3 : 2]);
            g.fillRect(x, y, PX, PX);
        }
    }

    private static void embers(Graphics2D g, double time, float strength) {
        Color[] colors = {new Color(255, 200, 90), new Color(255, 140, 50), new Color(255, 90, 40)};
        for (int i = 0; i < 26; i++) {
            double period = 4 + (i % 5);
            double life = ((time + i * 1.7) % period) / period;
            double x = (i * 151 + Math.sin(time * 0.8 + i) * 30) % W;
            double y = H - 20 - life * (H - 60);
            float a = (float) (strength * Math.min(1, (1 - life) * 2.5));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.setColor(colors[i % 3]);
            int size = i % 4 == 0 ? 8 : 4;
            g.fillRect((int) x / PX * PX, (int) y / PX * PX, size, size);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}
