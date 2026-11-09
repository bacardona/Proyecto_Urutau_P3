package com.pokemon.battle.ui;

import com.pokemon.battle.model.Player;
import com.pokemon.battle.service.TrainerCatalog;

import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Reusable screen to choose a registered trainer (player). It only shows the
 * list and tells who was chosen; what happens next is decided by whoever
 * creates it (see the history, start a battle...), so the same screen can be
 * used in every part of the game that needs a trainer.
 */
public class TrainerSelectPanel extends RetroScreen {

    private static final Rectangle HINT_BOX = new Rectangle(180, 72, 600, 56);
    private static final Rectangle LIST = new Rectangle(180, 140, 600, 440);
    private static final int ROW_H = 44;
    private static final int LIST_TOP = LIST.y + 12;
    private static final int VISIBLE_ROWS = (LIST.height - 24) / ROW_H;

    private final TrainerCatalog catalog;
    private final String hint;
    private final Consumer<Player> onSelected;
    private final Runnable onBack;

    private List<Player> players = new ArrayList<>();
    private int selected = 0;
    private int firstVisible = 0;
    private boolean loading = false;
    private String errorMessage;

    /**
     * @param catalog    where the trainers are read from (injected)
     * @param hint       short instruction shown above the list
     * @param onSelected what to do with the chosen trainer
     * @param onBack     what to do when the user leaves without choosing
     */
    public TrainerSelectPanel(TrainerCatalog catalog, String hint, Consumer<Player> onSelected, Runnable onBack) {
        super("SELECCIONAR ENTRENADOR");
        this.catalog = catalog;
        this.hint = hint;
        this.onSelected = onSelected;
        this.onBack = onBack;
        bindKey("UP", () -> moveSelection(-1));
        bindKey("DOWN", () -> moveSelection(1));
        bindKey("ENTER", this::choose);
        // Se recarga cada vez que se muestra, porque despues de una batalla cambian las victorias
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadPlayers();
            }
        });
    }

    /**
     * Reads the trainers again in the background.
     */
    public void loadPlayers() {
        if (loading) {
            return;
        }
        loading = true;
        errorMessage = null;
        repaint();
        new SwingWorker<List<Player>, Void>() {
            @Override
            protected List<Player> doInBackground() {
                List<Player> all = new ArrayList<>(catalog.listPlayers());
                all.sort(Comparator.comparing(p -> p.getName().toUpperCase()));
                return all;
            }

            @Override
            protected void done() {
                loading = false;
                try {
                    players = get();
                    selected = Math.min(selected, Math.max(0, players.size() - 1));
                    keepVisible();
                } catch (InterruptedException | ExecutionException e) {
                    errorMessage = "NO SE PUDIERON CARGAR LOS ENTRENADORES";
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.err.println("Seleccion de entrenador: " + cause);
                }
                repaint();
            }
        }.execute();
    }

    /**
     * Moves the selection a number of rows up (negative) or down (positive).
     */
    public void moveSelection(int delta) {
        if (players.isEmpty()) {
            return;
        }
        selected = Math.max(0, Math.min(players.size() - 1, selected + delta));
        keepVisible();
        repaint();
    }

    private void keepVisible() {
        if (selected < firstVisible) {
            firstVisible = selected;
        } else if (selected >= firstVisible + VISIBLE_ROWS) {
            firstVisible = selected - VISIBLE_ROWS + 1;
        }
    }

    private void choose() {
        if (!loading && !players.isEmpty()) {
            onSelected.accept(players.get(selected));
        }
    }

    @Override
    protected void goBack() {
        onBack.run();
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        if (LIST.contains(x, y) && y >= LIST_TOP) {
            int index = firstVisible + (y - LIST_TOP) / ROW_H;
            if (index < players.size()) {
                selected = index;
                repaint();
                // Doble clic = Enter
                if (clickCount >= 2) {
                    choose();
                }
            }
        }
    }

    @Override
    protected void onWheel(int rotation) {
        moveSelection(rotation);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void paintFooterButtons(Graphics2D g) {
        super.paintFooterButtons(g);
        RetroStyle.drawArrowKeyButton(g, true, "MOVER", 214, 598, 170, 36);
        RetroStyle.drawKeyButton(g, "ENTER  ELEGIR", 404, 598, 190, 36);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        RetroStyle.drawRedFrame(g, HINT_BOX.x, HINT_BOX.y, HINT_BOX.width, HINT_BOX.height);
        g.setFont(RetroStyle.font(26));
        int hw = g.getFontMetrics().stringWidth(hint);
        RetroStyle.shadowText(g, hint, HINT_BOX.x + (HINT_BOX.width - hw) / 2, HINT_BOX.y + 37, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        if (loading) {
            drawMessage(g, "CARGANDO ENTRENADORES...");
        } else if (errorMessage != null) {
            drawMessage(g, errorMessage);
        } else if (players.isEmpty()) {
            drawMessage(g, "NO HAY ENTRENADORES REGISTRADOS");
        } else {
            drawList(g);
        }
    }

    private void drawList(Graphics2D g) {
        RetroStyle.drawListFrame(g, LIST.x, LIST.y, LIST.width, LIST.height);
        g.setColor(RetroStyle.DARK);
        int last = Math.min(players.size(), firstVisible + VISIBLE_ROWS);
        for (int i = firstVisible; i < last; i++) {
            int y = LIST_TOP + (i - firstVisible) * ROW_H;
            boolean isSelected = i == selected;
            if (isSelected) {
                RetroStyle.drawSelectedRow(g, LIST.x + 22, y - 3, LIST.width - 44, ROW_H + 6);
            }
            Player p = players.get(i);
            Color main = isSelected ? RetroStyle.RED_DARK : RetroStyle.DARK;
            RetroStyle.drawListDot(g, LIST.x + 46, y + ROW_H / 2, isSelected);
            g.setFont(RetroStyle.font(28));
            RetroStyle.shadowText(g, p.getName().toUpperCase(), LIST.x + 74, y + 31, main, RetroStyle.SHADOW_LIGHT);

            // Victorias y derrotas a la derecha de cada fila
            g.setFont(RetroStyle.font(22));
            String record = "V " + p.getBattlesWon() + "   D " + p.getBattlesLost();
            int rw = g.getFontMetrics().stringWidth(record);
            RetroStyle.shadowText(g, record, LIST.x + LIST.width - 44 - rw, y + 30, main, RetroStyle.SHADOW_LIGHT);
        }
        RetroStyle.drawScrollBar(g, LIST.x + LIST.width - 16, LIST.y + 14, LIST.height - 28,
                firstVisible, VISIBLE_ROWS, players.size());
    }
}
