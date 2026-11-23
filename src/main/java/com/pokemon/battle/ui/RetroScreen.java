package com.pokemon.battle.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashMap;
import java.util.Map;


/**
 * Base of every retro screen of the game (Pokedex, trainer selection, combat
 * history). It draws what all of them share: the animated GIF background, the
 * dark key bar and the "ESC VOLVER" button. Each screen is designed on
 * a fixed 960x640 board that is scaled to the window, so it also works in full
 * screen. Subclasses only paint their own content and react to their keys.
 */
public abstract class RetroScreen extends JPanel {

    protected static final int BOARD_W = 960;
    protected static final int BOARD_H = 640;
    protected static final int CONTENT_TOP = 20;
    protected static final int FOOTER_Y = 590;
    protected static final Rectangle BACK_BUTTON = new Rectangle(24, 598, 170, 36);
    private static final AnimatedBackground BACKGROUND = new AnimatedBackground("/backgrounds/loading.gif"); // usa la misma ruta que LoadingScreen
    private static final Color OVERLAY = new Color(0, 0, 0, 60); // capa oscura suave para que el texto se lea

    private final String title;
    private final Map<String, Runnable> keyActions = new HashMap<>();

    /**
     * @param title name of the screen, for example "POKEDEX". It is no longer drawn
     *              in a bar; screens can show it with {@link #screenTitle()}.
     */
    protected RetroScreen(String title) {
        this.title = title;
        setBackground(RetroStyle.CREAM);
        bindKey("ESCAPE", this::goBack);
        installMouse();
    }

    /**
     * @return the name of the screen, so subclasses can draw it where they want
     */
    protected final String screenTitle() {
        return title;
    }

    // ------------------------------------------------------------------ lo que cada pantalla define

    /**
     * Paints the content of the screen on the 960x640 board, between the top
     * and the key bar.
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
        keyActions.put(key, action);
        String id = getClass().getSimpleName() + "-" + key;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), id);
        getActionMap().put(id, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /** Simula que se pulsó esa tecla (la usa el mando). Devuelve false si la pantalla no la usa. */
    public final boolean pressKey(String key) {
        Runnable action = keyActions.get(key);
        if (action == null) return false;
        action.run();
        return true;
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

    protected void paintBackdrop(Graphics2D g, int w, int h) {
        BACKGROUND.draw(g, 0, 0, w, h, this);
        g.setColor(OVERLAY);
        g.fillRect(0, 0, w, h);
    }

    @Override
    protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double scale = scale();
        double offsetX = (getWidth() - BOARD_W * scale) / 2;
        double offsetY = (getHeight() - BOARD_H * scale) / 2;

        paintBackdrop(g, getWidth(), getHeight());
        drawFooterBar(g, scale, offsetY);

        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        drawFooter(g);
        paintContent(g);
        g.dispose();
    }


    // La barra de abajo llega de lado a lado de la ventana, aunque sea mas ancha que el tablero
    private void drawFooterBar(Graphics2D g, double scale, double offsetY) {
        int footerTop = (int) Math.round(offsetY + FOOTER_Y * scale);
        g.setColor(new Color(6, 14, 11, 215));
        g.fillRect(0, footerTop, getWidth(), getHeight() - footerTop);
        g.setColor(RetroStyle.EDGE);
        g.fillRect(0, footerTop, getWidth(), (int) Math.ceil(3 * scale));
    }

    private void drawFooter(Graphics2D g) {
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