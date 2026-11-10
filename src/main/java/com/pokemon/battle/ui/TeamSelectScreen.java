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

    private static final Rectangle LIST = new Rectangle(20, 72, 300, 366);
    private static final Rectangle TEAM_BOX = new Rectangle(20, 450, 300, 134);
    private static final Rectangle NAME_PLATE = new Rectangle(336, 72, 604, 64);
    private static final Rectangle SPRITE_BOX = new Rectangle(336, 148, 210, 220);
    private static final Rectangle STATS_BOX = new Rectangle(558, 148, 382, 220);
    private static final Rectangle MOVES_BOX = new Rectangle(336, 380, 604, 200);

    // Botones de la barra gris (tambien se pueden pulsar con el mouse)
    private static final Rectangle ADD_BUTTON = new Rectangle(374, 598, 190, 36);
    private static final Rectangle REMOVE_BUTTON = new Rectangle(574, 598, 190, 36);
    private static final Rectangle DONE_BUTTON = new Rectangle(774, 598, 160, 36);

    private static final int ROW_H = 40;
    private static final int LIST_TOP = LIST.y + 12;
    private static final int VISIBLE_ROWS = (LIST.height - 24) / ROW_H;
    private static final int SLOT_H = 28;
    private static final int SLOTS_TOP = TEAM_BOX.y + 42;
    private static final int MAX_STAT = 200;
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

    /**
     * @param onDone que hacer con el equipo de 3 cuando se confirma
     * @param onBack que hacer al salir sin confirmar
     */
    public TeamSelectScreen(Consumer<List<Pokemon>> onDone, Runnable onBack) {
        super("ARMAR EQUIPO");
        this.onDone = onDone;
        this.onBack = onBack;

        bindKey("UP", () -> moveSelection(-1));
        bindKey("DOWN", () -> moveSelection(1));
        bindKey("LEFT", () -> moveSelection(-VISIBLE_ROWS));
        bindKey("RIGHT", () -> moveSelection(VISIBLE_ROWS));
        bindKey("ENTER", this::enter);
        bindKey("BACK_SPACE", this::removeLast);
        bindKey("DELETE", this::removeLast);

        animationTimer = new Timer(1000 / GifAnimation.FPS, e -> {
            frame++;
            repaint();
        });
        // La animacion solo corre mientras la pantalla se ve
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                animationTimer.start();
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
        repaint();
    }

    // ------------------------------------------------------------------ acciones

    private void moveSelection(int delta) {
        if (catalog.isEmpty()) {
            return;
        }
        select(Math.max(0, Math.min(catalog.size() - 1, selected + delta)));
    }

    private void select(int index) {
        selected = index;
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
        if (catalog.isEmpty()) {
            return;
        }
        if (chosen.size() >= Team.SIZE) {
            confirm();
        } else {
            addSelected();
        }
    }

    private void addSelected() {
        if (catalog.isEmpty()) {
            return;
        }
        Pokemon p = catalog.get(selected);
        if (chosen.size() >= Team.SIZE) {
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
        if (!chosen.isEmpty()) {
            chosen.remove(chosen.size() - 1);
            message = "";
            repaint();
        }
    }

    private void removeAt(int index) {
        if (index >= 0 && index < chosen.size()) {
            chosen.remove(index);
            message = "";
            repaint();
        }
    }

    private void confirm() {
        if (chosen.size() != Team.SIZE) {
            message = "ELIGE " + Team.SIZE + " POKEMON";
            repaint();
            return;
        }
        onDone.accept(new ArrayList<>(chosen));
    }

    @Override
    protected void goBack() {
        onBack.run();
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
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
                && y >= SLOTS_TOP && y < SLOTS_TOP + Team.SIZE * SLOT_H) {
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
        String enter = chosen.size() >= Team.SIZE ? "ENTER  LISTO" : "ENTER  AGREGAR";
        RetroStyle.drawKeyButton(g, enter, ADD_BUTTON.x, ADD_BUTTON.y, ADD_BUTTON.width, ADD_BUTTON.height);
        RetroStyle.drawKeyButton(g, "BORRAR  QUITAR", REMOVE_BUTTON.x, REMOVE_BUTTON.y,
                REMOVE_BUTTON.width, REMOVE_BUTTON.height);
        RetroStyle.drawKeyButton(g, "LISTO", DONE_BUTTON.x, DONE_BUTTON.y, DONE_BUTTON.width, DONE_BUTTON.height);
        if (chosen.size() < Team.SIZE) {
            // LISTO se ve apagado hasta que haya 3 Pokemon
            g.setColor(new Color(214, 214, 214, 170));
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
                hasMessage ? RetroStyle.RED_DARK : RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);

        g.setFont(RetroStyle.font(22));
        for (int i = 0; i < Team.SIZE; i++) {
            int y = SLOTS_TOP + i * SLOT_H;
            g.setColor(i % 2 == 0 ? RetroStyle.WHITE : new Color(0xEEEBE2));
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
        g.setColor(new Color(0xEFEBDD));
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
        RetroStyle.shadowText(g, "ESTADISTICAS BASE", r.x + 16, r.y + 30, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);

        String[] labels = {"PS", "ATAQUE", "DEFENSA", "AT. ESP.", "DEF. ESP.", "VELOCIDAD"};
        int[] values = {p.getHp(), p.getAttack(), p.getDefense(), p.getSpecialAttack(), p.getSpecialDefense(), p.getSpeed()};
        int rowH = 28;
        int top = r.y + 40;
        int lineX = r.x + 150;
        int barX = lineX + 12;
        int barMax = r.x + r.width - 16 - barX;
        g.setFont(RetroStyle.font(22));
        for (int i = 0; i < labels.length; i++) {
            int y = top + i * rowH;
            g.setColor(i % 2 == 0 ? RetroStyle.WHITE : new Color(0xEEEBE2));
            g.fillRect(r.x + 6, y, r.width - 12, rowH);
            RetroStyle.shadowText(g, labels[i], r.x + 14, y + 20, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            String value = String.valueOf(values[i]);
            int vw = g.getFontMetrics().stringWidth(value);
            RetroStyle.shadowText(g, value, lineX - 10 - vw, y + 20, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            RetroStyle.drawStatBar(g, barX, y + 6, barMax, 16, values[i], MAX_STAT);
        }
        g.setColor(new Color(0xC9C4B5));
        g.fillRect(lineX, top, 2, rowH * labels.length);
    }

    private void drawMoves(Graphics2D g, Pokemon p) {
        Rectangle r = MOVES_BOX;
        RetroStyle.drawPanel(g, r.x, r.y, r.width, r.height);
        g.setFont(RetroStyle.font(26));
        RetroStyle.shadowText(g, "ATAQUES", r.x + 16, r.y + 32, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);

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
        g.setColor(new Color(0xE3DDC4));
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