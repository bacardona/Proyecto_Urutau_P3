package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.RetroStyle;
import com.pokemon.battle.ui.SpriteLoader;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Top bar of the free fight: portrait of the active Pokemon, life in blocks that
 * change color (green, yellow, red), a trail that shows the damage just received,
 * the evolution / ultimate bar and the team of each trainer.
 */
final class FightingHud {

    private static final Color INK = new Color(0x141828);
    private static final Color EMPTY = new Color(0x3C4054);
    private static final Color EMPTY_TOP = new Color(0x4A4F66);
    private static final Color[] TEAM_COLOR = {new Color(0xE8784A), new Color(0x4A8FE8)};
    private static final int SEGMENTS = 20;

    private final SpriteLoader sprites;
    // Vida que se muestra con retraso (la estela roja del dano)
    private final Map<Fighter, Double> trail = new IdentityHashMap<>();
    private final Map<String, BufferedImage> icons = new java.util.HashMap<>();

    FightingHud(SpriteLoader sprites) {
        this.sprites = sprites;
    }

    void clear() {
        trail.clear();
    }

    void draw(Graphics2D g, FightingGameSession session, int frameIndex, Runnable repaint, double dt) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int side = 0; side < 2; side++) {
            Fighter f = session.getFighters().get(side);
            FightingTeam team = side == 0 ? session.getPlayerOneTeam() : session.getPlayerTwoTeam();
            drawSide(g, session, f, team, side, frameIndex, repaint, dt);
        }
        // Emblema del centro
        int cx = session.getWidth() / 2;
        g.setColor(INK);
        g.fillRect(cx - 44, 26, 88, 70);
        g.setColor(new Color(0xF6F1E2));
        g.fillRect(cx - 40, 30, 80, 62);
        g.setColor(new Color(0xD9CFB4));
        g.fillRect(cx - 40, 84, 80, 8);
        centered(g, "VS", cx, 80, 52, INK);
    }

    private void drawSide(Graphics2D g, FightingGameSession session, Fighter f, FightingTeam team,
                          int side, int frameIndex, Runnable repaint, double dt) {
        boolean left = side == 0;
        int width = session.getWidth();
        int portraitX = left ? 36 : width - 36 - 104;
        int barW = 420;
        int barX = left ? portraitX + 116 : portraitX - 12 - barW;

        // Retrato
        g.setColor(INK);
        g.fillRect(portraitX, 22, 104, 104);
        g.setColor(TEAM_COLOR[side]);
        g.fillRect(portraitX + 5, 27, 94, 94);
        g.setColor(TEAM_COLOR[side].brighter());
        g.fillRect(portraitX + 5, 27, 94, 10);
        BufferedImage img = icon(f.getPokemonId(), !left, frameIndex, repaint);
        if (img != null) {
            int h = 84;
            int w = Math.min(90, Math.round(h * img.getWidth() / (float) img.getHeight()));
            g.drawImage(img, portraitX + 52 - w / 2, 32, w, h, null);
        }

        // Nombre
        String name = f.getName().toUpperCase();
        g.setFont(RetroStyle.font(36));
        FontMetrics m = g.getFontMetrics();
        int nameX = left ? barX : barX + barW - m.stringWidth(name);
        g.setColor(INK);
        g.drawString(name, nameX + 2, 54);
        g.setColor(Color.WHITE);
        g.drawString(name, nameX, 52);

        // Vida en bloques con la estela del dano
        double ratio = Math.max(0, f.getHealth() / (double) f.getMaxHealth());
        double shown = trail.getOrDefault(f, ratio);
        if (shown < ratio) shown = ratio;
        else shown = Math.max(ratio, shown - dt * 0.35);
        trail.put(f, shown);
        g.setColor(INK);
        g.fillRect(barX, 60, barW, 34);
        double segW = (barW - 8) / (double) SEGMENTS;
        Color full = ratio > 0.5 ? new Color(0x58D660) : ratio > 0.25 ? new Color(0xFAC83C) : new Color(0xF05046);
        boolean blink = ratio <= 0.25 && (frameIndex / 3) % 2 == 0;
        for (int i = 0; i < SEGMENTS; i++) {
            int k = left ? i : SEGMENTS - 1 - i;
            int x0 = (int) Math.round(barX + 4 + k * segW);
            int x1 = (int) Math.round(barX + 4 + (k + 1) * segW) - 2;
            double from = i / (double) SEGMENTS;
            Color body;
            Color top;
            if (from < ratio) {
                body = blink ? full.brighter() : full;
                top = brighten(body, 60);
            } else if (from < shown) {
                body = new Color(0xFF6B5E);
                top = new Color(0xFFB2A8);
            } else {
                body = EMPTY;
                top = EMPTY_TOP;
            }
            g.setColor(body);
            g.fillRect(x0, 64, x1 - x0, 26);
            g.setColor(top);
            g.fillRect(x0, 64, x1 - x0, 6);
        }

        // Barra de evolucion / ulti
        String label = energyLabel(f, left ? "R" : "P");
        int evoW = 260;
        int evoX = left ? barX : barX + barW - evoW;
        g.setColor(INK);
        g.fillRect(evoX, 100, evoW, 16);
        double energy = Math.min(1, f.getEvolutionEnergy() / 100.0);
        boolean ready = f.isEvolutionReady();
        Color evo = ready && (frameIndex / 4) % 2 == 0 ? new Color(0xFFF0A0) : new Color(0xFFD25A);
        int fill = (int) Math.round((evoW - 6) * energy);
        g.setColor(evo);
        if (left) g.fillRect(evoX + 3, 103, fill, 10);
        else g.fillRect(evoX + evoW - 3 - fill, 103, fill, 10);
        g.setFont(RetroStyle.font(22));
        m = g.getFontMetrics();
        int labelX = left ? evoX + evoW + 10 : evoX - 10 - m.stringWidth(label);
        g.setColor(INK);
        g.drawString(label, labelX + 1, 116);
        g.setColor(ready ? new Color(0xFFE27A) : new Color(0xEDE6D2));
        g.drawString(label, labelX, 115);

        // Equipo: tres iconos chicos debajo, el activo marcado y los caidos en gris
        int iconSize = 42;
        for (int i = 0; i < team.getMembers().size(); i++) {
            Fighter member = team.getMembers().get(i);
            int x = left ? portraitX + i * (iconSize + 8) : portraitX + 104 - (i + 1) * iconSize - i * 8;
            int y = 136;
            boolean active = i == team.getActiveIndex() && !member.isDefeated();
            g.setColor(INK);
            g.fillRect(x, y, iconSize, iconSize);
            g.setColor(active ? TEAM_COLOR[side] : new Color(0x2C3044));
            g.fillRect(x + 3, y + 3, iconSize - 6, iconSize - 6);
            BufferedImage mini = icon(member.getPokemonId(), !left, frameIndex, repaint);
            if (mini != null) {
                int h = iconSize - 8;
                int w = Math.min(iconSize - 6, Math.round(h * mini.getWidth() / (float) mini.getHeight()));
                Composite old = g.getComposite();
                if (member.isDefeated()) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g.drawImage(mini, x + iconSize / 2 - w / 2, y + 4, w, h, null);
                g.setComposite(old);
            }
            if (member.isDefeated()) {
                g.setColor(new Color(0xF05046));
                g.setStroke(new java.awt.BasicStroke(4));
                g.drawLine(x + 8, y + 8, x + iconSize - 8, y + iconSize - 8);
                g.drawLine(x + iconSize - 8, y + 8, x + 8, y + iconSize - 8);
                g.setStroke(new java.awt.BasicStroke(1));
            }
        }
    }

    /**
     * Picture of the Pokemon for the bar: the first Idle frame of its animated
     * sprite (recortado al cuerpo); only if it has none, the old GIF.
     */
    private BufferedImage icon(String pokemonId, boolean facingLeft, int frameIndex, Runnable repaint) {
        String key = pokemonId + (facingLeft ? "L" : "R");
        if (icons.containsKey(key)) return icons.get(key);
        FightingSpriteProfile profile = FightingSpriteProfile.forSpecies(pokemonId);
        BufferedImage result = null;
        if (profile != null) {
            try {
                result = trim(profile.sprites().get("Idle").frame(facingLeft, 0).image());
            } catch (RuntimeException e) {
                result = null;
            }
        }
        if (result != null) {
            icons.put(key, result);
            return result;
        }
        GifAnimation gif = sprites.getOrLoad(pokemonId, false, repaint);
        if (gif == null) return null;
        BufferedImage frame = gif.frame(frameIndex);
        if (!facingLeft) return frame;
        BufferedImage flipped = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D fg = flipped.createGraphics();
        fg.drawImage(frame, frame.getWidth(), 0, -frame.getWidth(), frame.getHeight(), null);
        fg.dispose();
        return flipped;
    }

    private static BufferedImage trim(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX) return img;
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static String energyLabel(Fighter fighter, String evolutionKey) {
        if (fighter.isEvolved()) {
            return fighter.isUltimateReady() ? "ULTI LISTA (" + (evolutionKey.equals("R") ? "T" : "O") + ")" : "ULTI";
        }
        return fighter.isEvolutionReady() ? "EVOLUCIONA (" + evolutionKey + ")" : "EVO";
    }

    private static Color brighten(Color c, int amount) {
        return new Color(Math.min(255, c.getRed() + amount), Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount));
    }

    private static void centered(Graphics2D g, String text, int cx, int baseline, int size, Color color) {
        g.setFont(RetroStyle.font(size));
        FontMetrics m = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, cx - m.stringWidth(text) / 2, baseline);
    }
}
