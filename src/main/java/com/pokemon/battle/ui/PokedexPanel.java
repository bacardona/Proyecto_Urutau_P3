package com.pokemon.battle.ui;

import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.service.PokemonCatalog;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pokedex screen: the list of every Pokemon on the left and, on the right, the
 * data of the selected one (animated sprite, types, base stats and moves).
 * <p>
 * It is controlled with the keyboard (main way) and with the mouse (extra way);
 * both call the same methods. The common frame (grid, bars, scaling) comes from
 * {@link RetroScreen}.
 */
public class PokedexPanel extends RetroScreen {

    private static final Rectangle LIST = new Rectangle(20, 72, 300, 508);
    private static final Rectangle NAME_PLATE = new Rectangle(336, 72, 604, 64);
    private static final Rectangle SPRITE_BOX = new Rectangle(336, 148, 210, 220);
    private static final Rectangle STATS_BOX = new Rectangle(558, 148, 382, 220);
    private static final Rectangle MOVES_BOX = new Rectangle(336, 380, 604, 200);

    private static final int ROW_H = 40;
    private static final int LIST_TOP = LIST.y + 12;
    private static final int VISIBLE_ROWS = (LIST.height - 24) / ROW_H;
    // Valor que llena la barra completa (los que pasan de aqui, como Chansey con 250, la llenan toda)
    private static final int MAX_STAT = 200;
    // Todas las animaciones se reproducen a la misma velocidad
    private static final int ANIMATION_FPS = GifAnimation.FPS;
    // Lado del cuadro donde debe caber cualquier sprite, para que todos se vean del mismo tamano
    private static final int SPRITE_TARGET = 172;

