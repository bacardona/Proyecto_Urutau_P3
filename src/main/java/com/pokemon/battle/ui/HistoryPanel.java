package com.pokemon.battle.ui;

import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.CombatResult;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.service.CombatRegistry;

import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Combat history of one trainer: a summary with wins and losses, and
 * a table with every combat (number, date, rival and result), newest first.
 */
public class HistoryPanel extends RetroScreen {

    private static final Rectangle NAME_PLATE = new Rectangle(20, 72, 920, 64);
    private static final Rectangle TABLE = new Rectangle(20, 148, 920, 432);
    private static final int ROW_H = 40;
    private static final int TABLE_TOP = TABLE.y + 54;
    private static final int VISIBLE_ROWS = (TABLE.y + TABLE.height - 12 - TABLE_TOP) / ROW_H;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

    // Columnas de la tabla (posicion x dentro del tablero)
    private static final int COL_NUMBER = TABLE.x + 30;
    private static final int COL_DATE = TABLE.x + 170;
    private static final int COL_RIVAL = TABLE.x + 450;
    private static final int COL_RESULT = TABLE.x + 720;

    private final CombatRegistry registry;
    private final Runnable onBack;

    private Player player;
    private List<CombatHistoryEntry> entries = new ArrayList<>();
    private int selected = 0;
    private int firstVisible = 0;
    private boolean loading = false;
    private String errorMessage;

    /**
     * @param registry where the combats are read from (injected)
     * @param onBack   what to do when the user leaves, for example go back to the trainer list
     */
    public HistoryPanel(CombatRegistry registry, Runnable onBack) {
        super("HISTORIAL DE COMBATES");
        this.registry = registry;
        this.onBack = onBack;
        bindKey("UP", () -> moveSelection(-1));
        bindKey("DOWN", () -> moveSelection(1));
        bindKey("LEFT", () -> moveSelection(-VISIBLE_ROWS));
        bindKey("RIGHT", () -> moveSelection(VISIBLE_ROWS));
    }

