package com.pokemon.battle.fighting;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

/**
 * Weather of each map, drawn with code (no images):
 * - Templo: storm with rain and lightning far away.
 * - Volcan: ash and embers rising, with a red glow that pulses.
 * - Bosque (Angel's map): leaves falling slowly.
 * Everything depends only on the time, so it does not keep state and does not change the fight.
 */
final class StageWeather {

    private static final double LIGHTNING_PERIOD = 8.5;

    private StageWeather() {
    }

    /** Behind the fighters: far lightning and the volcano glow. */
    static void drawBack(Graphics2D g, FightingStage stage, double t, int w, int h, int groundY) {
        Composite old = g.getComposite();
        if (stage == FightingStage.TEMPLO) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));
            g.setColor(new Color(0x101830));
            g.fillRect(0, 0, w, h);
            double phase = t % LIGHTNING_PERIOD;
            int strike = (int) (t / LIGHTNING_PERIOD);
            if (phase < 0.35) {
                // Rayo lejano: una linea quebrada desde el cielo
                double x = 150 + hash(strike) * (w - 300);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.9 * (1 - phase / 0.35))));
                g.setColor(new Color(0xE8EEFF));
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int y = 0;
                double cx = x;
                while (y < groundY - 120) {
                    int ny = y + 40;
                    double nx = cx + (hash(strike * 31 + y) - 0.5) * 70;
                    g.drawLine((int) cx, y, (int) nx, ny);
                    cx = nx;
                    y = ny;
                }
            }
        } else if (stage == FightingStage.VOLCAN) {
            float pulse = (float) (0.10 + 0.07 * Math.sin(t * 2.2));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
            g.setColor(new Color(0xB01800));
            g.fillRect(0, 0, w, h);
        }
        g.setComposite(old);
    }

    /** In front of the fighters: rain, ash or leaves, and the lightning flash. */
    static void drawFront(Graphics2D g, FightingStage stage, double t, int w, int h, int groundY) {
        Composite old = g.getComposite();
        if (stage == FightingStage.TEMPLO) {
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(170, 190, 255, 120));
            for (int i = 0; i < 170; i++) {
                double speed = 0.8 + hash(i) * 0.5;
                double y = (hash(i * 7 + 3) * h + t * 950 * speed) % h;
                double x = (hash(i * 13 + 1) * (w + 200) - t * 220 * speed) % (w + 200);
                if (x < 0) x += w + 200;
                g.drawLine((int) x, (int) y, (int) (x - 7 * speed), (int) (y + 20 * speed));
            }
            // Gotas que rebotan en el piso
            g.setColor(new Color(170, 190, 255, 110));
            for (int i = 0; i < 28; i++) {
                double p = (t * 2.6 + hash(i * 5)) % 1;
                if (p < 0.3) {
                    int x = (int) (hash(i * 17 + (int) (t * 2.6 + hash(i * 5))) * w);
                    int r = (int) (p * 30);
                    g.drawOval(x - r, groundY - 2, r * 2, 4);
                }
            }
            double phase = t % LIGHTNING_PERIOD;
            if (phase < 0.12) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.45 * (1 - phase / 0.12))));
                g.setColor(new Color(0xE8EEFF));
                g.fillRect(0, 0, w, h);
            }
        } else if (stage == FightingStage.VOLCAN) {
            for (int i = 0; i < 130; i++) {
                double speed = 0.6 + hash(i) * 0.8;
                double y = h - ((hash(i * 7 + 2) * h + t * 55 * speed) % h);
                double x = (hash(i * 11 + 5) * w + Math.sin(t * 1.7 + i) * 18) % w;
                boolean ember = hash(i * 3) > 0.62;
                g.setColor(ember ? new Color(255, 150 + (int) (hash(i) * 80), 50, 220) : new Color(90, 80, 80, 170));
                int size = ember ? 3 : 2;
                g.fillRect((int) x, (int) y, size, size);
            }
        } else {
            // Bosque: hojas cayendo despacio
            for (int i = 0; i < 26; i++) {
                double speed = 0.5 + hash(i) * 0.6;
                double y = (hash(i * 9 + 4) * h + t * 45 * speed) % h;
                double x = (hash(i * 5 + 8) * w + Math.sin(t * 0.9 + i) * 40 + t * 12) % w;
                g.setColor(hash(i * 2) > 0.5 ? new Color(0x6FAE3A) : new Color(0xC9A13B));
                int s = 4 + (int) (hash(i * 4) * 3);
                g.fillRect((int) x, (int) y, s, s / 2 + 1);
            }
        }
        g.setComposite(old);
    }

    /** Deterministic pseudo random number in [0, 1) for a given seed. */
    private static double hash(int seed) {
        long x = seed * 0x9E3779B97F4A7C15L;
        x ^= (x >>> 31);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        return ((x >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }
}
