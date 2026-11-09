package com.pokemon.battle.ui;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.exceptions.ResourceNotFoundException;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.service.BattleController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Pantalla para escribir el nombre del entrenador. Si ya existe lo usa,
 * y si no, lo crea. Vive dentro de la ventana principal (CardLayout).
 */
public class TrainerNameScreen extends JPanel {

    private static final Color CREAM = new Color(0xF7F3E0);
    private static final Color DARK = new Color(0x2D3A52);
    private static final Color ERROR = new Color(0xC0392B);

    private final BattleController controller;
    private final Consumer<Player> onAccepted;
    private final JTextField nameField = new JTextField(16);
    private final JLabel messageLabel = new JLabel(" ");
    private Player excluded; // en PvP: el entrenador 1, que el 2 no puede repetir

    public TrainerNameScreen(BattleController controller, String title,
                             Consumer<Player> onAccepted, Runnable onBack) {
        this.controller = controller;
        this.onAccepted = onAccepted;

        setLayout(new GridBagLayout());
        setBackground(CREAM);
        setBorder(BorderFactory.createLineBorder(DARK, 8));

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(CREAM);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK, 4),
                BorderFactory.createEmptyBorder(24, 36, 24, 36)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(PixelFont.get(26));
        titleLabel.setForeground(DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("ESCRIBE EL NOMBRE:");
        hint.setFont(PixelFont.get(16));
        hint.setForeground(DARK);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        nameField.setFont(PixelFont.get(22));
        nameField.setForeground(DARK);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK, 3),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        nameField.setMaximumSize(nameField.getPreferredSize());
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameField.addActionListener(e -> accept()); // Enter en el campo

        messageLabel.setFont(PixelFont.get(14));
        messageLabel.setForeground(ERROR);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(makeButton("ACEPTAR", this::accept));
        buttons.add(makeButton("VOLVER", onBack));

        box.add(titleLabel);
        box.add(Box.createVerticalStrut(24));
        box.add(hint);
        box.add(Box.createVerticalStrut(10));
        box.add(nameField);
        box.add(Box.createVerticalStrut(10));
        box.add(messageLabel);
        box.add(Box.createVerticalStrut(16));
        box.add(buttons);
        add(box);

        // ESC también vuelve
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBack.run();
            }
        });
    }

    /** Llamar justo antes de mostrar la pantalla. */
    public void prepare(Player excluded) {
        this.excluded = excluded;
        nameField.setText("");
        messageLabel.setText(" ");
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
    }

    private JButton makeButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(PixelFont.get(18));
        button.setBackground(DARK);
        button.setForeground(CREAM);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }

    private void accept() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("ESCRIBE UN NOMBRE");
            return;
        }
        try {
            Player player = findOrCreate(name);
            if (excluded != null && Objects.equals(player.getId(), excluded.getId())) {
                showError("DEBE SER DISTINTO AL ENTRENADOR 1");
                return;
            }
            onAccepted.accept(player);
        } catch (ResourceNotFoundException e) {
            showError("NO SE ENCONTRO: " + e.getMessage());
        } catch (BusinessRuleException e) {
            showError(e.getMessage());
        } catch (InvalidDataException e) {
            showError("NOMBRE NO VALIDO: " + e.getMessage());
        } catch (PersistenceException e) {
            showError("PROBLEMA CON LA BASE DE DATOS");
        }
    }

    // Busca por nombre (sin importar mayúsculas); si no existe, lo crea
    private Player findOrCreate(String name) {
        for (Player candidate : controller.listPlayers()) {
            if (candidate.getName().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        return controller.createPlayer(name);
    }

    private void showError(String text) {
        messageLabel.setText("<html><div style='text-align:center;width:320px'>" + text + "</div></html>");
    }
}