    /**
     * Shows the history of a trainer, reading it in the background.
     *
     * @param trainer the chosen trainer
     */
    public void showPlayer(Player trainer) {
        this.player = trainer;
        this.entries = new ArrayList<>();
        this.selected = 0;
        this.firstVisible = 0;
        this.loading = true;
        this.errorMessage = null;
        repaint();
        new SwingWorker<List<CombatHistoryEntry>, Void>() {
            @Override
            protected List<CombatHistoryEntry> doInBackground() {
                List<CombatHistoryEntry> all = new ArrayList<>(registry.historyOf(trainer));
                // El combate mas reciente arriba
                all.sort(Comparator.comparing(CombatHistoryEntry::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                return all;
            }

            @Override
            protected void done() {
                // Si mientras cargaba se eligio otro entrenador, este resultado ya no sirve
                if (trainer != player) {
                    return;
                }
                loading = false;
                try {
                    entries = get();
                } catch (InterruptedException | ExecutionException e) {
                    errorMessage = "NO SE PUDO CARGAR EL HISTORIAL";
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.err.println("Historial: " + cause);
                }
                repaint();
            }
        }.execute();
    }

    /**
     * Moves the highlighted row a number of rows up (negative) or down (positive).
     */
    public void moveSelection(int delta) {
        if (entries.isEmpty()) {
            return;
        }
        selected = Math.max(0, Math.min(entries.size() - 1, selected + delta));
        if (selected < firstVisible) {
            firstVisible = selected;
        } else if (selected >= firstVisible + VISIBLE_ROWS) {
            firstVisible = selected - VISIBLE_ROWS + 1;
        }
        repaint();
    }

    @Override
    protected void goBack() {
        onBack.run();
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        if (TABLE.contains(x, y) && y >= TABLE_TOP) {
            int index = firstVisible + (y - TABLE_TOP) / ROW_H;
            if (index < entries.size()) {
                selected = index;
                repaint();
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
        RetroStyle.drawArrowKeyButton(g, false, "PAGINA", 404, 598, 170, 36);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        if (player == null) {
            return;
        }
        drawNamePlate(g);
        if (loading) {
            drawMessage(g, "CARGANDO HISTORIAL...");
        } else if (errorMessage != null) {
            drawMessage(g, errorMessage);
        } else {
            drawTable(g);
        }
    }

    private void drawNamePlate(Graphics2D g) {
        Rectangle r = NAME_PLATE;
        RetroStyle.drawRedFrame(g, r.x, r.y, r.width, r.height);
        g.setFont(RetroStyle.font(34));
        RetroStyle.shadowText(g, player.getName().toUpperCase(), r.x + 24, r.y + 43, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        int won = player.getBattlesWon();
        int lost = player.getBattlesLost();

        // Resumen a la derecha: victorias y derrotas
        g.setFont(RetroStyle.font(22));
        int x = r.x + r.width - 24;
        x = drawChip(g, "DERROTAS " + lost, x, r.y + 16, new Color(0xE0603A), new Color(0xA8361A));
        drawChip(g, "VICTORIAS " + won, x - 10, r.y + 16, new Color(0x3DAE5A), new Color(0x23793A));
    }

    // Dibuja una etiqueta de color terminando en rightX y devuelve donde empieza
    private int drawChip(Graphics2D g, String text, int rightX, int y, Color fill, Color border) {
        int w = g.getFontMetrics().stringWidth(text) + 24;
        int x = rightX - w;
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(x, y, w, 32, 10, 10);
        g.setColor(border);
        g.fillRoundRect(x + 2, y + 2, w - 4, 28, 8, 8);
        g.setColor(fill);
        g.fillRoundRect(x + 4, y + 4, w - 8, 22, 6, 6);
        RetroStyle.shadowText(g, text, x + 12, y + 23, RetroStyle.WHITE, RetroStyle.OUTLINE);
        return x;
    }

    private void drawTable(Graphics2D g) {
        Rectangle r = TABLE;
        RetroStyle.drawPanel(g, r.x, r.y, r.width, r.height);

        g.setFont(RetroStyle.font(24));
        int headerY = r.y + 36;
        RetroStyle.shadowText(g, "N.", COL_NUMBER, headerY, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);
        RetroStyle.shadowText(g, "FECHA", COL_DATE, headerY, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);
        RetroStyle.shadowText(g, "RIVAL", COL_RIVAL, headerY, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);
        RetroStyle.shadowText(g, "RESULTADO", COL_RESULT, headerY, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);
        g.setColor(new Color(0xE3DDC4));
        g.fillRect(r.x + 14, r.y + 46, r.width - 28, 2);

        if (entries.isEmpty()) {
            g.setFont(RetroStyle.font(28));
            String text = "TODAVIA NO TIENE COMBATES";
            int w = g.getFontMetrics().stringWidth(text);
            RetroStyle.shadowText(g, text, r.x + (r.width - w) / 2, r.y + 220, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            return;
        }

        int last = Math.min(entries.size(), firstVisible + VISIBLE_ROWS);
        for (int i = firstVisible; i < last; i++) {
            int y = TABLE_TOP + (i - firstVisible) * ROW_H;
            CombatHistoryEntry entry = entries.get(i);
            boolean isSelected = i == selected;
            // Filas alternadas y la seleccionada resaltada
            if (isSelected) {
                RetroStyle.drawSelectedRow(g, r.x + 8, y, r.width - 34, ROW_H);
            } else {
                g.setColor(i % 2 == 0 ? RetroStyle.WHITE : new Color(0xEEEBE2));
                g.fillRect(r.x + 10, y + 2, r.width - 38, ROW_H - 4);
            }
            Color main = isSelected ? RetroStyle.RED_DARK : RetroStyle.DARK;
            int baseline = y + 28;
            g.setFont(RetroStyle.font(24));
            RetroStyle.shadowText(g, String.format("%03d", entry.getCombatId()), COL_NUMBER, baseline, main, RetroStyle.SHADOW_LIGHT);
            String date = entry.getDate() == null ? "-" : entry.getDate().format(DATE);
            RetroStyle.shadowText(g, date, COL_DATE, baseline, main, RetroStyle.SHADOW_LIGHT);
            RetroStyle.shadowText(g, entry.getRivalName().toUpperCase(), COL_RIVAL, baseline, main, RetroStyle.SHADOW_LIGHT);
            drawResult(g, entry.getResult(), COL_RESULT, y + 6);
        }
        RetroStyle.drawScrollBar(g, r.x + r.width - 18, TABLE_TOP, VISIBLE_ROWS * ROW_H,
                firstVisible, VISIBLE_ROWS, entries.size());
    }

    // VICTORIA en verde y DERROTA en rojo
    private void drawResult(Graphics2D g, String result, int x, int y) {
        boolean win = CombatResult.WIN.getDbValue().equalsIgnoreCase(result);
        g.setFont(RetroStyle.font(20));
        String text = win ? "VICTORIA" : "DERROTA";
        Color fill = win ? new Color(0x3DAE5A) : new Color(0xE0603A);
        Color border = win ? new Color(0x23793A) : new Color(0xA8361A);
        int w = 120;
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(x, y, w, 28, 10, 10);
        g.setColor(border);
        g.fillRoundRect(x + 2, y + 2, w - 4, 24, 8, 8);
        g.setColor(fill);
        g.fillRoundRect(x + 4, y + 4, w - 8, 18, 6, 6);
        int tw = g.getFontMetrics().stringWidth(text);
        RetroStyle.shadowText(g, text, x + (w - tw) / 2, y + 21, RetroStyle.WHITE, RetroStyle.OUTLINE);
    }
}
