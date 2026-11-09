package com.pokemon.battle.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Base of every retro screen of the game (Pokedex, trainer selection, combat
 * history). It draws what all of them share: the notebook grid, the blue title
 * bar, the gray key bar and the "ESC VOLVER" button. Each screen is designed on
 * a fixed 960x640 board that is scaled to the window, so it also works in full
 * screen. Subclasses only paint their own content and react to their keys.
 */
public abstract class RetroScreen extends JPanel {

    protected static final int BOARD_W = 960;
    protected static final int BOARD_H = 640;
    protected static final int CONTENT_TOP = 72;
    protected static final int FOOTER_Y = 590;
    protected static final Rectangle BACK_BUTTON = new Rectangle(24, 598, 170, 36);

    private final String title;

    /**
     * @param title text of the blue bar, for example "POKEDEX"
     */
    protected RetroScreen(String title) {
        this.title = title;
        setBackground(RetroStyle.CREAM);
        bindKey("ESCAPE", this::goBack);
        installMouse();
    }

    // ------------------------------------------------------------------ lo que cada pantalla define

    /**
     * Paints the content of the screen on the 960x640 board, between the title
     * bar and the key bar.
     */
    protected abstract void paintContent(Graphics2D g);

    /**
     * Leaves the screen (ESC key or the "ESC VOLVER" button).
     */
    protected abstract void goBack();

    /**
     * Paints the key hints of the bottom bar. By default only "ESC VOLVER";
     * screens with more keys call this method and add their buttons.
     */
    protected void paintFooterButtons(Graphics2D g) {
        RetroStyle.drawKeyButton(g, "ESC  VOLVER", BACK_BUTTON.x, BACK_BUTTON.y, BACK_BUTTON.width, BACK_BUTTON.height);
    }

    /**
     * Called when the user clicks the board, with coordinates of the 960x640 board.
     */
    protected void onBoardClick(int x, int y, int clickCount) {
    }

    /**
     * Called when the user turns the mouse wheel (positive = down).
     */
    protected void onWheel(int rotation) {
    }

    // ------------------------------------------------------------------ ayudas para las subclases

    /**
     * Links a key with an action while this screen is the one being shown.
     *
     * @param key    name of the key, as in KeyStroke ("UP", "ENTER", "ESCAPE"...)
     * @param action what to do
     */
    protected final void bindKey(String key, Runnable action) {
        String id = getClass().getSimpleName() + "-" + key;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), id);
        getActionMap().put(id, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /**
     * Draws a centered message box, used for "CARGANDO..." or errors.
     */
    protected void drawMessage(Graphics2D g, String text) {
        g.setFont(RetroStyle.font(30));
        int w = g.getFontMetrics().stringWidth(text);
        int boxW = Math.max(500, w + 80);
        RetroStyle.drawPanel(g, (BOARD_W - boxW) / 2, 260, boxW, 90);
        RetroStyle.shadowText(g, text, (BOARD_W - w) / 2, 314, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
    }

    // ------------------------------------------------------------------ dibujo comun

    @Override
    protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double scale = scale();
        double offsetX = (getWidth() - BOARD_W * scale) / 2;
        double offsetY = (getHeight() - BOARD_H * scale) / 2;
        RetroStyle.drawGrid(g, getWidth(), getHeight(), (int) Math.max(8, 28 * scale));
        drawFullWidthBars(g, scale, offsetY);
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);

        drawHeader(g);
        drawFooter(g);
        paintContent(g);
        g.dispose();
    }

    // En pantallas mas anchas que el tablero, las barras de arriba y abajo llegan a los bordes
    private void drawFullWidthBars(Graphics2D g, double scale, double offsetY) {
        int headerBottom = (int) Math.round(offsetY + 56 * scale);
        g.setPaint(new GradientPaint(0, (float) offsetY, RetroStyle.BLUE, 0, headerBottom, RetroStyle.BLUE_DARK));
        g.fillRect(0, 0, getWidth(), headerBottom);
        g.setColor(RetroStyle.OUTLINE);
        g.fillRect(0, headerBottom, getWidth(), (int) Math.ceil(4 * scale));
        int footerTop = (int) Math.round(offsetY + FOOTER_Y * scale);
        g.fillRect(0, footerTop, getWidth(), (int) Math.ceil(4 * scale));
        g.setPaint(new GradientPaint(0, footerTop, new Color(0xD6D6D6), 0, getHeight(), new Color(0xAFAFAF)));
        g.fillRect(0, footerTop + (int) Math.ceil(4 * scale), getWidth(), getHeight());
    }

    private void drawHeader(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, RetroStyle.BLUE, 0, 56, RetroStyle.BLUE_DARK));
        g.fillRect(0, 0, BOARD_W, 56);
        g.setColor(RetroStyle.OUTLINE);
        g.fillRect(0, 56, BOARD_W, 4);
        g.setColor(new Color(255, 255, 255, 90));
        g.fillRect(0, 3, BOARD_W, 2);
        g.setFont(RetroStyle.font(40));
        RetroStyle.shadowText(g, title, 24, 42, RetroStyle.WHITE, RetroStyle.OUTLINE);
    }

    private void drawFooter(Graphics2D g) {
        g.setColor(RetroStyle.OUTLINE);
        g.fillRect(0, FOOTER_Y, BOARD_W, 4);
        g.setPaint(new GradientPaint(0, FOOTER_Y + 4, new Color(0xD6D6D6), 0, BOARD_H, new Color(0xAFAFAF)));
        g.fillRect(0, FOOTER_Y + 4, BOARD_W, BOARD_H - FOOTER_Y - 4);
        g.setFont(RetroStyle.font(24));
        paintFooterButtons(g);
    }

    // ------------------------------------------------------------------ mouse

    private void installMouse() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int[] p = toBoard(e.getX(), e.getY());
                if (BACK_BUTTON.contains(p[0], p[1])) {
                    goBack();
                } else {
                    onBoardClick(p[0], p[1], e.getClickCount());
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                onWheel(e.getWheelRotation());
            }
        };
        addMouseListener(mouse);
        addMouseWheelListener(mouse);
    }

    // Convierte un punto de la ventana a coordenadas del tablero de 960x640
    private int[] toBoard(int x, int y) {
        double scale = scale();
        double offsetX = (getWidth() - BOARD_W * scale) / 2;
        double offsetY = (getHeight() - BOARD_H * scale) / 2;
        return new int[]{(int) ((x - offsetX) / scale), (int) ((y - offsetY) / scale)};
    }

    private double scale() {
        return Math.min(getWidth() / (double) BOARD_W, getHeight() / (double) BOARD_H);
    }
}
