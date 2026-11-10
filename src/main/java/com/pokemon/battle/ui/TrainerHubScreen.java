package com.pokemon.battle.ui;

import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.CombatResult;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Team;
import com.pokemon.battle.service.BattleController;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

/**
 * Pantalla de entrenador para PvE y PvP: un encabezado y tres columnas.
 * 1) lista de entrenadores, 2) resumen de sus combates, 3) crear el entrenador
 * (nombre) o armar su equipo de 3 Pokemon.
 */
public class TrainerHubScreen extends RetroScreen {

    private static final String NEW_ROW_TEXT = "+ NUEVO ENTRENADOR";
    private static final int MAX_NAME = 14;
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yy");

    private static final int COL_Y = 140;
    private static final int COL_W = 298;
    private static final int COL_H = 440;
    private static final Rectangle HEADER = new Rectangle(20, 72, 920, 56);
    private static final Rectangle COL1 = new Rectangle(20, COL_Y, COL_W, COL_H);
    private static final Rectangle COL2 = new Rectangle(331, COL_Y, COL_W, COL_H);
    private static final Rectangle COL3 = new Rectangle(642, COL_Y, COL_W, COL_H);

    // Columna 1: lista de entrenadores
    private static final int ROW_H = 40;
    private static final int LIST_TOP = COL1.y + 12;
    private static final int LIST_ROWS = (COL1.height - 24) / ROW_H;

    // Columna 2: resumen
    private static final int SUMMARY_TOP = COL2.y + 136;
    private static final int SUMMARY_ROW_H = 36;
    private static final int SUMMARY_ROWS = 7;

    // Columna 3: equipo
    private static final int SLOT_H = 30;
    private static final int SLOTS_TOP = COL3.y + 46;
    private static final int POKE_ROW_H = 32;
    private static final int POKE_TOP = COL3.y + 150;
    private static final int POKE_ROWS = 6;
    private static final int BTN_Y = COL3.y + 384;
    private static final Rectangle ADD_BUTTON = new Rectangle(COL3.x + 10, BTN_Y, 88, 34);
    private static final Rectangle REMOVE_BUTTON = new Rectangle(COL3.x + 105, BTN_Y, 88, 34);
    private static final Rectangle DONE_BUTTON = new Rectangle(COL3.x + 200, BTN_Y, 88, 34);

    // Columna 3: nombre
    private static final Rectangle NAME_BOX = new Rectangle(COL3.x + 16, COL3.y + 104, COL_W - 32, 44);
    private static final Rectangle CREATE_BUTTON = new Rectangle(COL3.x + 16, COL3.y + 210, COL_W - 32, 40);

    private final BattleController controller;
    private final String header;
    private final BiConsumer<Player, List<Pokemon>> onReady;
    private final Runnable onBack;
    private final Timer blinkTimer;

    private final List<Player> allPlayers = new ArrayList<>();
    private final List<Player> shown = new ArrayList<>();
    private final Map<String, List<CombatHistoryEntry>> historyCache = new HashMap<>();
    private final List<Pokemon> chosen = new ArrayList<>();
    private final StringBuilder nameText = new StringBuilder();
    private List<Pokemon> catalog = new ArrayList<>();
    private Player excluded;

    private int selectedRow = -1; // 0 = "+ NUEVO ENTRENADOR"
    private int firstVisible = 0;
    private int pokeSelected = 0;
    private int pokeFirst = 0;
    private boolean sideFocus = false; // true = el cursor esta en la lista de Pokemon
    private boolean cursorOn = true;
    private boolean busy = false;
    private String message = "";

    /**
     * @param title   texto de la barra azul
     * @param header  texto del encabezado
     * @param onReady que hacer cuando hay entrenador y equipo de 3
     * @param onBack  que hacer al salir de la pantalla
     */
    public TrainerHubScreen(String title, String header, BattleController controller,
                            BiConsumer<Player, List<Pokemon>> onReady, Runnable onBack) {
        super(title);
        this.header = header;
        this.controller = controller;
        this.onReady = onReady;
        this.onBack = onBack;
        setFocusable(true);

        blinkTimer = new Timer(500, e -> {
            cursorOn = !cursorOn;
            repaint();
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                blinkTimer.start();
                SwingUtilities.invokeLater(TrainerHubScreen.this::requestFocusInWindow);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                blinkTimer.stop();
            }
        });

