package com.pokemon.battle.ui;

import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Team;
import com.pokemon.battle.model.Type;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla para armar el equipo de 3 Pokemon, con el diseno de la Pokedex:
 * lista a la izquierda, equipo debajo de la lista y, a la derecha, el sprite
 * animado, las estadisticas base y los ataques del Pokemon marcado.
 */
public class TeamSelectScreen extends RetroScreen {

    private static final Rectangle LIST = new Rectangle(20, 28, 300, 386);
    private static final Rectangle TEAM_BOX = new Rectangle(20, 426, 300, 134);
    private static final Rectangle NAME_PLATE = new Rectangle(336, 28, 604, 64);
    private static final Rectangle SPRITE_BOX = new Rectangle(336, 116, 210, 220);
    private static final Rectangle STATS_BOX = new Rectangle(558, 116, 382, 220);
    private static final Rectangle MOVES_BOX = new Rectangle(336, 360, 604, 200);
    // Botones de la barra gris (tambien se pueden pulsar con el mouse)
    private static final Rectangle ADD_BUTTON = new Rectangle(374, 598, 190, 36);
    private static final Rectangle REMOVE_BUTTON = new Rectangle(574, 598, 190, 36);
    private static final Rectangle DONE_BUTTON = new Rectangle(774, 598, 160, 36);
    private static final boolean CONFIRM_ON_ADD = false;
    private static final boolean CONFIRM_ON_FINISH = true;
    private static final Rectangle DIALOG = new Rectangle(280, 250, 400, 160);
    private static final Rectangle YES_BUTTON = new Rectangle(310, 348, 150, 44);
    private static final Rectangle NO_BUTTON = new Rectangle(500, 348, 150, 44);

    private enum Dialog { NONE, ADD, FINISH }

    private Dialog dialog = Dialog.NONE;
    private boolean dialogYes = true;

    private static final int ROW_H = 40;
    private static final int LIST_TOP = LIST.y + 12;
    private static final int VISIBLE_ROWS = (LIST.height - 24) / ROW_H;
    private static final int SLOT_H = 28;
    private static final int SLOTS_TOP = TEAM_BOX.y + 42;
    private static final int MAX_STAT = 200;
    private final int teamSize;
    private static final int SPRITE_TARGET = 172;

    private final Consumer<List<Pokemon>> onDone;
    private final Runnable onBack;
    private final SpriteLoader sprites = SpriteLoader.shared();
    private final Timer animationTimer;
    private final List<Pokemon> chosen = new ArrayList<>();

    private List<Pokemon> catalog = new ArrayList<>();
    private String trainerName = "";
    private int selected = 0;
    private int firstVisible = 0;
    private int frame = 0;
    private String message = "";
    private boolean hexStats = false;

    /**
     * @param onDone que hacer con el equipo de 3 cuando se confirma
     * @param onBack que hacer al salir sin confirmar
     */
    public TeamSelectScreen(Consumer<List<Pokemon>> onDone, Runnable onBack) {
        this(onDone, onBack, Team.SIZE);
    }

