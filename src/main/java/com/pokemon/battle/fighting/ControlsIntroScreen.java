package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.RetroStyle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla "¿COMO SE JUEGA?" antes de cada Combate Libre (pensada para la ExpoSoftware,
 * donde cada persona juega una sola partida).
 *
 * Muestra el mando con sus controles. Cada jugador presiona A (o su tecla de salto) cuando
 * quiera y su cuadro dice LISTO; cuando los dos estan listos se quita y empieza la pelea.
 * Si nadie presiona nada, empieza sola a los 15 segundos.
 */
final class ControlsIntroScreen {

    static final double TIMEOUT_SECONDS = 15.0;
    /** Volumen de la musica mientras se muestran los controles (bajita). */
    private static final float MUSIC_GAIN = 0.35f;
    /** Pausa corta despues de que los dos estan listos, para que se alcance a ver el LISTO. */
    private static final double READY_DELAY_SECONDS = 0.35;

    private static final int W = 1280;
    private static final int H = 720;
    private static final Color MAIN = new Color(0xFFC83D);
    private static final Color LIME = new Color(0x6CFF5A);
    private static final Color CYAN = new Color(0x2FD3FF);
    private static final Color ORANGE = new Color(0xFF9A2E);
    private static final Color J1 = new Color(0xFFD447);
    private static final Color J2 = new Color(0x9AD8FF);

    private boolean active;
    private final boolean[] ready = new boolean[2];
    private double elapsed;
    private double bothReadyTime = -1;
    private String nameOne = "JUGADOR 1";
    private String nameTwo = "JUGADOR 2";

    /** Mando y etiquetas: no cambian, se dibujan una vez y se reutilizan. */
    private static BufferedImage staticLayer;

    void start(String playerOne, String playerTwo) {
        active = true;
        ready[0] = false;
        ready[1] = false;
        elapsed = 0;
        bothReadyTime = -1;
        nameOne = playerOne == null || playerOne.isBlank() ? "JUGADOR 1" : playerOne.toUpperCase();
        nameTwo = playerTwo == null || playerTwo.isBlank() ? "JUGADOR 2" : playerTwo.toUpperCase();
        musicStarted = false;
    }

    /** La musica arranca con el primer cuadro en pantalla (no al crear la pelea). */
    private boolean musicStarted;

    boolean isActive() { return active; }

    /** La quita sin esperar (por ejemplo, al entrar directo contra Mewtwo). */
    void stop() {
        if (active) finish();
    }

    /** Se quita: la musica para y vuelve a sonar normal desde el "¡PELEA!". */
    private void finish() {
        active = false;
        FightingSound sound = FightingSound.get();
        sound.stopMusicAndRewind();
        sound.setMusicGain(1f);
    }

    boolean isReady(int player) { return ready[player]; }

    void markReady(int player) {
        if (!active || player < 0 || player > 1 || ready[player]) return;
        ready[player] = true;
        // Sonido de boton al quedar listo
        FightingSound.get().play(FightingSound.Sfx.COUNT, 0.8f);
    }

    void markBothReady() {
        markReady(0);
        markReady(1);
    }

    /** Avanza el tiempo; se quita sola cuando los dos estan listos o se acaba el tiempo. */
    void update(double dt) {
        if (!active) return;
        if (!musicStarted) {
            // Musica de batalla bajita mientras se leen los controles
            musicStarted = true;
            FightingSound sound = FightingSound.get();
            sound.setMusicGain(MUSIC_GAIN);
            sound.startBattleMusic();
        }
        elapsed += dt;
        if (ready[0] && ready[1] && bothReadyTime < 0) bothReadyTime = elapsed;
        boolean readyDone = bothReadyTime >= 0 && elapsed - bothReadyTime >= READY_DELAY_SECONDS;
        if (readyDone || elapsed >= TIMEOUT_SECONDS) finish();
    }

    int secondsLeft() {
        return (int) Math.ceil(Math.max(0, TIMEOUT_SECONDS - elapsed));
    }

    // ==========================================
    // DIBUJO
    // ==========================================

