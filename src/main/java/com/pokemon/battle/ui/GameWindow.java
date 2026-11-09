package com.pokemon.battle.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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

    // Pantalla temporal para lo que todavía no existe. ESC vuelve al menú.
    private JComponent placeholder(String text) {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();

            // 1. Intro (cambia el título y los nombres por los reales)
            IntroScreen intro = new IntroScreen("POKKEN BATTLE",
                    new String[]{"Jhonatan David Galindo Gómez", "Luis Javier Chávez Montes", "Brayan Andres Cardona Reyes", "Angel David Bermudez Gonzalez"},
                    () -> window.showScreen("menu"));

            // 2. Menú principal
            MenuScreen mainMenu = new MenuScreen("POKKEN BATTLE",
                    new String[]{"COMBATE", "POKEDEX", "HISTORIAL", "SALIR"},
                    new Runnable[]{
                            () -> window.showScreen("combatMenu"),
                            () -> window.showScreen("pokedex"),
                            () -> window.showScreen("history"),
                            () -> System.exit(0)});

            // 3. Submenú de combate (por ahora PVE abre la batalla de prueba)
            MenuScreen combatMenu = new MenuScreen("COMBATE",
                    new String[]{"PVE", "PVP", "VOLVER"},
                    new Runnable[]{
                            () -> window.showScreen("battleDemo"),
                            () -> window.showScreen("pvp"),
                            () -> window.showScreen("menu")});

            window.addScreen("intro", intro);
            window.addScreen("menu", mainMenu);
            window.addScreen("combatMenu", combatMenu);
            window.addScreen("battleDemo", BattleDemo.create());
            window.addScreen("pvp", window.placeholder("PVP (pendiente)"));
            window.addScreen("pokedex", window.placeholder("POKEDEX (pendiente)"));
            window.addScreen("history", window.placeholder("HISTORIAL (pendiente)"));

            window.showScreen("intro");
            window.setVisible(true);
            intro.start();
        });
    }
}