        bindKey("UP", () -> move(-1));
        bindKey("DOWN", () -> move(1));
        bindKey("LEFT", () -> {
            sideFocus = false;
            repaint();
        });
        bindKey("RIGHT", this::focusSide);
        bindKey("ENTER", this::enter);
        // Las letras que se escriben (y el retroceso) llegan por aqui
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                typed(e.getKeyChar());
            }
        });
    }

    /**
     * Carga los datos y deja la pantalla lista para mostrarse.
     *
     * @param players  entrenadores existentes
     * @param pokemon  catalogo de Pokemon
     * @param excluded entrenador que no se puede elegir (el 1 en PvP), o null
     */
    public void prepare(List<Player> players, List<Pokemon> pokemon, Player excluded) {
        allPlayers.clear();
        allPlayers.addAll(players);
        catalog = new ArrayList<>(pokemon);
        catalog.sort(Comparator.comparingInt(Pokemon::getId));
        this.excluded = excluded;
        historyCache.clear();
        nameText.setLength(0);
        busy = false;
        rebuildShown();
        selectedRow = -1;
        firstVisible = 0;
        selectRow(0);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
        repaint();
    }

    // ------------------------------------------------------------------ datos

    private void rebuildShown() {
        shown.clear();
        for (Player p : allPlayers) {
            if (excluded == null || !sameId(p, excluded)) {
                shown.add(p);
            }
        }
        shown.sort(Comparator.comparing(p -> p.getName().toUpperCase()));
    }

    private boolean sameId(Player a, Player b) {
        return String.valueOf(a.getId()).equals(String.valueOf(b.getId()));
    }

    private Player current() {
        return selectedRow > 0 && selectedRow <= shown.size() ? shown.get(selectedRow - 1) : null;
    }

    private boolean nameMode() {
        return selectedRow <= 0;
    }

    private void selectRow(int row) {
        int target = Math.max(0, Math.min(shown.size(), row));
        if (target == selectedRow) {
            return;
        }
        selectedRow = target;
        if (selectedRow < firstVisible) {
            firstVisible = selectedRow;
        } else if (selectedRow >= firstVisible + LIST_ROWS) {
            firstVisible = selectedRow - LIST_ROWS + 1;
        }
        chosen.clear();
        pokeSelected = 0;
        pokeFirst = 0;
        sideFocus = false;
        message = "";
        if (selectedRow > 0) {
            requestHistory(shown.get(selectedRow - 1));
        }
        repaint();
    }

    // Lee los combates del entrenador en segundo plano (null en el cache = cargando)
    private void requestHistory(Player p) {
        String key = String.valueOf(p.getId());
        if (historyCache.containsKey(key)) {
            return;
        }
        historyCache.put(key, null);
        new SwingWorker<List<CombatHistoryEntry>, Void>() {
            @Override
            protected List<CombatHistoryEntry> doInBackground() {
                List<CombatHistoryEntry> all = new ArrayList<>(controller.getHistory(p));
                all.sort(Comparator.comparing(CombatHistoryEntry::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                return all;
            }

            @Override
            protected void done() {
                try {
                    historyCache.put(key, get());
                } catch (InterruptedException | ExecutionException e) {
                    historyCache.put(key, new ArrayList<>());
                    System.err.println("Hub (historial): " + e);
                }
                repaint();
            }
        }.execute();
    }

    // ------------------------------------------------------------------ acciones

    private void move(int delta) {
        if (busy) {
            return;
        }
        if (sideFocus && !nameMode()) {
            pokeSelected = Math.max(0, Math.min(catalog.size() - 1, pokeSelected + delta));
            if (pokeSelected < pokeFirst) {
                pokeFirst = pokeSelected;
            } else if (pokeSelected >= pokeFirst + POKE_ROWS) {
                pokeFirst = pokeSelected - POKE_ROWS + 1;
            }
        } else {
            selectRow(selectedRow + delta);
        }
        repaint();
    }

    private void focusSide() {
        if (!nameMode()) {
            sideFocus = true;
            repaint();
        }
    }

    private void enter() {
        if (busy) {
            return;
        }
        if (nameMode()) {
            createTrainer();
        } else if (!sideFocus) {
            sideFocus = true;
            repaint();
        } else if (chosen.size() >= Team.SIZE) {
            confirm();
        } else {
            addSelected();
        }
    }

    private void typed(char c) {
        if (busy) {
            return;
        }
        if (nameMode()) {
            if (c == '\b') {
                if (nameText.length() > 0) {
                    nameText.setLength(nameText.length() - 1);
                }
            } else if ((Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-')
                    && nameText.length() < MAX_NAME) {
                nameText.append(c);
            } else {
                return;
            }
            message = "";
            cursorOn = true;
            repaint();
        } else if (c == '\b') {
            removeLast();
        }
    }

    private void addSelected() {
        if (catalog.isEmpty()) {
            return;
        }
        Pokemon p = catalog.get(pokeSelected);
        if (chosen.size() >= Team.SIZE) {
            message = "EQUIPO COMPLETO: PRESIONA LISTO";
        } else if (chosen.contains(p)) {
            message = "YA ESTA EN EL EQUIPO";
        } else {
            chosen.add(p);
            message = "";
        }
        repaint();
    }

    private void removeLast() {
        if (!chosen.isEmpty()) {
            chosen.remove(chosen.size() - 1);
            message = "";
            repaint();
        }
    }

    private void confirm() {
        Player p = current();
        if (p == null) {
            return;
        }
        if (chosen.size() != Team.SIZE) {
            message = "ELIGE " + Team.SIZE + " POKEMON";
            repaint();
            return;
        }
        onReady.accept(p, new ArrayList<>(chosen));
    }

    // Si el nombre ya existe lo selecciona; si no, lo crea en segundo plano
    private void createTrainer() {
        String name = nameText.toString().trim();
        if (name.isEmpty()) {
            message = "ESCRIBE UN NOMBRE";
            repaint();
            return;
        }
        for (Player p : allPlayers) {
            if (p.getName().equalsIgnoreCase(name)) {
                if (excluded != null && sameId(p, excluded)) {
                    message = "ESE ENTRENADOR YA FUE ELEGIDO. USA OTRO NOMBRE";
                } else {
                    nameText.setLength(0);
                    selectRow(shown.indexOf(p) + 1);
                    sideFocus = true;
                    message = "YA EXISTIA: ARMA SU EQUIPO";
                }
                repaint();
                return;
            }
        }
        busy = true;
        message = "CREANDO ENTRENADOR...";
        repaint();
        new SwingWorker<Player, Void>() {
            @Override
            protected Player doInBackground() {
                return controller.createPlayer(name);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    Player created = get();
                    allPlayers.add(created);
                    rebuildShown();
                    nameText.setLength(0);
                    selectRow(shown.indexOf(created) + 1);
                    sideFocus = true;
                    message = "";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    message = "OPERACION CANCELADA";
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    message = cause.getMessage() == null
                            ? "NO SE PUDO CREAR EL ENTRENADOR" : cause.getMessage().toUpperCase();
                }
                repaint();
            }
        }.execute();
    }

    @Override
    protected void goBack() {
        if (sideFocus && !nameMode()) {
            sideFocus = false;
            repaint();
        } else {
            onBack.run();
        }
    }

    // ------------------------------------------------------------------ mouse

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        requestFocusInWindow();
        if (busy) {
            return;
        }
        if (COL1.contains(x, y) && y >= LIST_TOP) {
            int row = firstVisible + (y - LIST_TOP) / ROW_H;
            if (row <= shown.size()) {
                selectRow(row);
                if (clickCount >= 2 && !nameMode()) {
                    sideFocus = true;
                }
                repaint();
            }
            return;
        }
        if (nameMode()) {
            if (CREATE_BUTTON.contains(x, y)) {
                createTrainer();
            }
            return;
        }
        if (ADD_BUTTON.contains(x, y)) {
            sideFocus = true;
            addSelected();
        } else if (REMOVE_BUTTON.contains(x, y)) {
            removeLast();
        } else if (DONE_BUTTON.contains(x, y)) {
            confirm();
        } else if (x >= COL3.x && x < COL3.x + COL3.width
                && y >= POKE_TOP && y < POKE_TOP + POKE_ROWS * POKE_ROW_H) {
            int index = pokeFirst + (y - POKE_TOP) / POKE_ROW_H;
            if (index < catalog.size()) {
                pokeSelected = index;
                sideFocus = true;
                if (clickCount >= 2) {
                    addSelected();
                }
                repaint();
            }
        }
    }

    @Override
    protected void onWheel(int rotation) {
        move(rotation);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void paintFooterButtons(Graphics2D g) {
        super.paintFooterButtons(g);
        RetroStyle.drawArrowKeyButton(g, true, "MOVER", 214, 598, 150, 36);
        if (nameMode()) {
            RetroStyle.drawKeyButton(g, "ENTER  CREAR", 374, 598, 190, 36);
        } else {
            RetroStyle.drawArrowKeyButton(g, false, "COLUMNA", 374, 598, 170, 36);
            String enter = !sideFocus ? "ENTER  ELEGIR"
                    : (chosen.size() >= Team.SIZE ? "ENTER  LISTO" : "ENTER  AGREGAR");
            RetroStyle.drawKeyButton(g, enter, 554, 598, 190, 36);
            RetroStyle.drawKeyButton(g, "BORRAR  QUITAR", 754, 598, 190, 36);
        }
    }

    @Override
    protected void paintContent(Graphics2D g) {
        drawHeaderPlate(g);
        drawTrainerList(g);
        drawSummary(g);
        if (nameMode()) {
            drawNameEntry(g);
        } else {
            drawTeamBuilder(g);
        }
        drawFocusOutline(g);
    }

    private void drawHeaderPlate(Graphics2D g) {
        RetroStyle.drawRedFrame(g, HEADER.x, HEADER.y, HEADER.width, HEADER.height);
        g.setFont(RetroStyle.font(28));
        int w = g.getFontMetrics().stringWidth(header);
        RetroStyle.shadowText(g, header, HEADER.x + (HEADER.width - w) / 2, HEADER.y + 37,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
    }

    // ----- columna 1

    private void drawTrainerList(Graphics2D g) {
        RetroStyle.drawListFrame(g, COL1.x, COL1.y, COL1.width, COL1.height);
        g.setFont(RetroStyle.font(24));
        FontMetrics fm = g.getFontMetrics();
        int last = Math.min(shown.size() + 1, firstVisible + LIST_ROWS);
        for (int row = firstVisible; row < last; row++) {
            int y = LIST_TOP + (row - firstVisible) * ROW_H;
            boolean selected = row == selectedRow;
            if (selected) {
                RetroStyle.drawSelectedRow(g, COL1.x + 22, y - 3, COL1.width - 44, ROW_H + 6);
            }
            RetroStyle.drawListDot(g, COL1.x + 46, y + ROW_H / 2, selected);
            String text = row == 0 ? NEW_ROW_TEXT : shown.get(row - 1).getName().toUpperCase();
            text = RetroText.fit(fm, text, COL1.width - 62 - 34);
            RetroStyle.shadowText(g, text, COL1.x + 62, y + 28,
                    selected ? RetroStyle.RED_DARK : RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
        RetroStyle.drawScrollBar(g, COL1.x + COL1.width - 16, COL1.y + 14, COL1.height - 28,
                firstVisible, LIST_ROWS, shown.size() + 1);
    }

    // ----- columna 2

    private void drawSummary(Graphics2D g) {
        RetroStyle.drawPanel(g, COL2.x, COL2.y, COL2.width, COL2.height);
        g.setFont(RetroStyle.font(26));
        RetroStyle.shadowText(g, "RESUMEN", COL2.x + 16, COL2.y + 34, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);

        Player p = current();
        if (p == null) {
            g.setFont(RetroStyle.font(22));
            List<String> lines = RetroText.wrap(g.getFontMetrics(),
                    "SELECCIONA UN ENTRENADOR DE LA LISTA PARA VER SUS COMBATES, "
                            + "O ESCRIBE UN NOMBRE PARA CREAR UNO NUEVO.", COL2.width - 32);
            for (int i = 0; i < lines.size(); i++) {
                RetroStyle.shadowText(g, lines.get(i), COL2.x + 16, COL2.y + 80 + i * 28,
                        RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            }
            return;
        }

        g.setFont(RetroStyle.font(30));
        String name = RetroText.fit(g.getFontMetrics(), p.getName().toUpperCase(), COL2.width - 32);
        RetroStyle.shadowText(g, name, COL2.x + 16, COL2.y + 74, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        g.setFont(RetroStyle.font(22));
        int next = drawChip(g, "V " + p.getBattlesWon(), COL2.x + 16, COL2.y + 86,
                new Color(0x3DAE5A), new Color(0x23793A));
        drawChip(g, "D " + p.getBattlesLost(), next + 8, COL2.y + 86,
                new Color(0xE0603A), new Color(0xA8361A));

        g.setColor(new Color(0xE3DDC4));
        g.fillRect(COL2.x + 12, COL2.y + 126, COL2.width - 24, 2);

        List<CombatHistoryEntry> entries = historyCache.get(String.valueOf(p.getId()));
        if (entries == null) {
            g.setFont(RetroStyle.font(24));
            drawCentered(g, "CARGANDO...", COL2, SUMMARY_TOP + 70);
            return;
        }
        if (entries.isEmpty()) {
            g.setFont(RetroStyle.font(24));
            drawCentered(g, "SIN COMBATES AUN", COL2, SUMMARY_TOP + 70);
            return;
        }

        g.setFont(RetroStyle.font(20));
        FontMetrics fm = g.getFontMetrics();
        int rows = Math.min(entries.size(), SUMMARY_ROWS);
        for (int i = 0; i < rows; i++) {
            CombatHistoryEntry entry = entries.get(i);
            int y = SUMMARY_TOP + i * SUMMARY_ROW_H;
            g.setColor(i % 2 == 0 ? RetroStyle.WHITE : new Color(0xEEEBE2));
            g.fillRect(COL2.x + 10, y, COL2.width - 20, SUMMARY_ROW_H - 2);

            boolean win = CombatResult.WIN.getDbValue().equalsIgnoreCase(entry.getResult());
            drawMiniResult(g, win, COL2.x + 16, y + 3);

            String date = entry.getDate() == null ? "-" : entry.getDate().format(SHORT_DATE);
            int dateW = fm.stringWidth(date);
            String rival = entry.getRivalName() == null ? "?" : entry.getRivalName().toUpperCase();
            rival = RetroText.fit(fm, rival, COL2.width - 74 - dateW);
            RetroStyle.shadowText(g, rival, COL2.x + 50, y + 24, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            RetroStyle.shadowText(g, date, COL2.x + COL2.width - 16 - dateW, y + 24,
                    RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
        if (entries.size() > rows) {
            drawCentered(g, "+ " + (entries.size() - rows) + " MAS EN HISTORIAL", COL2,
                    SUMMARY_TOP + SUMMARY_ROWS * SUMMARY_ROW_H + 22);
        }
    }

    private int drawChip(Graphics2D g, String text, int x, int y, Color fill, Color border) {
        int w = g.getFontMetrics().stringWidth(text) + 24;
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(x, y, w, 30, 10, 10);
        g.setColor(border);
        g.fillRoundRect(x + 2, y + 2, w - 4, 26, 8, 8);
        g.setColor(fill);
        g.fillRoundRect(x + 4, y + 4, w - 8, 20, 6, 6);
        RetroStyle.shadowText(g, text, x + 12, y + 23, RetroStyle.WHITE, RetroStyle.OUTLINE);
        return x + w;
    }

    private void drawMiniResult(Graphics2D g, boolean win, int x, int y) {
        Color fill = win ? new Color(0x3DAE5A) : new Color(0xE0603A);
        Color border = win ? new Color(0x23793A) : new Color(0xA8361A);
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(x, y, 28, 26, 8, 8);
        g.setColor(border);
        g.fillRoundRect(x + 2, y + 2, 24, 22, 6, 6);
        g.setColor(fill);
        g.fillRoundRect(x + 3, y + 3, 22, 18, 5, 5);
        String letter = win ? "V" : "D";
        int w = g.getFontMetrics().stringWidth(letter);
        RetroStyle.shadowText(g, letter, x + (28 - w) / 2, y + 20, RetroStyle.WHITE, RetroStyle.OUTLINE);
    }

    private void drawCentered(Graphics2D g, String text, Rectangle area, int baseline) {
        int w = g.getFontMetrics().stringWidth(text);
        RetroStyle.shadowText(g, text, area.x + (area.width - w) / 2, baseline,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
    }

    // ----- columna 3: nombre

    private void drawNameEntry(Graphics2D g) {
        RetroStyle.drawPanel(g, COL3.x, COL3.y, COL3.width, COL3.height);
        g.setFont(RetroStyle.font(26));
        RetroStyle.shadowText(g, "NUEVO ENTRENADOR", COL3.x + 16, COL3.y + 34,
                RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);
        g.setFont(RetroStyle.font(22));
        RetroStyle.shadowText(g, "ESCRIBE EL NOMBRE:", COL3.x + 16, COL3.y + 90,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        Rectangle b = NAME_BOX;
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(b.x, b.y, b.width, b.height, 10, 10);
        g.setColor(RetroStyle.BLUE);
        g.fillRoundRect(b.x + 2, b.y + 2, b.width - 4, b.height - 4, 8, 8);
        g.setColor(RetroStyle.WHITE);
        g.fillRoundRect(b.x + 5, b.y + 5, b.width - 10, b.height - 10, 6, 6);

        g.setFont(RetroStyle.font(28));
        String text = nameText.toString();
        RetroStyle.shadowText(g, text, b.x + 12, b.y + 32, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        if (cursorOn && !busy) {
            int cx = b.x + 12 + g.getFontMetrics().stringWidth(text) + 3;
            g.setColor(RetroStyle.RED);
            g.fillRect(cx, b.y + 10, 3, 26);
        }

        drawNote(g, COL3.x + 16, COL3.y + 172, COL_W - 32, 2);

        g.setFont(RetroStyle.font(24));
        RetroStyle.drawKeyButton(g, "CREAR  (ENTER)", CREATE_BUTTON.x, CREATE_BUTTON.y,
                CREATE_BUTTON.width, CREATE_BUTTON.height);

        g.setFont(RetroStyle.font(20));
        List<String> hint = RetroText.wrap(g.getFontMetrics(),
                "AL CREARLO, ARMARAS SU EQUIPO DE " + Team.SIZE + " POKEMON.", COL_W - 32);
        for (int i = 0; i < hint.size(); i++) {
            RetroStyle.shadowText(g, hint.get(i), COL3.x + 16, COL3.y + 292 + i * 24,
                    RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
    }

    // ----- columna 3: equipo

    private void drawTeamBuilder(Graphics2D g) {
        Player p = current();
        RetroStyle.drawPanel(g, COL3.x, COL3.y, COL3.width, COL3.height);
        g.setFont(RetroStyle.font(26));
        String title = RetroText.fit(g.getFontMetrics(),
                "EQUIPO: " + (p == null ? "" : p.getName().toUpperCase()), COL3.width - 32);
        RetroStyle.shadowText(g, title, COL3.x + 16, COL3.y + 34, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);

        // Casillas del equipo
        g.setFont(RetroStyle.font(22));
        for (int i = 0; i < Team.SIZE; i++) {
            int y = SLOTS_TOP + i * SLOT_H;
            g.setColor(i % 2 == 0 ? RetroStyle.WHITE : new Color(0xEEEBE2));
            g.fillRect(COL3.x + 10, y, COL3.width - 20, SLOT_H);
            String name = i < chosen.size() ? chosen.get(i).getName().toUpperCase() : "---";
            RetroStyle.shadowText(g, (i + 1) + ". " + name, COL3.x + 18, y + 22,
                    RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
        g.setColor(new Color(0xC9C4B5));
        g.fillRect(COL3.x + 10, COL3.y + 142, COL3.width - 20, 2);

        // Lista de Pokemon: punto rojo = ya esta en el equipo
        g.setFont(RetroStyle.font(22));
        FontMetrics fm = g.getFontMetrics();
        int last = Math.min(catalog.size(), pokeFirst + POKE_ROWS);
        for (int i = pokeFirst; i < last; i++) {
            Pokemon pokemon = catalog.get(i);
            int y = POKE_TOP + (i - pokeFirst) * POKE_ROW_H;
            boolean selected = i == pokeSelected;
            if (selected) {
                RetroStyle.drawSelectedRow(g, COL3.x + 8, y - 2, COL3.width - 34, POKE_ROW_H + 4);
            }
            RetroStyle.drawListDot(g, COL3.x + 26, y + POKE_ROW_H / 2, chosen.contains(pokemon));
            String name = RetroText.fit(fm, pokemon.getName().toUpperCase(), 138);
            RetroStyle.shadowText(g, name, COL3.x + 42, y + 23,
                    selected ? RetroStyle.RED_DARK : RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            g.setFont(RetroStyle.font(18));
            RetroStyle.drawTypeBadge(g, pokemon.getPrimaryType(), COL3.x + COL3.width - 110, y + 4, 76, 24);
            g.setFont(RetroStyle.font(22));
        }
        RetroStyle.drawScrollBar(g, COL3.x + COL3.width - 16, POKE_TOP, POKE_ROWS * POKE_ROW_H,
                pokeFirst, POKE_ROWS, catalog.size());

        drawNote(g, COL3.x + 16, COL3.y + 366, COL_W - 32, 1);

        // Botones
        g.setFont(RetroStyle.font(20));
        RetroStyle.drawKeyButton(g, "AGREGAR", ADD_BUTTON.x, ADD_BUTTON.y, ADD_BUTTON.width, ADD_BUTTON.height);
        RetroStyle.drawKeyButton(g, "QUITAR", REMOVE_BUTTON.x, REMOVE_BUTTON.y, REMOVE_BUTTON.width, REMOVE_BUTTON.height);
        RetroStyle.drawKeyButton(g, "LISTO", DONE_BUTTON.x, DONE_BUTTON.y, DONE_BUTTON.width, DONE_BUTTON.height);
        if (chosen.size() < Team.SIZE) {
            // LISTO se ve apagado hasta que haya 3 Pokemon
            g.setColor(new Color(247, 243, 224, 160));
            g.fillRoundRect(DONE_BUTTON.x, DONE_BUTTON.y, DONE_BUTTON.width, DONE_BUTTON.height, 10, 10);
        }
    }

    private void drawNote(Graphics2D g, int x, int y, int maxWidth, int maxLines) {
        if (message.isEmpty()) {
            return;
        }
        g.setFont(RetroStyle.font(20));
        List<String> lines = RetroText.wrap(g.getFontMetrics(), message, maxWidth);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            RetroStyle.shadowText(g, lines.get(i), x, y + i * 22, RetroStyle.RED_DARK, RetroStyle.SHADOW_LIGHT);
        }
    }

    // Marco azul alrededor de la columna donde esta el cursor
    private void drawFocusOutline(Graphics2D g) {
        Rectangle r = nameMode() || sideFocus ? COL3 : COL1;
        g.setColor(RetroStyle.BLUE);
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(r.x - 3, r.y - 3, r.width + 6, r.height + 6, 18, 18);
        g.setStroke(new BasicStroke(1));
    }
}