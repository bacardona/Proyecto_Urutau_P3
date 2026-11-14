package com.pokemon.battle.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class IntroScreen extends JPanel {

    private static final Color NAVY = new Color(0x2D3A52);
    private static final Color CREAM = new Color(0xF7F3E0);
    private static final int TOTAL_TICKS = 150; // unos 4,5 s a 30 ms por tick

    private final String title;
    private final String[] members;
    private final Runnable onFinish;
    private final Timer timer;
    private int tick = 0;
    private boolean finished = false;

    public IntroScreen(String title, String[] members, Runnable onFinish) {
        this.title = title;
        this.members = members;
        this.onFinish = onFinish;
        setBackground(NAVY);
        setFocusable(true);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "skip");
        getActionMap().put("skip", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finish();
            }
        });

        timer = new Timer(30, e -> {
            tick++;
            repaint();
            if (tick >= TOTAL_TICKS) {
                finish();
            }
        });
    }

    // Llamar cada vez que se muestre esta pantalla
    public void start() {
        tick = 0;
        finished = false;
        timer.start();
        requestFocusInWindow();
    }

    private void finish() {
        if (finished) return;
        finished = true;
        timer.stop();
        onFinish.run();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // Título: aparece en los primeros 30 ticks
        float alpha = Math.min(1f, tick / 30f);
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.setColor(CREAM);
        g2.setFont(PixelFont.get(36));
        drawCentered(g2, title, w, h / 3);

        // Integrantes: aparecen uno por uno desde el tick 40
        g2.setFont(PixelFont.get(16));
        for (int i = 0; i < members.length; i++) {
            float a = Math.max(0f, Math.min(1f, (tick - 40 - i * 15) / 20f));
            g2.setComposite(AlphaComposite.SrcOver.derive(a));
            drawCentered(g2, members[i], w, h / 2 + i * 28);
        }

        // Barra de carga
        g2.setComposite(AlphaComposite.SrcOver);
        int barW = w / 2;
        int barX = (w - barW) / 2;
        int barY = h - 110;
        g2.setColor(CREAM);
        g2.drawRect(barX, barY, barW, 18);
        g2.fillRect(barX + 3, barY + 3, (int) ((barW - 6) * (tick / (float) TOTAL_TICKS)), 12);

        g2.setFont(PixelFont.get(11));
        drawCentered(g2, "PRESIONA ENTER PARA SALTAR", w, barY + 48);
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int width, int y) {
        int x = (width - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }
}
