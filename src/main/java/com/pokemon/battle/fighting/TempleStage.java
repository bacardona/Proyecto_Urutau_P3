package com.pokemon.battle.fighting;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Stage of the free fight: jungle temple (resources/fondos/templo.png) with moving
 * parts drawn on top: light rays from the hole in the roof, dust, falling leaves,
 * fireflies and the fire of the torches. Everything depends only on the time, so it
 * does not keep state.
 */
final class TempleStage {

    private static final int W = 1280;
    private static final int H = 720;
    // Antorchas en las columnas y brasero del altar (coordenadas de la pantalla)
    private static final int[][] TORCHES = {{364, 248}, {916, 248}};
    private static final int[] ALTAR = {640, 392};

    private static BufferedImage background;
    private static boolean loaded;

    private TempleStage() {
    }

    private static synchronized BufferedImage background() {
        if (!loaded) {
            loaded = true;
            try (InputStream in = TempleStage.class.getResourceAsStream("/fondos/templo.png")) {
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
        // Un poco mas grande para que no se vean bordes cuando tiembla la pantalla
        g.drawImage(bg, -20, -12, W + 40, H + 24, null);
        Composite old = g.getComposite();
        for (int[] torch : TORCHES) flame(g, torch[0], torch[1], time, 1.0, torch[0]);
        flame(g, ALTAR[0], ALTAR[1], time, 1.25, 7);
        fireflies(g, time);
        leaves(g, time);
        g.setComposite(old);
    }

    /** Light from the roof and dust floating in it, on top of the Pokemon. */
    static void drawFront(Graphics2D g, double time) {
        if (background() == null) return;
        Composite old = g.getComposite();
        // Rayos de luz que cambian un poco de intensidad
        for (int i = 0; i < 3; i++) {
            double pulse = 0.5 + 0.5 * Math.sin(time * (0.6 + i * 0.27) + i * 2);
            float alpha = (float) (0.07 + 0.06 * pulse);
            int topL = 540 + i * 50;
            int topR = topL + 70;
            int bottomL = 420 + i * 120 - 40;
            int bottomR = bottomL + 220;
            Polygon ray = new Polygon(new int[]{topL, topR, bottomR, bottomL}, new int[]{40, 40, 700, 700}, 4);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setPaint(new GradientPaint(0, 40, new Color(255, 248, 200), 0, 700, new Color(255, 248, 200, 0)));
            g.fill(ray);
        }
        // Polvo dentro de la luz
        for (int i = 0; i < 46; i++) {
            double seed = i * 12.9898;
            double speed = 8 + (i % 7) * 3;
            double x = 470 + (Math.sin(seed) * 0.5 + 0.5) * 340 + Math.sin(time * 0.7 + i) * 18;
            double y = ((Math.cos(seed) * 0.5 + 0.5) * 640 + time * speed) % 660 + 40;
            double twinkle = 0.4 + 0.6 * Math.abs(Math.sin(time * 2 + i * 1.7));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.55 * twinkle)));
            g.setColor(new Color(255, 250, 215));
            int s = i % 5 == 0 ? 4 : 3;
            g.fillRect((int) x, (int) y, s, s);
        }
        g.setComposite(old);
    }

    private static void flame(Graphics2D g, int x, int y, double time, double size, int seed) {
        // Resplandor
        float glow = (float) (0.30 + 0.08 * Math.sin(time * 9 + seed));
        float radius = (float) (110 * size);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glow));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y - 10), radius, new float[]{0f, 1f},
                new Color[]{new Color(255, 170, 80, 200), new Color(255, 120, 40, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillOval((int) (x - radius), (int) (y - 10 - radius), (int) (radius * 2), (int) (radius * 2));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        // Llama en pixeles: tres capas que se mueven
        int px = 4;
        double flicker = Math.sin(time * 13 + seed) * 0.5 + Math.sin(time * 21 + seed * 2) * 0.5;
        int[][] layers = {{0xD8301A, 7, 9}, {0xFF8A2A, 5, 7}, {0xFFE27A, 3, 4}, {0xFFFFFF, 1, 2}};
        for (int[] layer : layers) {
            g.setColor(new Color(layer[0]));
            int halfW = (int) (layer[1] * size);
            int height = (int) ((layer[2] + flicker * 1.5) * size);
            for (int row = 0; row < height; row++) {
                double t = row / (double) height;
                int w = (int) Math.round(halfW * (1 - t * t));
                int sway = (int) Math.round(Math.sin(time * 10 + row * 0.6 + seed) * t * 1.6);
                g.fillRect(x + (sway - w) * px / 2, y - row * px, Math.max(1, w) * px, px);
            }
        }
        // Chispas que suben
        for (int i = 0; i < 4; i++) {
            double life = (time * 0.9 + i * 0.25 + seed * 0.13) % 1.0;
            int sx = x + (int) (Math.sin(i * 3.1 + seed + time * 2) * 10 * size);
            int sy = y - 30 - (int) (life * 60 * size);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - life)));
            g.setColor(new Color(255, 210, 110));
            g.fillRect(sx, sy, 4, 4);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private static void fireflies(Graphics2D g, double time) {
        for (int i = 0; i < 14; i++) {
            double x = (i * 97 + Math.sin(time * 0.5 + i) * 40 + time * 6 * (i % 2 == 0 ? 1 : -1)) % W;
            if (x < 0) x += W;
            double y = 160 + (i * 53) % 380 + Math.sin(time * 0.9 + i * 1.3) * 20;
            double blink = Math.max(0, Math.sin(time * 1.6 + i * 2.2));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.25 * blink)));
            g.setColor(new Color(220, 255, 140));
            g.fillOval((int) x - 6, (int) y - 6, 12, 12);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.9 * blink)));
            g.fillRect((int) x - 2, (int) y - 2, 4, 4);
        }
    }

    private static void leaves(Graphics2D g, double time) {
        Color[] colors = {new Color(0x3C7034), new Color(0x66A046), new Color(0x8CC35A)};
        for (int i = 0; i < 7; i++) {
            double period = 7 + i * 1.3;
            double life = ((time + i * 2.9) % period) / period;
            double x = 520 + i * 40 + Math.sin(life * Math.PI * 4 + i) * 60;
            double y = 30 + life * 660;
            double angle = Math.sin(life * Math.PI * 6 + i) * 0.9;
            Graphics2D leaf = (Graphics2D) g.create();
            leaf.translate(x, y);
            leaf.rotate(angle);
            leaf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, (1 - life) * 3)));
            leaf.setColor(colors[i % 3]);
            leaf.fillRect(-8, -3, 16, 6);
            leaf.setColor(colors[(i + 1) % 3]);
            leaf.fillRect(-8, -1, 16, 2);
            leaf.dispose();
        }
    }
}