    private final PokemonCatalog catalog;
    private final Runnable onBack;
    private final SpriteLoader sprites = SpriteLoader.shared();
    private final Timer animationTimer;
    // Hilo aparte para leer los GIF sin trabar la ventana mientras se recorre la lista
    private final ExecutorService spriteWorker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "pokedex-sprites");
        thread.setDaemon(true);
        return thread;
    });

    private List<Pokemon> pokemon = new ArrayList<>();
    private int selected = 0;
    private int firstVisible = 0;
    private boolean loading = false;
    private boolean loaded = false;
    private String errorMessage;

    private GifAnimation sprite;
    private String spriteOwner;
    private boolean spriteReady;
    private int frame;

    /**
     * @param catalog catalog of Pokemon, read from the database (injected)
     * @param onBack  what to do when the user leaves the Pokedex, for example show the menu
     */
    public PokedexPanel(PokemonCatalog catalog, Runnable onBack) {
        super("POKEDEX");
        this.catalog = catalog;
        this.onBack = onBack;
        bindKeys();
        animationTimer = new Timer(1000 / ANIMATION_FPS, e -> {
            frame++;
            repaint();
        });
        // Los datos se cargan la primera vez que se muestra la pantalla, no al abrir el juego
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadPokemon();
                animationTimer.start();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                // Si la Pokedex no se ve, la animacion no gasta procesador
                animationTimer.stop();
            }
        });
    }

    /**
     * Reads the Pokemon from the catalog in the background, so the window does
     * not freeze while the database answers. Does nothing if they are already loaded.
     */
    public void loadPokemon() {
        if (loading || loaded) {
            return;
        }
        loading = true;
        errorMessage = null;
        repaint();
        new SwingWorker<List<Pokemon>, Void>() {
            @Override
            protected List<Pokemon> doInBackground() {
                List<Pokemon> all = new ArrayList<>(catalog.listAll());
                all.sort(Comparator.comparingInt(Pokemon::getId));
                return all;
            }

            @Override
            protected void done() {
                loading = false;
                try {
                    pokemon = get();
                    loaded = true;
                    select(0);
                } catch (InterruptedException | ExecutionException e) {
                    errorMessage = "NO SE PUDIERON CARGAR LOS POKEMON";
                    // El detalle va a la consola para saber si fallo la conexion o la consulta
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.err.println("Pokedex: " + cause);
                    if (cause.getCause() != null) {
                        System.err.println("Causa: " + cause.getCause());
                    }
                }
                repaint();
            }
        }.execute();
    }

    // ------------------------------------------------------------------ acciones

    /**
     * Moves the selection a number of rows up (negative) or down (positive).
     *
     * @param delta rows to move
     */
    public void moveSelection(int delta) {
        if (pokemon.isEmpty()) {
            return;
        }
        int target = Math.max(0, Math.min(pokemon.size() - 1, selected + delta));
        select(target);
    }

    private void select(int index) {
        selected = index;
        requestSprite(pokemon.get(index).getName());
        // La lista se desplaza para que el seleccionado siempre se vea
        if (selected < firstVisible) {
            firstVisible = selected;
        } else if (selected >= firstVisible + VISIBLE_ROWS) {
            firstVisible = selected - VISIBLE_ROWS + 1;
        }
        repaint();
    }

    // Pide el GIF del Pokemon seleccionado; si no esta en memoria se lee en el otro hilo
    private void requestSprite(String name) {
        spriteOwner = name;
        frame = 0;
        if (sprites.isLoaded(name)) {
            sprite = sprites.front(name);
            spriteReady = true;
            return;
        }
        sprite = null;
        spriteReady = false;
        spriteWorker.submit(() -> {
            GifAnimation loadedSprite = sprites.front(name);
            SwingUtilities.invokeLater(() -> {
                // Solo se usa si el usuario sigue en ese Pokemon
                if (name.equals(spriteOwner)) {
                    sprite = loadedSprite;
                    spriteReady = true;
                    frame = 0;
                    repaint();
                }
            });
        });
    }

    @Override
    protected void goBack() {
        onBack.run();
    }

    // ------------------------------------------------------------------ teclado y mouse

    private void bindKeys() {
        bindKey("UP", () -> moveSelection(-1));
        bindKey("DOWN", () -> moveSelection(1));
        bindKey("LEFT", () -> moveSelection(-VISIBLE_ROWS));
        bindKey("RIGHT", () -> moveSelection(VISIBLE_ROWS));
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        if (LIST.contains(x, y) && y >= LIST_TOP) {
            int index = firstVisible + (y - LIST_TOP) / ROW_H;
            if (index < pokemon.size()) {
                select(index);
            }
        }
    }

    @Override
    protected void onWheel(int rotation) {
        moveSelection(rotation);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void paintContent(Graphics2D g) {
        if (loading) {
            drawMessage(g, "CARGANDO POKEDEX...");
        } else if (errorMessage != null) {
            drawMessage(g, errorMessage);
        } else if (pokemon.isEmpty()) {
            drawMessage(g, "NO HAY POKEMON REGISTRADOS");
        } else {
            Pokemon current = pokemon.get(selected);
            drawList(g);
            drawNamePlate(g, current);
            drawSprite(g, current);
            drawStats(g, current);
            drawMoves(g, current);
        }
    }

    @Override
    protected void paintFooterButtons(Graphics2D g) {
        super.paintFooterButtons(g);
        RetroStyle.drawArrowKeyButton(g, true, "MOVER", 214, 598, 170, 36);
        RetroStyle.drawArrowKeyButton(g, false, "PAGINA", 404, 598, 170, 36);
    }

    private void drawList(Graphics2D g) {
        // Marco con franja roja a la izquierda
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(LIST.x, LIST.y, LIST.width, LIST.height, 14, 14);
        g.setPaint(new GradientPaint(LIST.x, LIST.y, RetroStyle.RED, LIST.x + 26, LIST.y, RetroStyle.RED_DARK));
        g.fillRoundRect(LIST.x + 3, LIST.y + 3, 30, LIST.height - 6, 10, 10);
        g.setPaint(new GradientPaint(0, LIST.y + LIST.height * 0.7f, RetroStyle.WHITE, 0, LIST.y + LIST.height, RetroStyle.PANEL_BOTTOM));
        g.fillRect(LIST.x + 30, LIST.y + 3, LIST.width - 33, LIST.height - 6);

        g.setFont(RetroStyle.font(26));
        int last = Math.min(pokemon.size(), firstVisible + VISIBLE_ROWS);
        for (int i = firstVisible; i < last; i++) {
            int y = LIST_TOP + (i - firstVisible) * ROW_H;
            boolean isSelected = i == selected;
            if (isSelected) {
                // La seleccion sobresale un poco de la fila y del marco
                g.setColor(RetroStyle.OUTLINE);
                g.fillRoundRect(LIST.x + 22, y - 3, LIST.width - 40, ROW_H + 6, 14, 14);
                g.setColor(RetroStyle.RED);
                g.fillRoundRect(LIST.x + 25, y, LIST.width - 46, ROW_H, 10, 10);
                g.setColor(new Color(0xFDECEA));
                g.fillRoundRect(LIST.x + 30, y + 5, LIST.width - 56, ROW_H - 10, 6, 6);
            }
            Pokemon p = pokemon.get(i);
            Color main = isSelected ? RetroStyle.RED_DARK : RetroStyle.DARK;
            int baseline = y + 29;
            // Punto: relleno si esta seleccionado
            g.setColor(RetroStyle.OUTLINE);
            g.fillOval(LIST.x + 44, y + 12, 16, 16);
            g.setColor(isSelected ? RetroStyle.RED : RetroStyle.WHITE);
            g.fillOval(LIST.x + 47, y + 15, 10, 10);
            RetroStyle.shadowText(g, String.format("%03d", p.getId()), LIST.x + 72, baseline, main, RetroStyle.SHADOW_LIGHT);
            RetroStyle.shadowText(g, p.getName().toUpperCase(), LIST.x + 124, baseline, main, RetroStyle.SHADOW_LIGHT);
        }
        drawScrollBar(g);
    }

    private void drawScrollBar(Graphics2D g) {
        int x = LIST.x + LIST.width - 16;
        int top = LIST.y + 14;
        int height = LIST.height - 28;
        g.setColor(new Color(0xD9D4C2));
        g.fillRoundRect(x, top, 8, height, 8, 8);
        if (pokemon.size() > VISIBLE_ROWS) {
            int thumbH = Math.max(30, height * VISIBLE_ROWS / pokemon.size());
            int thumbY = top + (height - thumbH) * firstVisible / (pokemon.size() - VISIBLE_ROWS);
            g.setColor(RetroStyle.BLUE_DARK);
            g.fillRoundRect(x - 2, thumbY, 12, thumbH, 8, 8);
            g.setColor(RetroStyle.BLUE);
            g.fillRoundRect(x, thumbY + 2, 6, thumbH - 4, 6, 6);
        }
    }

    private void drawNamePlate(Graphics2D g, Pokemon p) {
        Rectangle r = NAME_PLATE;
        RetroStyle.drawRedFrame(g, r.x, r.y, r.width, r.height);
        g.setFont(RetroStyle.font(34));
        RetroStyle.shadowText(g, "No." + String.format("%03d", p.getId()), r.x + 24, r.y + 43, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        RetroStyle.shadowText(g, p.getName().toUpperCase(), r.x + 130, r.y + 43, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

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
        // Cuadricula suave dentro del recuadro del sprite
        g.setColor(new Color(0xEFEBDD));
        for (int i = 20; i < r.width - 6; i += 20) {
            g.fillRect(r.x + i, r.y + 6, 1, r.height - 12);
            g.fillRect(r.x + 6, r.y + i, r.width - 12, 1);
        }
        if (!spriteReady) {
            return; // Todavia se esta leyendo el GIF
        }
        if (sprite == null) {
            g.setFont(RetroStyle.font(24));
            String text = "SIN SPRITE";
            int w = g.getFontMetrics().stringWidth(text);
            RetroStyle.shadowText(g, text, r.x + (r.width - w) / 2, r.y + r.height / 2 + 8, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            return;
        }
        // Todos los sprites se ajustan al mismo cuadro, sin importar el tamano original del GIF
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
            // Filas alternadas blanco / gris claro, como una tabla
            g.setColor(i % 2 == 0 ? RetroStyle.WHITE : new Color(0xEEEBE2));
            g.fillRect(r.x + 6, y, r.width - 12, rowH);
            RetroStyle.shadowText(g, labels[i], r.x + 14, y + 20, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            String value = String.valueOf(values[i]);
            int vw = g.getFontMetrics().stringWidth(value);
            RetroStyle.shadowText(g, value, lineX - 10 - vw, y + 20, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            RetroStyle.drawStatBar(g, barX, y + 6, barMax, 16, values[i], MAX_STAT);
        }
        // Linea que separa los numeros de las barras
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
            RetroStyle.shadowText(g, m.getName().toUpperCase(), r.x + 122, y + 26, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
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
