package com.pokemon.battle.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class GameWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);
    private final Map<String, JComponent> screens = new HashMap<>();

    public GameWindow() {
        setTitle("Pokken Battle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        container.setPreferredSize(new Dimension(960, 640));
        add(container);
        pack();
        setLocationRelativeTo(null);
    }

    // Si ya existía una pantalla con ese nombre, la reemplaza
    public void addScreen(String name, JComponent screen) {
        JComponent old = screens.put(name, screen);
        if (old != null) {
            container.remove(old);
        }
        container.add(screen, name);
        container.revalidate();
    }

    public void showScreen(String name) {
        cards.show(container, name);
    }

    // Pantalla temporal para lo que todavía no existe. ESC vuelve al menú.
    public JComponent placeholder(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(0xF7F3E0));
        JLabel label = new JLabel(text + " (ESC para volver)", SwingConstants.CENTER);
        label.setFont(PixelFont.get(20));
        panel.add(label, BorderLayout.CENTER);
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "back");
        panel.getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showScreen("menu");
            }
        });
        return panel;
    }
}