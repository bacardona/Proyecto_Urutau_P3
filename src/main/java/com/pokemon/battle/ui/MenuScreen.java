package com.pokemon.battle.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Menú reutilizable: un título y una lista de opciones.
 * Se usa para el menú principal y para el submenú de combate.
 */
public class MenuScreen extends JPanel {

    private static final Color CREAM = new Color(0xF7F3E0);
    private static final Color DARK = new Color(0x2D3A52);

    private final String[] options;
    private final Runnable[] actions;
    private final JLabel[] optionLabels;
    private int selected = 0;

    public MenuScreen(String title, String[] options, Runnable[] actions) {
        this.options = options;
        this.actions = actions;
        this.optionLabels = new JLabel[options.length];

        setLayout(new GridBagLayout());
        setBackground(CREAM);
        setBorder(BorderFactory.createLineBorder(DARK, 8));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(PixelFont.get(28));
        titleLabel.setForeground(DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(40));

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            JLabel label = new JLabel();
            label.setFont(PixelFont.get(20));
            label.setForeground(DARK);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            label.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    selected = index;
                    refresh();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selected = index;
                    refresh();
                    choose();
                }
            });
            optionLabels[i] = label;
            list.add(label);
        }
        content.add(list);
        add(content);

        bind("UP", () -> move(-1));
        bind("DOWN", () -> move(1));
        bind("ENTER", this::choose);
        refresh();
    }

    // Llamar cuando se vuelve a mostrar el menú, para que empiece en la primera opción
    public void reset() {
        selected = 0;
        refresh();
    }

    private void bind(String key, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void move(int delta) {
        selected = Math.floorMod(selected + delta, options.length);
        refresh();
    }

    private void choose() {
        actions[selected].run();
    }

    private void refresh() {
        for (int i = 0; i < optionLabels.length; i++) {
            optionLabels[i].setText((i == selected ? "> " : "  ") + options[i]);
        }
    }
}