package com.pokemon.battle.ui;

import javax.swing.Timer;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/**
 * Pantalla de carga con el estilo retro: una bola que gira y una barra que
 * corre. Tambien sirve para mostrar un error con la opcion de volver.
 */
public class LoadingScreen extends RetroScreen {

    private final Timer timer;
    private String message = "CARGANDO";
    private String errorText;
    private Runnable onCancel;
    private int tick = 0;

    public LoadingScreen() {
        super("CARGANDO");
        timer = new Timer(40, e -> {
            tick++;
            repaint();
        });
        bindKey("ENTER", this::goBack);
        // La animacion solo corre mientras la pantalla se ve
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                tick = 0;
                timer.start();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                timer.stop();
            }
        });
    }

    /** Cambia el texto y quita cualquier error anterior. */
    public void setMessage(String message) {
        this.message = message;
        this.errorText = null;
        this.onCancel = null;
        repaint();
    }

    /** Muestra un error; ESC o Enter ejecutan onBack. */
    public void showError(String text, Runnable onBack) {
        this.errorText = text.toUpperCase();
        this.onCancel = onBack;
        repaint();
    }

    @Override
    protected void goBack() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    @Override
    protected void paintFooterButtons(Graphics2D g) {
        if (onCancel != null) {
            super.paintFooterButtons(g);
        }
    }

    @Override
    protected void paintContent(Graphics2D g) {
        if (errorText != null) {
            drawError(g);
            return;
        }
        RetroStyle.drawPanel(g, 230, 150, 500, 340);
        drawBall(g, 480, 262, 52);

        g.setFont(RetroStyle.font(36));
        String dots = ".".repeat((tick / 8) % 4);
        int w = g.getFontMetrics().stringWidth(message);
        // El texto se centra sin los puntos, asi no se mueve al animarse
        RetroStyle.shadowText(g, message + dots, (BOARD_W - w) / 2, 372, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        drawBar(g);
    }

    private void drawBall(Graphics2D g, int cx, int cy, int r) {
        Graphics2D b = (Graphics2D) g.create();
        b.translate(cx, cy);
        b.rotate(Math.toRadians((tick * 9) % 360));
        b.setColor(RetroStyle.OUTLINE);
        b.fillOval(-r - 4, -r - 4, 2 * r + 8, 2 * r + 8);
        b.setColor(RetroStyle.RED);
        b.fillArc(-r, -r, 2 * r, 2 * r, 0, 180);
        b.setColor(RetroStyle.WHITE);
        b.fillArc(-r, -r, 2 * r, 2 * r, 180, 180);
        b.setColor(RetroStyle.OUTLINE);
        b.fillRect(-r, -5, 2 * r, 10);
        b.fillOval(-18, -18, 36, 36);
        b.setColor(RetroStyle.WHITE);
        b.fillOval(-11, -11, 22, 22);
        b.dispose();
    }

    private void drawBar(Graphics2D g) {
        int x = 290;
        int y = 412;
        int w = 380;
        int h = 24;
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(RetroStyle.WHITE);
        g.fillRoundRect(x + 3, y + 3, w - 6, h - 6, h - 6, h - 6);

        Shape oldClip = g.getClip();
        g.clipRect(x + 3, y + 3, w - 6, h - 6);
        int blockW = 110;
        int pos = (tick * 7) % (w + blockW) - blockW;
        g.setPaint(new GradientPaint(0, y, RetroStyle.BLUE, 0, y + h, RetroStyle.BLUE_DARK));
        g.fillRoundRect(x + pos, y + 3, blockW, h - 6, h - 6, h - 6);
        g.setClip(oldClip);
    }

    private void drawError(Graphics2D g) {
        RetroStyle.drawPanel(g, 180, 150, 600, 320);
        g.setFont(RetroStyle.font(40));
        String title = "ERROR";
        int w = g.getFontMetrics().stringWidth(title);
        RetroStyle.shadowText(g, title, (BOARD_W - w) / 2, 210, RetroStyle.RED_DARK, RetroStyle.SHADOW_LIGHT);

        g.setFont(RetroStyle.font(26));
        List<String> lines = RetroText.wrap(g.getFontMetrics(), errorText, 540);
        for (int i = 0; i < lines.size() && i < 6; i++) {
            RetroStyle.shadowText(g, lines.get(i), 210, 262 + i * 34, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
    }
}