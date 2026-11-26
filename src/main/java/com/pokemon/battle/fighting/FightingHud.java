package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.RetroStyle;
import com.pokemon.battle.ui.SpriteLoader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Top bar of the free fight: portrait inside a Poke Ball, life in arrows that
 * change color (green, yellow, red), a trail that shows the damage just received,
 * the evolution / ultimate bar in blue squares and the team in small circles.
 * The Poke Ball in the middle is the pause button.
 */
final class FightingHud {

    // Paleta pixel art: contorno oscuro, borde claro y nada de fondo negro detras
    private static final Color INK = new Color(0x1A1C2C);
    private static final Color[] TEAM_COLOR = {new Color(0xFFD447), new Color(0x7FB8FF)};
    // Estilo de la barra: fondo azul oscuro con borde celeste
    private static final Color NAVY = new Color(0x262B45);
    private static final Color DARK = new Color(0x161A2E);
    private static final Color FRAME = new Color(0xBFE3F5);
    private static final Color EVO_BLUE = new Color(0x4AA8FF);
    private static final int ARROWS = 14;
    private static final int EVO_SQUARES = 16;

    /** Boton de pausa del centro (coordenadas del escenario 1280x720). */
    static final int PAUSE_SIZE = 92;
    static final int PAUSE_X = 640 - PAUSE_SIZE / 2;
    static final int PAUSE_Y = 58 - PAUSE_SIZE / 2;

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
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int side = 0; side < 2; side++) {
            Fighter f = session.getFighters().get(side);
            FightingTeam team = side == 0 ? session.getPlayerOneTeam() : session.getPlayerTwoTeam();
            drawSide(g, session, f, team, side, frameIndex, repaint, dt);
        }
        drawPauseButton(g, session.getState() == FightingGameSession.State.PAUSED);
        if (aa != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    /**
     * Boton de pausa: Poke Ball pixel art dentro de un anillo oscuro con borde celeste.
     * Al pasar el mouse se ilumina; en pausa muestra un triangulo de "seguir".
     */
    private void drawPauseButton(Graphics2D g, boolean paused) {
        int cx = PAUSE_X + PAUSE_SIZE / 2;
        int cy = PAUSE_Y + PAUSE_SIZE / 2;
        int r = PAUSE_SIZE / 2 - 4;
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(cx - r + 3, cy - r + 4, 2 * r, 2 * r);
        g.setColor(new Color(0x0D1424));
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setColor(pauseHover ? Color.WHITE : FRAME);
        g.setStroke(new BasicStroke(5));
        g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setColor(new Color(0x6FDCFF));
        g.setStroke(new BasicStroke(2));
        g.drawOval(cx - r + 5, cy - r + 5, 2 * r - 10, 2 * r - 10);
        // Poke Ball de 24x24 pixeles, agrandada x3
        int n = 24;
        int scale = 3;
        double c = 11.5;
        Color red = pauseHover ? new Color(0xFF5A5A) : new Color(0xE8323C);
        Color redLight = brighten(red, 60);
        Color redDark = new Color(0x9A1820);
        Color white = pauseHover ? Color.WHITE : new Color(0xF0F0F0);
        Color whiteDark = new Color(0xB8BCC6);
        int ox = cx - n * scale / 2;
        int oy = cy - n * scale / 2;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                double d = Math.hypot(x - c, y - c);
                if (d > 11) continue;
                Color col;
                if (d > 9.8 || Math.abs(y - c) < 1.3 || (d < 4.2 && d >= 3.0)) col = INK;
                else if (d < 3.0) col = Color.WHITE;
                else if (y < c) col = x + y < 14 ? redLight : (x - y > 8 ? redDark : red);
                else col = x - y > -4 ? whiteDark : white;
                g.setColor(col);
                g.fillRect(ox + x * scale, oy + y * scale, scale, scale);
            }
        }
        g.setColor(Color.WHITE);
        g.fillRect(ox + 6 * scale, oy + 5 * scale, 2 * scale, scale);
        g.fillRect(ox + 6 * scale, oy + 6 * scale, scale, scale);
        if (paused) {
            // Triangulo de "seguir" en el boton del centro
            g.setColor(INK);
            g.fillPolygon(new int[]{cx - 3, cx + 4, cx - 3}, new int[]{cy - 5, cy, cy + 5}, 3);
        }
    }

    private void drawSide(Graphics2D g, FightingGameSession session, Fighter f, FightingTeam team,
                          int side, int frameIndex, Runnable repaint, double dt) {
        boolean left = side == 0;
        int width = session.getWidth();
        Color accent = TEAM_COLOR[side];

        // Vida que baja con retraso (la estela del dano)
        double ratio = Math.max(0, f.getHealth() / (double) f.getMaxHealth());
        double shown = trail.getOrDefault(f, ratio);
        if (shown < ratio) shown = ratio;
        else shown = Math.max(ratio, shown - dt * 0.35);
        trail.put(f, shown);
        Color life = ratio > 0.5 ? new Color(0x3FD46A) : ratio > 0.2 ? new Color(0xF6C83B) : new Color(0xFF4D5A);
        // Mega Mewtwo Y: barra rosada para que se note que es la fase final
        if (f.isMega()) life = ratio > 0.25 ? new Color(0xFF5FB8) : new Color(0xFF2E7A);
        boolean blink = ratio <= 0.2 && (frameIndex / 3) % 2 == 0;
        if (blink) life = brighten(life, 60);

        double energy = Math.min(1, f.getEvolutionEnergy() / 100.0);
        boolean ready = f.isEvolutionReady() || f.isUltimateReady();
        Color evo = ready && (frameIndex / 4) % 2 == 0 ? new Color(0x9FE6FF) : EVO_BLUE;

        // Las barras se dibujan como el lado izquierdo; el derecho es su reflejo
        Graphics2D b = (Graphics2D) g.create();
        if (!left) b.transform(new AffineTransform(-1, 0, 0, 1, width, 0));
        Polygon body = new Polygon(new int[]{90, 560, 580, 560, 90}, new int[]{36, 36, 57, 78, 78}, 5);
        plate(b, body, 5);
        int filled = (int) Math.ceil(ratio * ARROWS - 1e-9);
        int trailed = (int) Math.ceil(shown * ARROWS - 1e-9);
        for (int i = 0; i < ARROWS; i++) {
            int x = 124 + i * 30;
            Polygon arrow = new Polygon(new int[]{x, x + 24, x + 34, x + 24, x, x + 10},
                    new int[]{44, 44, 57, 70, 70, 57}, 6);
            if (i < filled) gloss(b, arrow, life);
            else if (i < trailed) gloss(b, arrow, new Color(0xFF6B5E));
            else {
                b.setColor(DARK);
                b.fill(arrow);
            }
        }
        Polygon evoBody = new Polygon(new int[]{120, 420, 432, 420, 120}, new int[]{88, 88, 99, 110, 110}, 5);
        plate(b, evoBody, 4);
        int evoFilled = (int) Math.floor(energy * EVO_SQUARES + 1e-9);
        for (int i = 0; i < EVO_SQUARES; i++) {
            Rectangle2D sq = new Rectangle2D.Double(128 + i * 18, 93, 13, 12);
            if (i < evoFilled) gloss(b, sq, evo);
            else {
                b.setColor(DARK);
                b.fill(sq);
            }
        }
        b.dispose();

        // Retrato dentro de una Poke Ball
        int cx = left ? 70 : width - 70;
        int cy = 62;
        drawPortraitBall(g, cx, cy, f, !left, accent, frameIndex, repaint);

        // Nombre del Pokemon y del entrenador
        String name = f.isMega() ? "MEGA MEWTWO Y" : f.getName().toUpperCase();
        String trainer = session.getTrainerName(side);
        trainer = trainer == null ? "" : trainer.toUpperCase();
        g.setFont(RetroStyle.font(28));
        int nameW = g.getFontMetrics().stringWidth(name);
        int nameX = left ? 140 : width - 140 - nameW;
        outlined(g, name, nameX, 28, 28, Color.WHITE);
        if (!trainer.isEmpty() && !f.isBoss()) {
            g.setFont(RetroStyle.font(20));
            int tw = g.getFontMetrics().stringWidth(trainer);
            outlined(g, trainer, left ? nameX + nameW + 14 : nameX - 14 - tw, 26, 20, accent);
        }

        // Etiqueta de la barra EVO (con la tecla cuando ya se puede usar)
        String label = energyLabel(f, side);
        g.setFont(RetroStyle.font(20));
        int lw = g.getFontMetrics().stringWidth(label);
        outlined(g, label, left ? 445 : width - 445 - lw, 108, 20, ready ? new Color(0xFFE27A) : new Color(0x9FD8FF));

        // Equipo en circulos: el activo con brillo, los caidos en gris con una X
        for (int i = 0; i < team.getMembers().size(); i++) {
            Fighter member = team.getMembers().get(i);
            int mx = left ? 40 + i * 70 : width - 40 - i * 70;
            boolean active = i == team.getActiveIndex() && !member.isDefeated();
            drawTeamCircle(g, mx, 162, 27, member, !left, active, accent, frameIndex, repaint);
        }
    }

    private void drawPortraitBall(Graphics2D g, int cx, int cy, Fighter f, boolean facingLeft, Color accent,
                                  int frameIndex, Runnable repaint) {
        int r = 54;
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(cx - r + 3, cy - r + 5, 2 * r, 2 * r);
        g.setColor(FRAME);
        g.fillOval(cx - r - 5, cy - r - 5, 2 * r + 10, 2 * r + 10);
        g.setColor(new Color(0xE8434A));
        g.fill(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 0, 180, Arc2D.CHORD));
        g.setColor(new Color(0xF4F4F4));
        g.fill(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 180, 180, Arc2D.CHORD));
        g.setColor(DARK);
        g.fillRect(cx - r, cy - 4, 2 * r, 8);
        g.setColor(NAVY);
        g.fillOval(cx - 38, cy - 38, 76, 76);
        g.setColor(accent);
        g.setStroke(new BasicStroke(4));
        g.drawOval(cx - 38, cy - 38, 76, 76);
        BufferedImage img = icon(f.getPokemonId(), facingLeft, frameIndex, repaint);
        if (f.usesAltColors()) img = AltPalette.recolor(f.getPokemonId(), img);
        drawFitted(g, img, cx, cy + 2, 56, 62, new Ellipse2D.Double(cx - 36, cy - 36, 72, 72));
    }

    private void drawTeamCircle(Graphics2D g, int cx, int cy, int r, Fighter member, boolean facingLeft,
                                boolean active, Color accent, int frameIndex, Runnable repaint) {
        boolean down = member.isDefeated();
        Ellipse2D circle = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        if (active) {
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
            g.setStroke(new BasicStroke(12));
            g.draw(circle);
        }
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(cx - r + 2, cy - r + 3, 2 * r, 2 * r);
        g.setColor(FRAME);
        g.setStroke(new BasicStroke(7));
        g.draw(circle);
        g.setColor(down ? new Color(0x3A3D48) : NAVY);
        g.fill(circle);
        g.setColor(active ? accent : down ? new Color(0x6B7080) : new Color(0x8A93A8));
        g.setStroke(new BasicStroke(3));
        g.draw(circle);
        BufferedImage mini = icon(member.getPokemonId(), facingLeft, frameIndex, repaint);
        if (member.usesAltColors()) mini = AltPalette.recolor(member.getPokemonId(), mini);
        if (down && mini != null) mini = grayOf(mini);
        drawFitted(g, mini, cx, cy, (int) (r * 1.35), (int) (r * 1.6), circle);
        if (down) {
            g.setColor(new Color(0xE5484D));
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int k = (int) (r * 0.55);
            g.drawLine(cx - k, cy - k, cx + k, cy + k);
            g.drawLine(cx + k, cy - k, cx - k, cy + k);
        }
    }

    /** Dibuja el sprite a cierta altura (sin pasarse de maxW), recortado a la forma. */
    private static void drawFitted(Graphics2D g, BufferedImage img, int cx, int cy, int h, int maxW, Shape clip) {
        if (img == null) return;
        int w = Math.round(h * img.getWidth() / (float) img.getHeight());
        if (w > maxW) {
            h = Math.round(h * maxW / (float) w);
            w = maxW;
        }
        Shape old = g.getClip();
        g.clip(clip);
        g.drawImage(img, cx - w / 2, cy - h / 2, w, h, null);
        g.setClip(old);
    }

    private final Map<BufferedImage, BufferedImage> grays = new IdentityHashMap<>();

    private BufferedImage grayOf(BufferedImage src) {
        return grays.computeIfAbsent(src, s -> {
            BufferedImage out = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < s.getHeight(); y++) {
                for (int x = 0; x < s.getWidth(); x++) {
                    int p = s.getRGB(x, y);
                    int l = (int) (0.3 * ((p >> 16) & 255) + 0.59 * ((p >> 8) & 255) + 0.11 * (p & 255)) / 2 + 40;
                    out.setRGB(x, y, (p & 0xFF000000) | (l << 16) | (l << 8) | l);
                }
            }
            return out;
        });
    }

    /** Placa azul oscura con borde celeste grueso y sombra. */
    private static void plate(Graphics2D g, Shape shape, int rim) {
        g.setColor(new Color(0, 0, 0, 90));
        g.translate(3, 5);
        g.fill(shape);
        g.translate(-3, -5);
        g.setColor(FRAME);
        g.setStroke(new BasicStroke(rim * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
        g.setColor(NAVY);
        g.fill(shape);
    }

    /** Relleno con degradado y un brillo arriba. */
    private static void gloss(Graphics2D g, Shape shape, Color c) {
        Rectangle2D bounds = shape.getBounds2D();
        g.setPaint(new GradientPaint(0, (float) bounds.getY(), c.brighter(),
                0, (float) (bounds.getY() + bounds.getHeight()), c.darker()));
        g.fill(shape);
        Shape old = g.getClip();
        g.clip(shape);
        g.setColor(new Color(255, 255, 255, 85));
        g.fill(new Rectangle2D.Double(bounds.getX(), bounds.getY() + 2, bounds.getWidth(),
                Math.max(2, bounds.getHeight() / 3)));
        g.setClip(old);
    }

    private static void outlined(Graphics2D g, String text, int x, int y, int size, Color color) {
        g.setFont(RetroStyle.font(size));
        g.setColor(new Color(0, 0, 0, 220));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) g.drawString(text, x + dx, y + dy);
            }
        }
        g.setColor(color);
        g.drawString(text, x, y);
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
        if (fighter.isBoss()) {
            // Mewtwo no evoluciona: su barra carga el Psicoataque
            return fighter.isUltimateReady() ? "PSICOATAQUE LISTO" : "PSICOATAQUE";
        }
        if (fighter.isEvolved()) {
            return fighter.isUltimateReady() ? "ULTI LISTA (" + ultimateKey + ")" : "ULTI";
        }
        return fighter.isEvolutionReady() ? "EVOLUCIONA (" + evolveKey + ")" : "EVO";
    }

    private static Color brighten(Color c, int amount) {
        return new Color(Math.min(255, c.getRed() + amount), Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount), c.getAlpha());
    }

}
