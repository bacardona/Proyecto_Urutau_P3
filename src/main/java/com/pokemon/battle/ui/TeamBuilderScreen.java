package com.pokemon.battle.ui;

import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Team;
import com.pokemon.battle.model.Type;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla para armar el equipo de 3 Pokemon. Lista a la izquierda,
 * vista previa animada a la derecha y las casillas del equipo abajo.
 */
public class TeamBuilderScreen extends JPanel {

    private static final Color CREAM = new Color(0xF7F3E0);
    private static final Color DARK = new Color(0x2D3A52);
    private static final Color SELECTED_BG = new Color(0xDFF3E0);
    private static final Color ERROR = new Color(0xC0392B);

    private final Consumer<List<Pokemon>> onDone;
    private final DefaultListModel<Pokemon> model = new DefaultListModel<>();
    private final JList<Pokemon> list = new JList<>(model);
    private final List<Pokemon> chosen = new ArrayList<>();
    private final JLabel titleLabel = new JLabel("ARMA TU EQUIPO");
    private final JLabel infoLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel[] slotLabels = new JLabel[Team.SIZE];
    private final PreviewPanel preview = new PreviewPanel();
    private final JButton confirmButton;

    public TeamBuilderScreen(Consumer<List<Pokemon>> onDone, Runnable onBack) {
        this.onDone = onDone;

        setLayout(new BorderLayout(12, 12));
        setBackground(CREAM);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK, 8),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        // Arriba: título
        titleLabel.setFont(PixelFont.get(26));
        titleLabel.setForeground(DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // Centro: lista a la izquierda, vista previa a la derecha
        list.setFont(PixelFont.get(16));
        list.setFixedCellHeight(34);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PokemonRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showPreview(list.getSelectedValue());
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSelected();
                }
            }
        });
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "add");
        list.getActionMap().put("add", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(DARK, 4));

        infoLabel.setFont(PixelFont.get(14));
        infoLabel.setForeground(DARK);
        infoLabel.setVerticalAlignment(SwingConstants.TOP);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(CREAM);
        right.setBorder(BorderFactory.createLineBorder(DARK, 4));
        right.add(preview, BorderLayout.CENTER);
        right.add(infoLabel, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setOpaque(false);
        center.add(scroll);
        center.add(right);
        add(center, BorderLayout.CENTER);

        // Abajo: casillas del equipo, mensaje y botones
        JPanel slots = new JPanel(new GridLayout(1, Team.SIZE, 12, 0));
        slots.setOpaque(false);
        for (int i = 0; i < Team.SIZE; i++) {
            slotLabels[i] = new JLabel("", SwingConstants.CENTER);
            slotLabels[i].setFont(PixelFont.get(16));
            slotLabels[i].setForeground(DARK);
            slotLabels[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARK, 3),
                    BorderFactory.createEmptyBorder(8, 4, 8, 4)));
            slots.add(slotLabels[i]);
        }

        messageLabel.setFont(PixelFont.get(14));
        messageLabel.setForeground(ERROR);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        confirmButton = makeButton("CONFIRMAR", this::confirm);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.add(makeButton("AGREGAR", this::addSelected));
        buttons.add(makeButton("QUITAR ULTIMO", this::removeLast));
        buttons.add(confirmButton);
        buttons.add(makeButton("VOLVER", onBack));

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(slots);
        south.add(Box.createVerticalStrut(6));
        south.add(messageLabel);
        south.add(Box.createVerticalStrut(6));
        south.add(buttons);
        add(south, BorderLayout.SOUTH);

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
    public void prepare(String trainerName, List<Pokemon> catalog) {
        titleLabel.setText("EQUIPO DE " + trainerName.toUpperCase());
        model.clear();
        for (Pokemon p : catalog) {
            model.addElement(p);
        }
        chosen.clear();
        updateSlots();
        setMessage(" ");
        if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        }
        SwingUtilities.invokeLater(list::requestFocusInWindow);
    }

    private void addSelected() {
        Pokemon selected = list.getSelectedValue();
        if (selected == null) {
            return;
        }
        if (chosen.size() >= Team.SIZE) {
            setMessage("EQUIPO COMPLETO: CONFIRMA O QUITA UNO");
            return;
        }
        if (chosen.contains(selected)) {
            setMessage("YA ESTA EN TU EQUIPO");
            return;
        }
        chosen.add(selected);
        setMessage(" ");
        updateSlots();
        list.repaint();
    }

    private void removeLast() {
        if (!chosen.isEmpty()) {
            chosen.remove(chosen.size() - 1);
            setMessage(" ");
            updateSlots();
            list.repaint();
        }
    }

    private void confirm() {
        if (chosen.size() != Team.SIZE) {
            setMessage("ELIGE " + Team.SIZE + " POKEMON");
            return;
        }
        onDone.accept(new ArrayList<>(chosen));
    }

    private void updateSlots() {
        for (int i = 0; i < slotLabels.length; i++) {
            String text = i < chosen.size() ? chosen.get(i).getName().toUpperCase() : "---";
            slotLabels[i].setText((i + 1) + ". " + text);
        }
        confirmButton.setEnabled(chosen.size() == Team.SIZE);
    }

    private void showPreview(Pokemon p) {
        preview.show(p);
        if (p == null) {
            infoLabel.setText(" ");
            return;
        }
        // Tipos en espanol, como en la Pokedex
        StringBuilder sb = new StringBuilder("<html>TIPO: ")
                .append(RetroStyle.typeName(p.getPrimaryType()));
        if (p.getSecondaryType() != null && p.getSecondaryType() != Type.NONE) {
            sb.append(" / ").append(RetroStyle.typeName(p.getSecondaryType()));
        }
        sb.append("<br>ATAQUES:<br>");
        for (Move m : p.getMoves()) {
            sb.append("- ").append(m.getName().toUpperCase()).append("<br>");
        }
        infoLabel.setText(sb.append("</html>").toString());
    }

    private void setMessage(String text) {
        messageLabel.setText(text);
    }

    private JButton makeButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(PixelFont.get(16));
        button.setBackground(DARK);
        button.setForeground(CREAM);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFocusable(false); // así Enter y las flechas siguen yendo a la lista
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }

    // Cada fila: icono de tipo + nombre. Los que ya están en el equipo llevan "*"
    private class PokemonRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                      boolean isSelected, boolean hasFocus) {
            super.getListCellRendererComponent(l, value, index, isSelected, hasFocus);
            Pokemon p = (Pokemon) value;
            boolean inTeam = chosen.contains(p);
            setText((inTeam ? "* " : "  ") + p.getName().toUpperCase());
            setIcon(SpriteLoader.typeIcon(p.getPrimaryType(), 28));
            setFont(PixelFont.get(16));
            setForeground(DARK);
            setBackground(isSelected ? SELECTED_BG : CREAM);
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            return this;
        }
    }

    // Sprite grande de frente, animado igual que en la Pokedex
    private static class PreviewPanel extends JPanel {
        // Lado del cuadro donde cabe cualquier sprite, asi todos se ven del mismo tamano
        private static final int SPRITE_TARGET = 200;

        private final SpriteLoader sprites = SpriteLoader.shared();
        private final Timer animationTimer;
        private Pokemon pokemon;
        private int frame;

        PreviewPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 260));
            animationTimer = new Timer(1000 / GifAnimation.FPS, e -> {
                frame++;
                repaint();
            });
            // La animacion solo corre mientras la pantalla se ve
            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) {
                        animationTimer.start();
                    } else {
                        animationTimer.stop();
                    }
                }
            });
        }

        void show(Pokemon p) {
            this.pokemon = p;
            frame = 0;
            if (p != null) {
                // Se lee en el otro hilo; al terminar se vuelve a dibujar
                sprites.getOrLoad(p.getName(), false, this::repaint);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (pokemon == null || !sprites.isLoaded(pokemon.getName(), false)) {
                return; // Nada elegido o el GIF todavia se esta leyendo
            }
            Graphics2D g2 = (Graphics2D) g.create();
            GifAnimation sprite = sprites.getOrLoad(pokemon.getName(), false, this::repaint);
            if (sprite == null) {
                g2.setFont(PixelFont.get(16));
                g2.setColor(DARK);
                String text = "SIN IMAGEN";
                g2.drawString(text, (getWidth() - g2.getFontMetrics().stringWidth(text)) / 2,
                        getHeight() / 2);
                g2.dispose();
                return;
            }
            int box = Math.min(SPRITE_TARGET, Math.min(getWidth(), getHeight()) - 20);
            double factor = Math.min(box / (double) sprite.width(), box / (double) sprite.height());
            int w = (int) Math.round(sprite.width() * factor);
            int h = (int) Math.round(sprite.height() * factor);
            RetroStyle.drawPixelImage(g2, sprite.frame(frame), (getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
            g2.dispose();
        }
    }
}
