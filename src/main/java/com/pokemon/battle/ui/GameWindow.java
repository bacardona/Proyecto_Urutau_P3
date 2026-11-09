package com.pokemon.battle.ui;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);

    public GameWindow() {
        setTitle("Pokken Battle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        container.setPreferredSize(new Dimension(960, 640));
        add(container);
        pack();
        setLocationRelativeTo(null);
    }

    public void addScreen(String name, JComponent screen) {
        container.add(screen, name);
    }

    public void showScreen(String name) {
        cards.show(container, name);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.addScreen("menu", new MainMenuForm(window).getRootPanel());
            // Pantallas de prueba mientras no existan las reales
            window.addScreen("tower", BattleDemo.create());
            window.addScreen("pokedex", new JLabel("POKEDEX (pendiente)", SwingConstants.CENTER));
            window.showScreen("menu");
            window.setVisible(true);
        });
    }
}