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

    // Paleta pixel art: contorno oscuro, borde claro y nada de fondo negro detras
    private static final Color INK = new Color(0x1A1C2C);
    private static final Color RIM = new Color(0xF4F0E0);
    private static final Color RIM_SHADE = new Color(0xB8B2A0);
    private static final Color EMPTY = new Color(40, 44, 66, 120);
    private static final Color[] TEAM_COLOR = {new Color(0xE8784A), new Color(0x4A8FE8)};
    private static final int SEGMENTS = 20;
    /** Tamano de un "pixel" del HUD en pantalla: todo se dibuja en una cuadricula de 4x4. */
    private static final int P = 4;

    /** Boton de pausa del centro (coordenadas del escenario 1280x720). */
    static final int PAUSE_X = 640 - 17 * P / 2;
    static final int PAUSE_Y = 6 * P;
    static final int PAUSE_SIZE = 17 * P;

    private final SpriteLoader sprites;
    // Vida que se muestra con retraso (la estela roja del dano)
    private final Map<Fighter, Double> trail = new IdentityHashMap<>();
    private final Map<String, BufferedImage> icons = new java.util.HashMap<>();
    private boolean pauseHover;

    FightingHud(SpriteLoader sprites) {
        this.sprites = sprites;
    }

    void clear() {
        trail.clear();
    }

    void setPauseHover(boolean hover) {
        pauseHover = hover;
    }

    static boolean isOnPauseButton(int x, int y) {
        double r = PAUSE_SIZE / 2.0;
        double dx = x - (PAUSE_X + r);
        double dy = y - (PAUSE_Y + r);
        return dx * dx + dy * dy <= r * r;
    }

    void draw(Graphics2D g, FightingGameSession session, int frameIndex, Runnable repaint, double dt) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (int side = 0; side < 2; side++) {
            Fighter f = session.getFighters().get(side);
            FightingTeam team = side == 0 ? session.getPlayerOneTeam() : session.getPlayerTwoTeam();
            drawSide(g, session, f, team, side, frameIndex, repaint, dt);
        }
        drawPauseButton(g, session.getState() == FightingGameSession.State.PAUSED);
        if (aa != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    /** Boton estilo Poke Ball con el simbolo de pausa en el centro. */
    private void drawPauseButton(Graphics2D g, boolean paused) {
        int n = 17;
        double c = (n - 1) / 2.0;
        Color red = pauseHover ? new Color(0xFF6A5C) : new Color(0xE8463A);
        Color redDark = new Color(0xA82A26);
        Color white = pauseHover ? Color.WHITE : new Color(0xF0EEE6);
        Color whiteDark = new Color(0xB9B6AE);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (!inside(i, j, c, 8.4)) continue;
                boolean edge = !inside(i - 1, j, c, 8.4) || !inside(i + 1, j, c, 8.4)
                        || !inside(i, j - 1, c, 8.4) || !inside(i, j + 1, c, 8.4);
                Color col;
                if (edge || j == 8) col = INK;
                else if (j < 8) col = (i + j < 7) ? brighten(red, 50) : (j > 5 && i > 10) ? redDark : red;
                else col = (j > 13 || (i > 12 && j > 10)) ? whiteDark : white;
                g.setColor(col);
                g.fillRect(PAUSE_X + i * P, PAUSE_Y + j * P, P, P);
            }
        }
        // Disco del centro con "II" (o un triangulo si ya esta en pausa)
        for (int j = 4; j <= 12; j++) {
            for (int i = 4; i <= 12; i++) {
                if (!inside(i, j, c, 4.3)) continue;
                boolean edge = !inside(i - 1, j, c, 4.3) || !inside(i + 1, j, c, 4.3)
                        || !inside(i, j - 1, c, 4.3) || !inside(i, j + 1, c, 4.3);
                g.setColor(edge ? INK : Color.WHITE);
                g.fillRect(PAUSE_X + i * P, PAUSE_Y + j * P, P, P);
            }
        }
        g.setColor(INK);
        if (paused) {
            for (int k = 0; k < 3; k++) g.fillRect(PAUSE_X + (7 + k) * P, PAUSE_Y + (6 + k) * P, P, (5 - 2 * k) * P);
        } else {
            g.fillRect(PAUSE_X + 6 * P + P / 2, PAUSE_Y + 6 * P, P, 5 * P);
            g.fillRect(PAUSE_X + 9 * P + P / 2, PAUSE_Y + 6 * P, P, 5 * P);
        }
    }

    private static boolean inside(int i, int j, double c, double r) {
        double dx = i - c;
        double dy = j - c;
        return dx * dx + dy * dy <= r * r;
    }

    private void drawSide(Graphics2D g, FightingGameSession session, Fighter f, FightingTeam team,
                          int side, int frameIndex, Runnable repaint, double dt) {
        boolean left = side == 0;
        int width = session.getWidth();
        int portraitX = left ? 8 * P : width - 8 * P - 26 * P;
        int portraitY = 5 * P;
        int barW = 104 * P;
        int barX = left ? portraitX + 30 * P : portraitX - 4 * P - barW;

        // Retrato con marco pixelado
        frame(g, portraitX, portraitY, 26 * P, 26 * P, TEAM_COLOR[side]);
        BufferedImage img = icon(f.getPokemonId(), !left, frameIndex, repaint);
        if (img != null) {
            int h = 21 * P;
            int w = Math.min(22 * P, Math.round(h * img.getWidth() / (float) img.getHeight()));
            g.drawImage(img, portraitX + 13 * P - w / 2, portraitY + 3 * P, w, h, null);
        }

        // Nombre
        String name = f.getName().toUpperCase();
        g.setFont(RetroStyle.font(36));
        FontMetrics m = g.getFontMetrics();
        int nameX = left ? barX + P : barX + barW - P - m.stringWidth(name);
        g.setColor(INK);
        g.drawString(name, nameX + P / 2, 13 * P + P / 2);
        g.drawString(name, nameX, 13 * P + P / 2);
        g.setColor(Color.WHITE);
        g.drawString(name, nameX, 13 * P);

        // Vida en bloques con la estela del dano
        double ratio = Math.max(0, f.getHealth() / (double) f.getMaxHealth());
        double shown = trail.getOrDefault(f, ratio);
        if (shown < ratio) shown = ratio;
        else shown = Math.max(ratio, shown - dt * 0.35);
        trail.put(f, shown);
        int barY = 15 * P;
        int barH = 8 * P;
        frame(g, barX, barY, barW, barH, null);
        Color full = ratio > 0.5 ? new Color(0x58D660) : ratio > 0.25 ? new Color(0xFAC83C) : new Color(0xF05046);
        boolean blink = ratio <= 0.25 && (frameIndex / 3) % 2 == 0;
        int innerX = barX + 2 * P;
        int innerY = barY + 2 * P;
        int innerH = barH - 4 * P;
        int segW = (barW - 4 * P) / SEGMENTS;   // 5 pixeles: 4 de color y 1 de separacion
        for (int i = 0; i < SEGMENTS; i++) {
            int k = left ? i : SEGMENTS - 1 - i;
            int x0 = innerX + k * segW;
            int w = segW - P;
            if (k < SEGMENTS - 1) {
                g.setColor(INK);
                g.fillRect(x0 + w, innerY, P, innerH);
            }
            double from = i / (double) SEGMENTS;
            Color body;
            if (from < ratio) body = blink ? full.brighter() : full;
            else if (from < shown) body = new Color(0xFF6B5E);
            else body = null;
            if (body == null) {
                g.setColor(EMPTY);
                g.fillRect(x0, innerY, w, innerH);
                continue;
            }
            g.setColor(body);
            g.fillRect(x0, innerY, w, innerH);
            g.setColor(brighten(body, 70));
            g.fillRect(x0, innerY, w, P);
            g.setColor(darken(body, 50));
            g.fillRect(x0, innerY + innerH - P, w, P);
        }

        // Barra de evolucion / ulti
        String label = energyLabel(f, left ? 0 : 1);
        int evoW = 66 * P;
        int evoH = 5 * P;
        int evoX = left ? barX : barX + barW - evoW;
        int evoY = 24 * P + P;
        frame(g, evoX, evoY, evoW, evoH, null);
        double energy = Math.min(1, f.getEvolutionEnergy() / 100.0);
        boolean ready = f.isEvolutionReady();
        Color evo = ready && (frameIndex / 4) % 2 == 0 ? new Color(0xFFF0A0) : new Color(0xFFD25A);
        int innerW = evoW - 4 * P;
        int fill = (int) Math.round(innerW * energy / P) * P;
        g.setColor(EMPTY);
        g.fillRect(evoX + 2 * P, evoY + 2 * P, innerW, P);
        g.setColor(evo);
        int fx = left ? evoX + 2 * P : evoX + 2 * P + innerW - fill;
        g.fillRect(fx, evoY + 2 * P, fill, P);
        g.setFont(RetroStyle.font(22));
        m = g.getFontMetrics();
        int labelX = left ? evoX + evoW + 3 * P : evoX - 3 * P - m.stringWidth(label);
        g.setColor(INK);
        g.drawString(label, labelX + P / 2, evoY + evoH - P / 2);
        g.setColor(ready ? new Color(0xFFE27A) : new Color(0xEDE6D2));
        g.drawString(label, labelX, evoY + evoH - P);

        // Equipo: tres iconos chicos debajo, el activo marcado y los caidos en gris
        int iconSize = 11 * P;
        for (int i = 0; i < team.getMembers().size(); i++) {
            Fighter member = team.getMembers().get(i);
            int x = left ? portraitX + i * (iconSize + 2 * P) - P : portraitX + 26 * P - (i + 1) * iconSize - i * 2 * P + P;
            int y = 33 * P;
            boolean active = i == team.getActiveIndex() && !member.isDefeated();
            frame(g, x, y, iconSize, iconSize, active ? TEAM_COLOR[side] : new Color(90, 96, 120, 150));
            BufferedImage mini = icon(member.getPokemonId(), !left, frameIndex, repaint);
            if (mini != null) {
                int h = iconSize - 4 * P;
                int w = Math.min(iconSize - 4 * P, Math.round(h * mini.getWidth() / (float) mini.getHeight()));
                Composite old = g.getComposite();
                if (member.isDefeated()) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g.drawImage(mini, x + iconSize / 2 - w / 2, y + 2 * P, w, h, null);
                g.setComposite(old);
            }
            if (member.isDefeated()) {
                // X pixelada
                for (int k = 0; k < 7; k++) {
                    g.setColor(INK);
                    g.fillRect(x + (2 + k) * P, y + (2 + k) * P + P / 2, P, P);
                    g.fillRect(x + (8 - k) * P, y + (2 + k) * P + P / 2, P, P);
                    g.setColor(new Color(0xF05046));
                    g.fillRect(x + (2 + k) * P, y + (2 + k) * P, P, P);
                    g.fillRect(x + (8 - k) * P, y + (2 + k) * P, P, P);
                }
            }
        }
    }

    /**
     * Marco pixel art: contorno oscuro con esquinas recortadas y un borde claro con sombra.
     * El interior se rellena con {@code fill}, o se deja transparente si es null.
     */
    private static void frame(Graphics2D g, int x, int y, int w, int h, Color fill) {
        // contorno (sin las esquinas)
        g.setColor(INK);
        g.fillRect(x + P, y, w - 2 * P, P);
        g.fillRect(x + P, y + h - P, w - 2 * P, P);
        g.fillRect(x, y + P, P, h - 2 * P);
        g.fillRect(x + w - P, y + P, P, h - 2 * P);
        // borde claro arriba/izquierda, sombra abajo/derecha
        g.setColor(RIM);
        g.fillRect(x + P, y + P, w - 2 * P, P);
        g.fillRect(x + P, y + P, P, h - 2 * P);
        g.setColor(RIM_SHADE);
        g.fillRect(x + P, y + h - 2 * P, w - 2 * P, P);
        g.fillRect(x + w - 2 * P, y + 2 * P, P, h - 3 * P);
        if (fill != null) {
            g.setColor(fill);
            g.fillRect(x + 2 * P, y + 2 * P, w - 4 * P, h - 4 * P);
            g.setColor(brighten(fill, 45));
            g.fillRect(x + 2 * P, y + 2 * P, w - 4 * P, P);
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

    // Con mando conectado, los avisos muestran sus botones (LT / RT) en vez de las teclas
    private final boolean[] padConnected = new boolean[2];

    /** Lo avisa la pantalla de combate cada vez que lee los mandos. */
    void setPadConnected(int player, boolean connected) {
        if (player == 0 || player == 1) padConnected[player] = connected;
    }

    private String energyLabel(Fighter fighter, int player) {
        boolean pad = padConnected[player];
        String evolveKey = pad ? "LT" : player == 0 ? "R" : "P";
        String ultimateKey = pad ? "RT" : player == 0 ? "T" : "O";
        if (fighter.isEvolved()) {
            return fighter.isUltimateReady() ? "ULTI LISTA (" + ultimateKey + ")" : "ULTI";
        }
        return fighter.isEvolutionReady() ? "EVOLUCIONA (" + evolveKey + ")" : "EVO";
    }

    private static Color brighten(Color c, int amount) {
        return new Color(Math.min(255, c.getRed() + amount), Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount), c.getAlpha());
    }

    private static Color darken(Color c, int amount) {
        return new Color(Math.max(0, c.getRed() - amount), Math.max(0, c.getGreen() - amount),
                Math.max(0, c.getBlue() - amount), c.getAlpha());
    }

    private static void centered(Graphics2D g, String text, int cx, int baseline, int size, Color color) {
        g.setFont(RetroStyle.font(size));
        FontMetrics m = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, cx - m.stringWidth(text) / 2, baseline);
    }
}