    /** Dibuja la pantalla encima del escenario (coordenadas del combate, 1280x720). */
    void draw(Graphics2D g0) {
        if (!active) return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawImage(staticLayer(), 0, 0, null);
        drawReadyBox(g, 70, 572, nameOne, J1, ready[0], true);
        drawReadyBox(g, 860, 572, nameTwo, J2, ready[1], false);
        drawTimer(g);
        g.dispose();
    }

    private static synchronized BufferedImage staticLayer() {
        if (staticLayer != null) return staticLayer;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Fondo oscuro con lineas de escaneo y un brillo suave al centro
        g.setColor(new Color(4, 4, 10, 236));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 255, 255, 8));
        for (int y = 0; y < H; y += 4) g.fillRect(0, y, W, 1);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(640, 300), 620, new float[]{0f, 1f},
                new Color[]{alpha(MAIN, 26), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, W, H);
        // Titulo con flechas
        centered(g, "¿COMO SE JUEGA?", 640, 62, 50, Color.WHITE);
        g.setColor(MAIN);
        g.setStroke(new BasicStroke(2));
        g.drawLine(380, 78, 560, 78);
        g.drawLine(720, 78, 900, 78);
        g.fillRect(612, 75, 56, 6);
        g.fill(new Polygon(new int[]{360, 380, 380}, new int[]{78, 70, 86}, 3));
        g.fill(new Polygon(new int[]{920, 900, 900}, new int[]{78, 70, 86}, 3));
        // Mando en lineas de neon
        Pad pad = new Pad(640, 300, 1.05);
        for (Shape s : pad.lines) glowStroke(g, s, MAIN, 2.6f);
        Object[][] face = {{"Y", new Color(0xF2C12E)}, {"X", new Color(0x3F8EF0)},
                {"B", new Color(0xE0412F)}, {"A", new Color(0x5BC236)}};
        for (Object[] o : face) {
            Point p = pad.points.get((String) o[0]);
            Color c = (Color) o[1];
            g.setColor(alpha(c, 70));
            g.fillOval(p.x - 14, p.y - 14, 28, 28);
            centered(g, (String) o[0], p.x, p.y + 7, 20, c);
        }
        // Etiquetas a la izquierda y a la derecha
        String[][] left = {{"LT", "EVOLUCIONAR", ""}, {"LB", "ESCUDO", "+ LADO = ESQUIVA"},
                {"STK", "MOVERSE", ""}, {"SE", "CAMBIAR POKEMON", ""}, {"ST", "PAUSA", ""}};
        String[][] right = {{"RT", "ULTI", ""}, {"RB", "ATAQUE 4", ""}, {"Y", "ESPECIAL", ""},
                {"X", "GOLPE", "x2 = DOBLE GOLPE"}, {"B", "ATAQUE 3", ""}, {"A", "SALTAR", ""}};
        int[] leftY = {140, 200, 290, 360, 420};
        int[] rightY = {130, 180, 250, 300, 360, 410};
        Color line = new Color(255, 255, 255, 200);
        for (int i = 0; i < left.length; i++) {
            int y = leftY[i];
            leader(g, 300, y, pad.points.get(left[i][0]), 380, line);
            text(g, left[i][1], 248, y + 7, 26, MAIN, true);
            glyph(g, left[i][0], 278, y);
            if (!left[i][2].isEmpty()) text(g, left[i][2], 248, y + 30, 19, new Color(255, 255, 255, 150), true);
        }
        for (int i = 0; i < right.length; i++) {
            int y = rightY[i];
            leader(g, 980, y, pad.points.get(right[i][0]), 900, line);
            glyph(g, right[i][0], 1002, y);
            text(g, right[i][1], 1034, y + 7, 26, MAIN, false);
            if (!right[i][2].isEmpty()) text(g, right[i][2], 1034, y + 30, 19, new Color(255, 255, 255, 150), false);
        }
        // Linea de ayuda
        String a = "ESCUDO + LADO = ESQUIVA";
        outlined(g, a, 640 - width(g, a, 24) / 2, 532, 24, Color.WHITE);
        g.dispose();
        staticLayer = img;
        return img;
    }

    private void drawReadyBox(Graphics2D g, int x, int y, String name, Color who, boolean isReady, boolean leftSide) {
        int w = 350;
        int h = 110;
        Color c = isReady ? LIME : (leftSide ? who : CYAN);
        Polygon frame = cut(x, y, w, h, 16);
        g.setPaint(new GradientPaint(0, y, alpha(c.darker().darker(), 190), 0, y + h, new Color(5, 10, 20, 225)));
        g.fill(frame);
        glowStroke(g, frame, c, 2.5f);
        // Esquinas tecnicas
        g.setColor(c);
        g.setStroke(new BasicStroke(4));
        g.drawLine(x - 10, y + 20, x - 10, y + h - 20);
        g.drawLine(x + w + 10, y + 20, x + w + 10, y + h - 20);
        for (int i = 0; i < 3; i++) {
            g.fillRect(x + 30 + i * 14, y - 8, 9, 4);
            g.fillRect(x + w - 70 + i * 14, y + h + 4, 9, 4);
        }
        String label = (leftSide ? "J1  " : "J2  ") + name;
        outlined(g, label, x + w / 2 - width(g, label, 26) / 2, y + 40, 26, who);
        String big = isReady ? "¡LISTO!" : "PRESIONA [A]";
        Color bigColor = isReady ? LIME : new Color(0x7FE3FF);
        int size = isReady ? 46 : 42;
        glowText(g, big, x + w / 2 - width(g, big, size) / 2, y + 90, size, bigColor);
        if (isReady) {
            bolt(g, x - 10, y + 28, 40, Math.PI, 11);
            bolt(g, x + w + 10, y + 78, 40, 0, 12);
        }
    }

    private void drawTimer(Graphics2D g) {
        double frac = Math.max(0, 1 - elapsed / TIMEOUT_SECONDS);
        RoundRectangle2D track = new RoundRectangle2D.Double(470, 626, 340, 18, 10, 10);
        g.setColor(new Color(0, 0, 0, 200));
        g.fill(track);
        g.setColor(new Color(0x8A93A6));
        g.setStroke(new BasicStroke(2));
        g.draw(track);
        double fillW = 334 * frac;
        if (fillW > 1) {
            RoundRectangle2D fill = new RoundRectangle2D.Double(473, 629, fillW, 12, 8, 8);
            g.setPaint(new GradientPaint(473, 0, new Color(0xFF7A1A), 807, 0, new Color(0xFFE066)));
            g.fill(fill);
            glowStroke(g, fill, alpha(ORANGE, 180), 1f);
            // Chispas en la punta de la barra
            java.util.Random rnd = new java.util.Random((long) (elapsed * 20));
            int tipX = (int) (473 + fillW);
            for (int i = 0; i < 18; i++) {
                int ex = tipX - 20 + rnd.nextInt(30);
                int ey = 600 + rnd.nextInt(40);
                g.setColor(i % 3 == 0 ? new Color(255, 240, 150, 220) : new Color(255, 140, 40, 200));
                int sz = 2 + rnd.nextInt(3);
                g.fillRect(ex, ey, sz, sz);
            }
        }
        String t = "EMPIEZA SOLO EN " + secondsLeft();
        outlined(g, t, 640 - width(g, t, 24) / 2, 678, 24, Color.WHITE);
    }

    // ==========================================
    // AYUDAS DE DIBUJO
    // ==========================================

    /** Mando estilo Xbox en lineas, con la posicion de cada boton. */
    private static final class Pad {
        final List<Shape> lines = new ArrayList<>();
        final Map<String, Point> points = new HashMap<>();

        Pad(int cx, int cy, double s) {
            AffineTransform t = new AffineTransform();
            t.translate(cx, cy);
            t.scale(s, s);
            Path2D b = new Path2D.Double();
            b.moveTo(-150, -70);
            b.curveTo(-110, -95, -60, -88, -40, -80);
            b.lineTo(40, -80);
            b.curveTo(60, -88, 110, -95, 150, -70);
            b.curveTo(195, -40, 215, 40, 225, 110);
            b.curveTo(232, 160, 205, 185, 175, 180);
            b.curveTo(140, 175, 120, 130, 95, 105);
            b.curveTo(70, 85, -70, 85, -95, 105);
            b.curveTo(-120, 130, -140, 175, -175, 180);
            b.curveTo(-205, 185, -232, 160, -225, 110);
            b.curveTo(-215, 40, -195, -40, -150, -70);
            b.closePath();
            lines.add(new Area(t.createTransformedShape(b)));
            lines.add(t.createTransformedShape(curve(-160, -78, -160, -120, -120, -128, -100, -112, -95, -88)));
            lines.add(t.createTransformedShape(curve(160, -78, 160, -120, 120, -128, 100, -112, 95, -88)));
            Path2D lb = new Path2D.Double();
            lb.moveTo(-168, -62);
            lb.curveTo(-150, -92, -90, -98, -55, -84);
            lines.add(t.createTransformedShape(lb));
            Path2D rb = new Path2D.Double();
            rb.moveTo(168, -62);
            rb.curveTo(150, -92, 90, -98, 55, -84);
            lines.add(t.createTransformedShape(rb));
            ring(t, -118, -15, 30);
            ring(t, -118, -15, 19);
            ring(t, 62, 48, 28);
            ring(t, 62, 48, 18);
            lines.add(t.createTransformedShape(cross(-62, 48, 11, 30)));
            double bx = 120;
            double by = -15;
            ring(t, bx, by - 30, 14);
            ring(t, bx - 30, by, 14);
            ring(t, bx + 30, by, 14);
            ring(t, bx, by + 30, 14);
            // Centro: SE, captura, inicio, T y ST (como el mando que usamos)
            ring(t, 0, -50, 12);
            ring(t, -48, -34, 9);
            ring(t, -26, -14, 8);
            ring(t, 26, -14, 8);
            ring(t, 48, -34, 9);
            put(t, "LT", -130, -118);
            put(t, "RT", 130, -118);
            put(t, "LB", -120, -88);
            put(t, "RB", 120, -88);
            put(t, "STK", -118, -15);
            put(t, "Y", bx, by - 30);
            put(t, "X", bx - 30, by);
            put(t, "B", bx + 30, by);
            put(t, "A", bx, by + 30);
            put(t, "SE", -48, -34);
            put(t, "ST", 48, -34);
        }

        private static Path2D curve(double x0, double y0, double c1x, double c1y, double c2x, double c2y,
                                    double x1, double y1, double x2, double y2) {
            Path2D p = new Path2D.Double();
            p.moveTo(x0, y0);
            p.curveTo(c1x, c1y, c2x, c2y, x1, y1);
            p.lineTo(x2, y2);
            return p;
        }

        private static Path2D cross(double dx, double dy, double w, double l) {
            Path2D d = new Path2D.Double();
            d.moveTo(dx - w, dy - l);
            d.lineTo(dx + w, dy - l);
            d.lineTo(dx + w, dy - w);
            d.lineTo(dx + l, dy - w);
            d.lineTo(dx + l, dy + w);
            d.lineTo(dx + w, dy + w);
            d.lineTo(dx + w, dy + l);
            d.lineTo(dx - w, dy + l);
            d.lineTo(dx - w, dy + w);
            d.lineTo(dx - l, dy + w);
            d.lineTo(dx - l, dy - w);
            d.lineTo(dx - w, dy - w);
            d.closePath();
            return d;
        }

        private void ring(AffineTransform t, double x, double y, double r) {
            lines.add(t.createTransformedShape(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r)));
        }

        private void put(AffineTransform t, String key, double x, double y) {
            Point2D p = t.transform(new Point2D.Double(x, y), null);
            points.put(key, new Point((int) p.getX(), (int) p.getY()));
        }
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private static void glowStroke(Graphics2D g, Shape s, Color c, float w) {
        for (int i = 5; i >= 1; i--) {
            g.setColor(alpha(c, 18));
            g.setStroke(new BasicStroke(w + i * 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(s);
        }
        g.setColor(c);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
        g.setColor(new Color(255, 255, 255, 110));
        g.setStroke(new BasicStroke(Math.max(1, w / 3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    private static void leader(Graphics2D g, int fromX, int fromY, Point to, int elbowX, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(fromX, fromY);
        p.lineTo(elbowX, fromY);
        p.lineTo(to.x, to.y);
        g.setColor(c);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(p);
        g.fillOval(to.x - 4, to.y - 4, 8, 8);
    }

    /** Icono pequeno del boton: circulo para A/B/X/Y y pastilla para los demas. */
    private static void glyph(Graphics2D g, String key, int cx, int cy) {
        g.setStroke(new BasicStroke(2));
        if (key.length() == 1) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillOval(cx - 12, cy - 12, 24, 24);
            g.setColor(Color.WHITE);
            g.drawOval(cx - 12, cy - 12, 24, 24);
            centered(g, key, cx, cy + 7, 20, Color.WHITE);
        } else {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(cx - 20, cy - 11, 40, 22, 8, 8);
            g.setColor(Color.WHITE);
            g.drawRoundRect(cx - 20, cy - 11, 40, 22, 8, 8);
            centered(g, key, cx, cy + 7, 18, Color.WHITE);
        }
    }

    private static Polygon cut(int x, int y, int w, int h, int c) {
        return new Polygon(new int[]{x + c, x + w, x + w, x + w - c, x, x},
                new int[]{y, y, y + h - c, y + h, y + h, y + c}, 6);
    }

    private static void bolt(Graphics2D g, int x, int y, int len, double angle, long seed) {
        java.util.Random rnd = new java.util.Random(seed);
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        for (int i = 1; i <= 6; i++) {
            double t = len * i / 6.0;
            double off = (rnd.nextDouble() - 0.5) * len * 0.35;
            p.lineTo(x + Math.cos(angle) * t - Math.sin(angle) * off, y + Math.sin(angle) * t + Math.cos(angle) * off);
        }
        for (int i = 4; i >= 1; i--) {
            g.setColor(new Color(255, 230, 120, 22));
            g.setStroke(new BasicStroke(2 + i * 3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);
        }
        g.setColor(new Color(255, 230, 120));
        g.setStroke(new BasicStroke(2f));
        g.draw(p);
    }

    /** Texto con un brillo suave del mismo color. */
    private static void glowText(Graphics2D g, String t, int x, int y, float size, Color c) {
        g.setFont(RetroStyle.font(size));
        for (int r = 6; r >= 2; r -= 2) {
            g.setColor(alpha(c, 22));
            for (int a = 0; a < 8; a++) {
                double ang = a * Math.PI / 4;
                g.drawString(t, (float) (x + Math.cos(ang) * r), (float) (y + Math.sin(ang) * r));
            }
        }
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(t, x + 2, y + 3);
        g.setColor(c);
        g.drawString(t, x, y);
    }

    private static void outlined(Graphics2D g, String t, int x, int y, float size, Color c) {
        g.setFont(RetroStyle.font(size));
        g.setColor(new Color(0, 0, 0, 200));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) g.drawString(t, x + dx, y + dy);
            }
        }
        g.setColor(c);
        g.drawString(t, x, y);
    }

    private static void text(Graphics2D g, String t, int x, int y, float size, Color c, boolean alignRight) {
        Font f = RetroStyle.font(size);
        g.setFont(f);
        int w = g.getFontMetrics().stringWidth(t);
        g.setColor(c);
        g.drawString(t, alignRight ? x - w : x, y);
    }

    private static void centered(Graphics2D g, String t, int cx, int y, float size, Color c) {
        g.setFont(RetroStyle.font(size));
        int w = g.getFontMetrics().stringWidth(t);
        g.setColor(c);
        g.drawString(t, cx - w / 2, y);
    }

    private static int width(Graphics2D g, String t, float size) {
        g.setFont(RetroStyle.font(size));
        return g.getFontMetrics().stringWidth(t);
    }
}
