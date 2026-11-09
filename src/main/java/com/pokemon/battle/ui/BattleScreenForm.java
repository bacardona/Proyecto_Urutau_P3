package com.pokemon.battle.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.IntConsumer;

public class BattleScreenForm {

    // Variables enlazadas con el diseñador (solo estas tres)
    private JPanel rootPanel;
    private JPanel battlefieldPanel;
    private JPanel bottomPanel;

    // Componentes que ahora crea el codigo
    private JPanel promptPanel;
    private JPanel movesPanel;
    private JPanel detailPanel;
    private JLabel promptLabel;
    private JLabel move1Label;
    private JLabel move2Label;
    private JLabel move3Label;
    private JLabel move4Label;
    private JLabel ppLabel;
    private JLabel typeLabel;

    private static final Color CREAM = new Color(0xF7F3E0);
    private static final Color DARK = new Color(0x3A3A3A);
    private static final Color NAVY = new Color(0x2D3A52);

    private BattleField field;
    private final Combatant player;
    private final JLabel[] moveLabels;
    private int selected = 0;
    private boolean inputEnabled = true;
    private IntConsumer onMoveChosen = index -> {
    };

    public BattleScreenForm(Combatant player, Combatant rival) {
        $$$setupUI$$$();
        this.player = player;
        buildBottomBar();
        this.moveLabels = new JLabel[]{move1Label, move2Label, move3Label, move4Label};

        field.setCombatants(player, rival);
        applyStyle();
        bindKeys();
        showPrompt();
        refreshMoves();
    }

    // El diseñador llama a este metodo para crear los paneles marcados "Custom Create"
    private void createUIComponents() {
        field = new BattleField();
        battlefieldPanel = field;
    }

    // Construye las tres cajas de abajo: texto, ataques (2x2) y PP/tipo
    private void buildBottomBar() {
        bottomPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.insets = new Insets(0, 6, 0, 6);

        promptPanel = new JPanel(new BorderLayout());
        promptLabel = new JLabel();
        promptPanel.add(promptLabel, BorderLayout.CENTER);
        gc.gridx = 0;
        gc.weightx = 0.28;
        bottomPanel.add(promptPanel, gc);

        movesPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        move1Label = new JLabel();
        move2Label = new JLabel();
        move3Label = new JLabel();
        move4Label = new JLabel();
        movesPanel.add(move1Label);
        movesPanel.add(move2Label);
        movesPanel.add(move3Label);
        movesPanel.add(move4Label);
        gc.gridx = 1;
        gc.weightx = 0.50;
        bottomPanel.add(movesPanel, gc);

        detailPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        ppLabel = new JLabel();
        typeLabel = new JLabel();
        detailPanel.add(ppLabel);
        detailPanel.add(typeLabel);
        gc.gridx = 2;
        gc.weightx = 0.22;
        bottomPanel.add(detailPanel, gc);
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    /**
     * Lo que pasa cuando el jugador confirma un ataque (recibe su posicion 0 a 3).
     */
    public void setOnMoveChosen(IntConsumer onMoveChosen) {
        this.onMoveChosen = onMoveChosen;
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }

    public void refresh() {
        field.refresh();
        refreshMoves();
    }

    public void showPrompt() {
        promptLabel.setText("<html>¿QUE HARA<br>" + player.getName().toUpperCase() + "?</html>");
    }

    public void showMessages(List<String> messages) {
        StringBuilder sb = new StringBuilder("<html>");
        for (String m : messages) {
            if (!m.isBlank()) {
                sb.append(m.toUpperCase()).append("<br>");
            }
        }
        promptLabel.setText(sb.append("</html>").toString());
    }

    private void applyStyle() {
        bottomPanel.setPreferredSize(new Dimension(960, 200));
        bottomPanel.setBackground(NAVY);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        for (JPanel p : new JPanel[]{promptPanel, movesPanel, detailPanel}) {
            p.setBackground(CREAM);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARK, 4),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        }

        promptLabel.setFont(PixelFont.get(11));
        promptLabel.setVerticalAlignment(SwingConstants.TOP);
        promptLabel.setForeground(DARK);
        for (JLabel l : moveLabels) {
            l.setFont(PixelFont.get(12));
            l.setForeground(DARK);
        }
        ppLabel.setFont(PixelFont.get(11));
        typeLabel.setFont(PixelFont.get(11));
        ppLabel.setForeground(DARK);
        typeLabel.setForeground(DARK);
    }

    private void bindKeys() {
        bind("UP", () -> moveSelection(-1, 0));
        bind("DOWN", () -> moveSelection(1, 0));
        bind("LEFT", () -> moveSelection(0, -1));
        bind("RIGHT", () -> moveSelection(0, 1));
        bind("ENTER", () -> {
            if (inputEnabled && selected < moveCount()) {
                onMoveChosen.accept(selected);
            }
        });
    }

    private void bind(String key, Runnable action) {
        rootPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), key);
        rootPanel.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private int moveCount() {
        return player.getPokemon().getMoves().size();
    }

    // Los ataques estan en cuadricula 2x2: se mueve por fila y columna
    private void moveSelection(int dRow, int dCol) {
        int row = Math.floorMod(selected / 2 + dRow, 2);
        int col = Math.floorMod(selected % 2 + dCol, 2);
        int target = row * 2 + col;
        if (target < moveCount()) {
            selected = target;
            refreshMoves();
        }
    }

    private void refreshMoves() {
        for (int i = 0; i < moveLabels.length; i++) {
            if (i < moveCount()) {
                String name = player.getPokemon().getMove(i).getName().toUpperCase();
                moveLabels[i].setText((i == selected ? "> " : "  ") + name);
            } else {
                moveLabels[i].setText("  -");
            }
        }
        if (selected < moveCount()) {
            Move move = player.getPokemon().getMove(selected);
            // El modelo aun no guarda los PP actuales, por ahora se muestran los maximos
            ppLabel.setText("PP " + move.getPp() + "/" + move.getPp());
            typeLabel.setText("TIPO/" + move.getType());
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(battlefieldPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(bottomPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
