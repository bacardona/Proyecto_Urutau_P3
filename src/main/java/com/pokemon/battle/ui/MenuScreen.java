package com.pokemon.battle.ui;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Menu reutilizable con el estilo retro: una lista de opciones.
 * Sirve para el menu principal y para el submenu de combate.
 */
public class MenuScreen extends RetroScreen {

    private static final int ROW_H = 52;
    private static final int LIST_W = 420;

    private final String[] options;
    private final Runnable[] actions;
    private final Runnable onBack;
    private final Rectangle list;
    private int selected = 0;

    // Menu sin boton de volver (el principal)
    public MenuScreen(String title, String[] options, Runnable[] actions) {
        this(title, options, actions, null);
    }

    // Con onBack, ESC y el boton "ESC VOLVER" ejecutan esa accion
    public MenuScreen(String title, String[] options, Runnable[] actions, Runnable onBack) {
        super(title);
        this.options = options;
        this.actions = actions;
        this.onBack = onBack;
        int height = options.length * ROW_H + 24;
        int x = (BOARD_W - LIST_W) / 2;
        int y = CONTENT_TOP + (FOOTER_Y - CONTENT_TOP - height) / 2;
        this.list = new Rectangle(x, y, LIST_W, height);
        bindKey("UP", () -> move(-1));
        bindKey("DOWN", () -> move(1));
        bindKey("ENTER", this::choose);
    }

    // Llamar cuando se vuelve a mostrar el menu, para que empiece en la primera opcion
    public void reset() {
        selected = 0;
        repaint();
    }

    @Override
    protected void goBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    private void move(int delta) {
        selected = Math.floorMod(selected + delta, options.length);
        repaint();
    }

    private void choose() {
        actions[selected].run();
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        int top = list.y + 12;
        if (list.contains(x, y) && y >= top) {
            int index = (y - top) / ROW_H;
            if (index < options.length) {
                selected = index;
                repaint();
                choose();
            }
        }
    }

    @Override
    protected void onWheel(int rotation) {
        move(rotation);
    }

    @Override
    protected void paintFooterButtons(Graphics2D g) {
        if (onBack != null) {
            super.paintFooterButtons(g);
        }
        RetroStyle.drawArrowKeyButton(g, true, "MOVER", 214, 598, 170, 36);
        RetroStyle.drawKeyButton(g, "ENTER  ELEGIR", 404, 598, 190, 36);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        RetroStyle.drawListFrame(g, list.x, list.y, list.width, list.height);
        g.setFont(RetroStyle.font(32));
        for (int i = 0; i < options.length; i++) {
            int y = list.y + 12 + i * ROW_H;
            boolean isSelected = i == selected;
            if (isSelected) {
                RetroStyle.drawSelectedRow(g, list.x + 22, y - 3, list.width - 44, ROW_H + 6);
            }
            RetroStyle.drawListDot(g, list.x + 46, y + ROW_H / 2, isSelected);
            RetroStyle.shadowText(g, options[i], list.x + 74, y + 36,
                    isSelected ? RetroStyle.RED_DARK : RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
    }
}