    /**
     * @param teamSize cuantos Pokemon hay que elegir (3 en el modo normal, 2 en Combate Libre)
     */
    public TeamSelectScreen(Consumer<List<Pokemon>> onDone, Runnable onBack, int teamSize) {
        super("ARMAR EQUIPO");
        this.teamSize = teamSize;
        this.onDone = onDone;
        this.onBack = onBack;

        bindKey("UP", () -> moveSelection(-1));
        bindKey("DOWN", () -> moveSelection(1));
        bindKey("LEFT", () -> moveSelection(-VISIBLE_ROWS));
        bindKey("RIGHT", () -> moveSelection(VISIBLE_ROWS));
        bindKey("ENTER", this::enter);
        bindKey("BACK_SPACE", this::removeLast);
        bindKey("DELETE", this::removeLast);
        bindKey("H", this::toggleStats);
        bindKey("F2", this::requestFinish);

        animationTimer = new Timer(1000 / GifAnimation.FPS, e -> {
            frame++;
            repaint();
        });
        // La animacion solo corre mientras la pantalla se ve
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                animationTimer.start();
                setFocusable(true);
                javax.swing.SwingUtilities.invokeLater(
                        TeamSelectScreen.this::requestFocusInWindow);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                animationTimer.stop();
            }
        });
    }

    /**
     * Deja la pantalla lista para un entrenador.
     *
     * @param name    nombre del entrenador
     * @param pokemon catalogo de Pokemon
     */
    public void prepare(String name, List<Pokemon> pokemon) {
        this.trainerName = name.toUpperCase();
        this.catalog = new ArrayList<>(pokemon);
        this.catalog.sort(Comparator.comparingInt(Pokemon::getId));
        chosen.clear();
        selected = 0;
        firstVisible = 0;
        frame = 0;
        message = "";
        javax.swing.SwingUtilities.invokeLater(this::requestFocusInWindow);
        repaint();
    }

    // ------------------------------------------------------------------ acciones

    private void moveSelection(int delta) {
        if (dialog != Dialog.NONE) {
            dialogYes = !dialogYes;
            repaint();
            return;
        }
        if (catalog.isEmpty()) {
            return;
        }
        select(Math.max(0, Math.min(catalog.size() - 1, selected + delta)));
    }

    private void select(int index) {
        selected = index;
        System.out.println("select -> selected=" + selected + " first=" + firstVisible
                + " catalog=" + catalog.size() + " visible=" + isShowing());
        frame = 0;
        message = "";
        if (selected < firstVisible) {
            firstVisible = selected;
        } else if (selected >= firstVisible + VISIBLE_ROWS) {
            firstVisible = selected - VISIBLE_ROWS + 1;
        }
        repaint();
    }

    // Enter agrega; con el equipo completo, confirma
    private void enter() {
        if (dialog != Dialog.NONE) {
            acceptDialog();
            return;
        }
        if (catalog.isEmpty()) {
            return;
        }
        if (chosen.size() >= teamSize) {
            requestFinish();
        } else {
            requestAdd();
        }
    }

    private void addSelected() {
        if (catalog.isEmpty()) {
            return;
        }
        Pokemon p = catalog.get(selected);
        if (chosen.size() >= teamSize) {
            message = "EQUIPO COMPLETO";
        } else if (chosen.contains(p)) {
            message = "YA ESTA EN EL EQUIPO";
        } else {
            chosen.add(p);
            message = "";
        }
        repaint();
    }

    private void removeLast() {
        if (dialog != Dialog.NONE) return;
        if (!chosen.isEmpty()) {
            chosen.remove(chosen.size() - 1);
            message = "";
            repaint();
        }
    }

    private void removeAt(int index) {
        if (dialog != Dialog.NONE) return;
        if (index >= 0 && index < chosen.size()) {
            chosen.remove(index);
            message = "";
            repaint();
        }
    }

    private void toggleStats() {
        hexStats = !hexStats;
        repaint();
    }

    private void requestAdd() {
        if (catalog.isEmpty()) {
            return;
        }
        Pokemon p = catalog.get(selected);
        // Si ya esta lleno o repetido, addSelected muestra el mensaje sin preguntar
        if (!CONFIRM_ON_ADD || chosen.size() >= teamSize || chosen.contains(p)) {
            addSelected();
            return;
        }
        openDialog(Dialog.ADD);
    }

    private void requestFinish() {
        if (dialog != Dialog.NONE) {
            return;
        }
        // Si faltan Pokemon, confirm() muestra "ELIGE N POKEMON"
        if (!CONFIRM_ON_FINISH || chosen.size() != teamSize) {
            confirm();
            return;
        }
        openDialog(Dialog.FINISH);
    }

    private void openDialog(Dialog d) {
        dialog = d;
        dialogYes = true;
        repaint();
    }

    private void acceptDialog() {
        Dialog d = dialog;
        boolean yes = dialogYes;
        dialog = Dialog.NONE;
        if (yes) {
            if (d == Dialog.ADD) {
                addSelected();
            } else if (d == Dialog.FINISH) {
                confirm();
            }
        }
        repaint();
    }

    private void confirm() {
        if (chosen.size() != teamSize) {
            message = "ELIGE " + teamSize + " POKEMON";
            repaint();
            return;
        }
        onDone.accept(new ArrayList<>(chosen));
    }

    @Override
    protected void goBack() {
        if (dialog != Dialog.NONE) {
            dialog = Dialog.NONE;
            repaint();
            return;
        }
        onBack.run();
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        if (dialog != Dialog.NONE) {
            if (YES_BUTTON.contains(x, y)) {
                dialogYes = true;
                acceptDialog();
            } else if (NO_BUTTON.contains(x, y)) {
                dialogYes = false;
                acceptDialog();
            }
            return;
        }
        if (STATS_BOX.contains(x, y)) {
            toggleStats();
            return;
        }
        if (ADD_BUTTON.contains(x, y)) {
            enter();
            return;
        }
        if (REMOVE_BUTTON.contains(x, y)) {
            removeLast();
            return;
        }
        if (DONE_BUTTON.contains(x, y)) {
            confirm();
            return;
        }
        if (LIST.contains(x, y) && y >= LIST_TOP) {
            int index = firstVisible + (y - LIST_TOP) / ROW_H;
            if (index < Math.min(catalog.size(), firstVisible + VISIBLE_ROWS)) {
                select(index);
                if (clickCount >= 2) {
                    addSelected();
                }
            }
            return;
        }
        if (x >= TEAM_BOX.x && x < TEAM_BOX.x + TEAM_BOX.width
                && y >= SLOTS_TOP && y < SLOTS_TOP + teamSize * SLOT_H) {
            removeAt((y - SLOTS_TOP) / SLOT_H);
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
        RetroStyle.drawArrowKeyButton(g, true, "MOVER", 214, 598, 150, 36);
        String enter = chosen.size() >= teamSize ? "ENTER  LISTO" : "ENTER  AGREGAR";
        RetroStyle.drawKeyButton(g, enter, ADD_BUTTON.x, ADD_BUTTON.y, ADD_BUTTON.width, ADD_BUTTON.height);
        RetroStyle.drawKeyButton(g, "BORRAR  QUITAR", REMOVE_BUTTON.x, REMOVE_BUTTON.y,
                REMOVE_BUTTON.width, REMOVE_BUTTON.height);
        RetroStyle.drawKeyButton(g, "LISTO", DONE_BUTTON.x, DONE_BUTTON.y, DONE_BUTTON.width, DONE_BUTTON.height);
        if (chosen.size() < teamSize) {
            // LISTO se ve apagado hasta que haya 3 Pokemon
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(DONE_BUTTON.x, DONE_BUTTON.y, DONE_BUTTON.width, DONE_BUTTON.height, 10, 10);
        }
    }

    @Override
    protected void paintContent(Graphics2D g) {
        if (catalog.isEmpty()) {
            drawMessage(g, "NO HAY POKEMON REGISTRADOS");
            return;
        }
        Pokemon current = catalog.get(selected);
        drawList(g);
        drawTeamBox(g);
        drawNamePlate(g, current);
        drawSprite(g, current);
        drawStats(g, current);
        drawMoves(g, current);
        if (dialog != Dialog.NONE) {
            drawDialog(g, current);
        }
    }

    private void drawDialog(Graphics2D g, Pokemon current) {
        // Oscurece lo de atras
        g.setColor(new Color(26, 31, 43, 150));
        g.fillRect(0, CONTENT_TOP, BOARD_W, FOOTER_Y - CONTENT_TOP);
        RetroStyle.drawRedFrame(g, DIALOG.x, DIALOG.y, DIALOG.width, DIALOG.height);

        String text = dialog == Dialog.ADD
                ? "ELEGIR A " + current.getName().toUpperCase() + "?"
                : "LISTO CON ESTE EQUIPO?";
        g.setFont(RetroStyle.font(30));
        text = RetroText.fit(g.getFontMetrics(), text, DIALOG.width - 40);
        int w = g.getFontMetrics().stringWidth(text);
        RetroStyle.shadowText(g, text, DIALOG.x + (DIALOG.width - w) / 2, DIALOG.y + 75,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        g.setFont(RetroStyle.font(28));
        drawChoice(g, "SI", YES_BUTTON, dialogYes);
        drawChoice(g, "NO", NO_BUTTON, !dialogYes);
    }

    private void drawChoice(Graphics2D g, String label, Rectangle r, boolean active) {
        if (active) {
            RetroStyle.drawSelectedRow(g, r.x, r.y, r.width, r.height);
        } else {
            RetroStyle.drawKeyButton(g, "", r.x, r.y, r.width, r.height);
        }
        int w = g.getFontMetrics().stringWidth(label);
        RetroStyle.shadowText(g, label, r.x + (r.width - w) / 2, r.y + 31,
                active ? RetroStyle.RED_DARK : RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
    }

    private void drawList(Graphics2D g) {
        RetroStyle.drawListFrame(g, LIST.x, LIST.y, LIST.width, LIST.height);
        int last = Math.min(catalog.size(), firstVisible + VISIBLE_ROWS);
        for (int i = firstVisible; i < last; i++) {
            int y = LIST_TOP + (i - firstVisible) * ROW_H;
            boolean isSelected = i == selected;
            if (isSelected) {
                RetroStyle.drawSelectedRow(g, LIST.x + 22, y - 3, LIST.width - 44, ROW_H + 6);
            }
            Pokemon p = catalog.get(i);
            Color main = isSelected ? RetroStyle.RED_DARK : RetroStyle.DARK;
            RetroStyle.drawListDot(g, LIST.x + 46, y + ROW_H / 2, isSelected);
            g.setFont(RetroStyle.font(26));
            FontMetrics fm = g.getFontMetrics();
            RetroStyle.shadowText(g, String.format("%03d", p.getId()), LIST.x + 72, y + 29,
                    main, RetroStyle.SHADOW_LIGHT);
            String name = RetroText.fit(fm, p.getName().toUpperCase(), 112);
            RetroStyle.shadowText(g, name, LIST.x + 124, y + 29, main, RetroStyle.SHADOW_LIGHT);

            // Numero azul: ya esta en el equipo (1, 2 o 3)
            int slot = chosen.indexOf(p);
            if (slot >= 0) {
                int cx = LIST.x + LIST.width - 56;
                int cy = y + 8;
                g.setColor(RetroStyle.OUTLINE);
                g.fillRoundRect(cx, cy, 24, 24, 8, 8);
                g.setColor(RetroStyle.BLUE);
                g.fillRoundRect(cx + 2, cy + 2, 20, 20, 6, 6);
                g.setFont(RetroStyle.font(20));
                String n = String.valueOf(slot + 1);
                int nw = g.getFontMetrics().stringWidth(n);
                RetroStyle.shadowText(g, n, cx + (24 - nw) / 2, cy + 19, RetroStyle.WHITE, RetroStyle.OUTLINE);
            }
        }
        RetroStyle.drawScrollBar(g, LIST.x + LIST.width - 16, LIST.y + 14, LIST.height - 28,
                firstVisible, VISIBLE_ROWS, catalog.size());
    }

    private void drawTeamBox(Graphics2D g) {
        RetroStyle.drawPanel(g, TEAM_BOX.x, TEAM_BOX.y, TEAM_BOX.width, TEAM_BOX.height);
        g.setFont(RetroStyle.font(24));
        boolean hasMessage = !message.isEmpty();
        String title = RetroText.fit(g.getFontMetrics(),
                hasMessage ? message : "EQUIPO: " + trainerName, TEAM_BOX.width - 32);
        RetroStyle.shadowText(g, title, TEAM_BOX.x + 16, TEAM_BOX.y + 30,
                hasMessage ? RetroStyle.PINK : RetroStyle.ACCENT, RetroStyle.SHADOW_LIGHT);

        g.setFont(RetroStyle.font(22));
        for (int i = 0; i < teamSize; i++) {
            int y = SLOTS_TOP + i * SLOT_H;
            g.setColor(i % 2 == 0 ? RetroStyle.ROW_A : RetroStyle.ROW_B);
            g.fillRect(TEAM_BOX.x + 10, y, TEAM_BOX.width - 20, SLOT_H);
            String name = i < chosen.size() ? chosen.get(i).getName().toUpperCase() : "---";
            RetroStyle.shadowText(g, (i + 1) + ". " + name, TEAM_BOX.x + 18, y + 21,
                    RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
    }

    private void drawNamePlate(Graphics2D g, Pokemon p) {
        Rectangle r = NAME_PLATE;
        RetroStyle.drawRedFrame(g, r.x, r.y, r.width, r.height);
        g.setFont(RetroStyle.font(34));
        RetroStyle.shadowText(g, "No." + String.format("%03d", p.getId()), r.x + 24, r.y + 43,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        RetroStyle.shadowText(g, p.getName().toUpperCase(), r.x + 130, r.y + 43,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        g.setFont(RetroStyle.font(22));
        int badgeX = r.x + r.width - 24 - 96;
        if (p.getSecondaryType() != null && p.getSecondaryType() != Type.NONE) {
            RetroStyle.drawTypeBadge(g, p.getSecondaryType(), badgeX, r.y + 16, 96, 32);
            badgeX -= 106;
        }
        RetroStyle.drawTypeBadge(g, p.getPrimaryType(), badgeX, r.y + 16, 96, 32);
    }

    private void drawSprite(Graphics2D g, Pokemon p) {
        Rectangle r = SPRITE_BOX;
        RetroStyle.drawPanel(g, r.x, r.y, r.width, r.height);
        g.setColor(new Color(255, 255, 255, 14));
        for (int i = 20; i < r.width - 6; i += 20) {
            g.fillRect(r.x + i, r.y + 6, 1, r.height - 12);
            g.fillRect(r.x + 6, r.y + i, r.width - 12, 1);
        }
        boolean loaded = sprites.isLoaded(p.getName(), false);
        GifAnimation sprite = sprites.getOrLoad(p.getName(), false, this::repaint);
        if (!loaded) {
            return; // Todavia se esta leyendo el GIF; al terminar se vuelve a dibujar
        }
        if (sprite == null) {
            g.setFont(RetroStyle.font(24));
            String text = "SIN SPRITE";
            int w = g.getFontMetrics().stringWidth(text);
            RetroStyle.shadowText(g, text, r.x + (r.width - w) / 2, r.y + r.height / 2 + 8,
                    RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            return;
        }
        double factor = Math.min(SPRITE_TARGET / (double) sprite.width(), SPRITE_TARGET / (double) sprite.height());
        int w = (int) Math.round(sprite.width() * factor);
        int h = (int) Math.round(sprite.height() * factor);
        BufferedImage current = sprite.frame(frame);
        RetroStyle.drawPixelImage(g, current, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h);
    }

    private void drawStats(Graphics2D g, Pokemon p) {
        Rectangle r = STATS_BOX;
        RetroStyle.drawPanel(g, r.x, r.y, r.width, r.height);
        g.setFont(RetroStyle.font(26));
        RetroStyle.shadowText(g, "ESTADISTICAS BASE", r.x + 16, r.y + 30, RetroStyle.ACCENT, RetroStyle.SHADOW_LIGHT);

        String[] labels = {"PS", "ATAQUE", "DEFENSA", "AT. ESP.", "DEF. ESP.", "VELOCIDAD"};
        int[] values = {p.getHp(), p.getAttack(), p.getDefense(), p.getSpecialAttack(), p.getSpecialDefense(), p.getSpeed()};
        // Pista de la tecla, a la derecha del titulo
        g.setFont(RetroStyle.font(18));
        String hint = hexStats ? "H / Y  BARRAS" : "H / Y  HEXAGONO";
        int hw = g.getFontMetrics().stringWidth(hint);
        RetroStyle.shadowText(g, hint, r.x + r.width - 14 - hw, r.y + 28, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        if (hexStats) {
            String[] hexLabels = {"PS", "ATAQUE", "DEFENSA", "VELOC.", "DEF.ESP.", "AT.ESP."};
            int[] hexValues = {p.getHp(), p.getAttack(), p.getDefense(), p.getSpeed(),
                    p.getSpecialDefense(), p.getSpecialAttack()};

            Color typeA = RetroStyle.typeColor(p.getPrimaryType());
            boolean twoTypes = p.getSecondaryType() != null && p.getSecondaryType() != Type.NONE;
            Color typeB = twoTypes ? RetroStyle.typeColor(p.getSecondaryType()) : typeA;
            RetroStyle.drawStatRadar(g, r.x + 191, r.y + 126, 60, hexLabels, hexValues,
                    RetroStyle.RADAR_MAX, typeA, typeB);

            g.setFont(RetroStyle.font(20));
            String total = p.getTotalStats() >= 0 ? String.valueOf(p.getTotalStats()) : "--";
            RetroStyle.shadowText(g, "TOTAL " + total, r.x + 14, r.y + r.height - 12,
                    RetroStyle.ACCENT, RetroStyle.SHADOW_LIGHT);
            return;
        }
        int rowH = 25;
        int top = r.y + 40;
        int lineX = r.x + 150;
        int barX = lineX + 12;
        int barMax = r.x + r.width - 16 - barX;
        g.setFont(RetroStyle.font(22));
        for (int i = 0; i < labels.length; i++) {
            int y = top + i * rowH;
            g.setColor(i % 2 == 0 ? RetroStyle.ROW_A : RetroStyle.ROW_B);
            g.fillRect(r.x + 6, y, r.width - 12, rowH);
            RetroStyle.shadowText(g, labels[i], r.x + 14, y + 19, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            String value = String.valueOf(values[i]);
            int vw = g.getFontMetrics().stringWidth(value);
            RetroStyle.shadowText(g, value, lineX - 10 - vw, y + 19, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            RetroStyle.drawStatBar(g, barX, y + 5, barMax, 15, values[i], MAX_STAT);
        }
        g.setColor(RetroStyle.LINE);
        g.fillRect(lineX, top, 2, rowH * labels.length);
        drawTotalStats(g, p, r, top + rowH * labels.length, lineX);
    }

    /** Fila TOTAL: la suma de stats que calcula la funcion func_stats_totales_pokken de la BD. */
    private void drawTotalStats(Graphics2D g, Pokemon p, Rectangle r, int y, int lineX) {
        g.setColor(new Color(255, 217, 90, 45));
        g.fillRect(r.x + 6, y, r.width - 12, 25);
        g.setColor(RetroStyle.LINE);
        g.fillRect(r.x + 6, y, r.width - 12, 2);
        g.setFont(RetroStyle.font(22));
        RetroStyle.shadowText(g, "TOTAL", r.x + 14, y + 20, RetroStyle.ACCENT, RetroStyle.SHADOW_LIGHT);
        String total = p.getTotalStats() >= 0 ? String.valueOf(p.getTotalStats()) : "--";
        int tw = g.getFontMetrics().stringWidth(total);
        RetroStyle.shadowText(g, total, lineX - 10 - tw, y + 20, RetroStyle.ACCENT, RetroStyle.SHADOW_LIGHT);
    }

    private void drawMoves(Graphics2D g, Pokemon p) {
        Rectangle r = MOVES_BOX;
        RetroStyle.drawPanel(g, r.x, r.y, r.width, r.height);
        g.setFont(RetroStyle.font(26));
        RetroStyle.shadowText(g, "ATAQUES", r.x + 16, r.y + 32, RetroStyle.ACCENT, RetroStyle.SHADOW_LIGHT);

        int categoryX = r.x + 330;
        int potRight = r.x + 470;
        int accRight = r.x + 534;
        int ppRight = r.x + r.width - 18;
        g.setFont(RetroStyle.font(20));
        int headerY = r.y + 32;
        int cw = g.getFontMetrics().stringWidth("CATEG.");
        RetroStyle.shadowText(g, "CATEG.", categoryX + (64 - cw) / 2, headerY, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        rightText(g, "POT.", potRight, headerY);
        rightText(g, "PREC.", accRight, headerY);
        rightText(g, "PP", ppRight, headerY);
        g.setColor(RetroStyle.LINE);
        g.fillRect(r.x + 14, r.y + 40, r.width - 28, 2);

        List<Move> moves = p.getMoves();
        for (int i = 0; i < moves.size() && i < 4; i++) {
            Move m = moves.get(i);
            int y = r.y + 46 + i * 37;
            g.setFont(RetroStyle.font(20));
            RetroStyle.drawTypeBadge(g, m.getType(), r.x + 14, y + 2, 96, 32);
            g.setFont(RetroStyle.font(24));
            RetroStyle.shadowText(g, m.getName().toUpperCase(), r.x + 122, y + 26,
                    RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            RetroStyle.drawCategory(g, m.getCategory(), categoryX, y + 2, 64, 32);
            g.setFont(RetroStyle.font(22));
            rightText(g, m.getPower() > 0 ? String.valueOf(m.getPower()) : "-", potRight, y + 26);
            rightText(g, String.valueOf(m.getAccuracy()), accRight, y + 26);
            rightText(g, String.valueOf(m.getPp()), ppRight, y + 26);
        }
    }

    private void rightText(Graphics2D g, String text, int rightX, int baseline) {
        int w = g.getFontMetrics().stringWidth(text);
        RetroStyle.shadowText(g, text, rightX - w, baseline, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
    }
}