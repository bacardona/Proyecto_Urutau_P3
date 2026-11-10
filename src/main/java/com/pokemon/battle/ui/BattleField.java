package com.pokemon.battle.ui;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.StatusCondition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class BattleField extends JPanel {

    private static final Color SKY = new Color(0xDFF3E0);
    private static final Color GROUND = new Color(0xB7DFAA);
    private static final Color SHADOW = new Color(0x8BC381);
    private static final Color CREAM = new Color(0xF7F3E0);
    private static final Color DARK = new Color(0x3A3A3A);

    private Combatant player;
    private Combatant rival;

    // HP que se dibuja ahora mismo: baja poco a poco hasta el HP real
    private double playerShownHp;
    private double rivalShownHp;
    private final Timer timer;

    // Mismos sprites y misma animacion que la Pokedex
    private final SpriteLoader sprites = SpriteLoader.shared();
    private final Timer animationTimer;
    private int frame;

    public BattleField() {
        setPreferredSize(new Dimension(960, 440));
        timer = new Timer(30, e -> animateHp());
        animationTimer = new Timer(1000 / GifAnimation.FPS, e -> {
            frame++;
            repaint();
        });
        // La animacion solo corre mientras el campo se ve en pantalla
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    animationTimer.start();
                } else {
                    animationTimer.stop();
                }
            }
        });
    }

    public void setCombatants(Combatant player, Combatant rival) {
        this.player = player;
        this.rival = rival;
        this.playerShownHp = player.getHitPoints().getCurrent();
        this.rivalShownHp = rival.getHitPoints().getCurrent();
        // Se piden los GIF de una vez para que esten listos al dibujar
        sprites.getOrLoad(player.getName(), true, this::repaint);
        sprites.getOrLoad(rival.getName(), false, this::repaint);
        repaint();
    }

    /** Llamar despues de que cambie la vida o el estado: anima las barras. */
    public void refresh() {
        if (!timer.isRunning()) {
            timer.start();
        }
        repaint();
    }

    private void animateHp() {
        if (player == null || rival == null) {
            timer.stop();
            return;
        }
        playerShownHp = step(playerShownHp, player.getHitPoints().getCurrent());
        rivalShownHp = step(rivalShownHp, rival.getHitPoints().getCurrent());
        repaint();
        if (playerShownHp == player.getHitPoints().getCurrent()
                && rivalShownHp == rival.getHitPoints().getCurrent()) {
            timer.stop();
        }
    }

    private double step(double shown, int target) {
        double diff = target - shown;
        if (Math.abs(diff) < 0.5) {
            return target;
        }
        return shown + Math.signum(diff) * Math.max(0.5, Math.abs(diff) * 0.15);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int w = getWidth();
        int h = getHeight();

        // Fondo: cielo y pasto
        g2.setColor(SKY);
        g2.fillRect(0, 0, w, h);
        int groundY = (int) (h * 0.85);
        g2.setColor(GROUND);
        g2.fillRect(0, groundY, w, h - groundY);

        // Sombras
        drawShadow(g2, 0.72 * w, 0.43 * h, 0.354 * w, 0.136 * h);
        drawShadow(g2, 0.30 * w, 0.89 * h, 0.417 * w, 0.17 * h);

        // Sprites animados con los pies sobre la sombra (rival de frente, jugador de espalda)
        drawCombatant(g2, rival, false, 0.72 * w, 0.43 * h, 170);
        drawCombatant(g2, player, true, 0.30 * w, 0.89 * h, 230);

        // Cuadros de informacion
        if (rival != null) {
            drawInfoBox(g2, (int) (0.0375 * w), (int) (0.088 * h), (int) (0.375 * w),
                    (int) (0.248 * h), rival, rivalShownHp, false);
        }
        if (player != null) {
            drawInfoBox(g2, (int) (0.5625 * w), (int) (0.708 * h), (int) (0.395 * w),
                    (int) (0.256 * h), player, playerShownHp, true);
        }
        g2.dispose();
    }

    private void drawShadow(Graphics2D g2, double cx, double cy, double w, double h) {
        g2.setColor(SHADOW);
        g2.fill(new Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h));
    }

    // Dibuja el sprite con los pies sobre la sombra. Igual que en la Pokedex,
    // todos los Pokemon se ajustan al mismo cuadro, asi se ven del mismo tamano.
    private void drawCombatant(Graphics2D g2, Combatant c, boolean back, double cx,
                               double feetY, int box) {
        if (c == null || c.isFainted()) {
            return;
        }
        String name = c.getName();
        if (!sprites.isLoaded(name, back)) {
            // Todavia se esta leyendo el GIF; al terminar se vuelve a dibujar
            sprites.getOrLoad(name, back, this::repaint);
            return;
        }
        GifAnimation sprite = sprites.getOrLoad(name, back, this::repaint);
        if (sprite == null) {
            drawSpritePlaceholder(g2, cx, feetY - box / 2.0, box, back ? "JUGADOR" : "RIVAL");
            return;
        }
        double factor = Math.min(box / (double) sprite.width(), box / (double) sprite.height());
        int w = (int) Math.round(sprite.width() * factor);
        int h = (int) Math.round(sprite.height() * factor);
        RetroStyle.drawPixelImage(g2, sprite.frame(frame), (int) (cx - w / 2.0), (int) (feetY - h), w, h);
    }

    // Marcador que se usa solo cuando falta la imagen del Pokemon
    private void drawSpritePlaceholder(Graphics2D g2, double cx, double cy, double d, String text) {
        g2.setColor(new Color(0x4A7A4A));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{8f, 8f}, 0f));
        g2.draw(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
        g2.setFont(PixelFont.get(20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (int) (cx - fm.stringWidth(text) / 2.0), (int) cy);
    }

    private void drawInfoBox(Graphics2D g2, int x, int y, int w, int h, Combatant c,
                             double shownHp, boolean showNumbers) {
        g2.setColor(CREAM);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 20, 20));
        g2.setColor(DARK);
        g2.setStroke(new BasicStroke(4f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 20, 20));

        int pad = 16;

        // Nombre y nivel
        g2.setFont(PixelFont.get(30));
        g2.drawString(c.getName().toUpperCase(), x + pad, y + 30);
        String level = "Nv" + c.getPokemon().getLevel();
        g2.drawString(level, x + w - pad - g2.getFontMetrics().stringWidth(level), y + 30);

        // Etiqueta HP y barra
        g2.setFont(PixelFont.get(20));
        g2.setColor(new Color(0xC0392B));
        g2.drawString("HP", x + pad, y + 60);
        int barX = x + pad + 34;
        int barW = w - pad * 2 - 34;
        int barH = 12;
        int barY = y + 48;
        g2.setColor(DARK);
        g2.fill(new RoundRectangle2D.Double(barX, barY, barW, barH, barH, barH));
        double ratio = shownHp / Math.max(1, c.getHitPoints().getMax());
        ratio = Math.max(0, Math.min(1, ratio));
        g2.setColor(barColor(ratio));
        g2.fill(new RoundRectangle2D.Double(barX, barY, barW * ratio, barH, barH, barH));

        // Estado (izquierda) y HP en numeros (derecha, solo el jugador)
        int textY = y + h - 16;
        String status = statusText(c.getStatus());
        if (!status.isEmpty()) {
            g2.setColor(new Color(0x6A4BBF));
            g2.drawString(status, x + pad, textY);
        }
        if (showNumbers) {
            String hp = (int) Math.ceil(shownHp) + " / " + c.getHitPoints().getMax();
            g2.setColor(DARK);
            g2.drawString(hp, x + w - pad - g2.getFontMetrics().stringWidth(hp), textY);
        }
    }

    private Color barColor(double ratio) {
        if (ratio > 0.5) {
            return new Color(0x3DA35D);
        }
        if (ratio > 0.2) {
            return new Color(0xE0A526);
        }
        return new Color(0xC0392B);
    }

    private String statusText(StatusCondition s) {
        switch (s) {
            case PARALYSIS: return "PARALIZADO";
            case SLEEP: return "DORMIDO";
            case POISON: return "ENVENENADO";
            case BURN: return "QUEMADO";
            case FREEZE: return "CONGELADO";
            default: return "";
        }
    }
}