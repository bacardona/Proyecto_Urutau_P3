package com.pokemon.battle.ui;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.StatusCondition;
import com.pokemon.battle.service.MoveEvent;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Pantalla de combate con el mismo estilo que la Pokedex: usa RetroScreen (cuadricula,
 * barra azul, barra gris de teclas y boton "ESC VOLVER") y las piezas de RetroStyle.
 * Es codigo puro, sin diseñador. Se maneja con teclado (flechas, ENTER, ESC) y con mouse.
 *
 * Mantiene los mismos metodos publicos que la version anterior, asi que PveBattleFlow,
 * PvpBattleFlow y BattleDemo no cambian.
 */
public class BattleScreenForm extends RetroScreen {

    // Donde quedan los pies de cada pokemon (centro de su plataforma)
    private static final int RIVAL_X = 700;
    private static final int RIVAL_FEET_Y = 304;
    private static final int PLAYER_X = 250;
    private static final int PLAYER_FEET_Y = 434;
    // Cuanto se bajan los sprites desde el centro de la plataforma para que se vean
    // parados encima de ella y no en el borde de atras (parecian flotar)
    private static final int RIVAL_FEET_DROP = 16;
    private static final int PLAYER_FEET_DROP = 14;
    // Pokemon cuyo GIF los muestra flotando: se bajan un 5% extra de su alto
    private static final Set<String> FLOATING = Set.of("BUTTERFREE", "MEW", "VOLCARONA", "GENGAR", "FLYGON");
    private static final double FLOATING_DROP = 0.05;
    // Escala fija: cada pixel del GIF se dibuja como un cuadro de N x N.
    // Asi todos los pokemon usan la misma escala y se respeta su tamano real
    // (un Squirtle se ve mas pequeno que un Snorlax). El rival usa menos escala
    // porque esta mas lejos.
    // El de frente (rival) va 45% mas grande que la escala base 2 y
    // el de espalda (jugador) 33% mas grande que la escala base 3.
    private static final double RIVAL_SCALE = 2 * 1.45;
    private static final double PLAYER_SCALE = 3 * 1.33;
    // Tamano maximo por si algun GIF es muy grande y no cabe en el campo
    private static final int RIVAL_MAX = 230;
    private static final int PLAYER_MAX = 350;

    // Zonas de la pantalla, en coordenadas del tablero de 960x640
    private static final Rectangle FIELD = new Rectangle(24, 74, 912, 382);
    private static final Rectangle PROMPT_BOX = new Rectangle(24, 468, 424, 112);
    // Antes medía 372 de ancho; ahora 2/3 de eso (248). El espacio que sobra lo usa la caja de mensajes
    private static final Rectangle OPTIONS_BOX = new Rectangle(460, 468, 248, 112);
    private static final Rectangle DETAIL_BOX = new Rectangle(720, 468, 216, 112);

    private Combatant player;   // cambia cuando el jugador cambia de pokemon
    private Combatant rival;

    // HP que se dibuja ahora mismo: baja poco a poco hasta el HP real
    private double playerShownHp;
    private double rivalShownHp;
    private final Timer hpTimer;
    // Mientras se anima un turno, la vida baja hasta estos valores y no hasta la real
    private final Map<Combatant, Integer> hpTargets = new HashMap<>();
    private final BattleAnimator animator = new BattleAnimator(this);
    private final SpriteLoader sprites = SpriteLoader.shared();
    private final Timer animationTimer;
    private int frame;

    private List<String> promptLines = new ArrayList<>();

    // Modo ataques (usa los movimientos del pokemon) o modo opciones (textos libres)
    private boolean movesMode = true;
    private String[] choices = new String[0];
    private IntConsumer onChoice = index -> {
    };
    private Runnable onBack = null;   // null = no hay a donde volver (no se muestra ESC VOLVER)
    private int selected = 0;
    private boolean inputEnabled = true;

    public BattleScreenForm(Combatant player, Combatant rival) {
        super("COMBATE");
        this.player = player;
        this.rival = rival;
        this.playerShownHp = player.getHitPoints().getCurrent();
        this.rivalShownHp = rival.getHitPoints().getCurrent();

        this.hpTimer = new Timer(30, e -> animateHp());
        this.animationTimer = new Timer(1000 / GifAnimation.FPS, e -> {
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
        requestSprites();
        animator.syncHidden(player, rival);

        bindKey("UP", () -> moveSelection(-1, 0));
        bindKey("DOWN", () -> moveSelection(1, 0));
        bindKey("LEFT", () -> moveSelection(0, -1));
        bindKey("RIGHT", () -> moveSelection(0, 1));
        bindKey("ENTER", this::confirm);

        // Mouse: pasar encima marca la opcion y cambia el cursor
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                onHover(e.getX(), e.getY());
            }
        });

        showPrompt();
    }

    private void requestSprites() {
        sprites.getOrLoad(player.getName(), true, this::repaint);
        sprites.getOrLoad(rival.getName(), false, this::repaint);
    }

    // ------------------------------------------------------------------ API que usan los flujos

    public JPanel getRootPanel() {
        return this;
    }

    /**
     * Lo que pasa cuando el jugador confirma un ataque (recibe su posicion 0 a 3).
     */
    public void setOnMoveChosen(IntConsumer onMoveChosen) {
        this.onChoice = onMoveChosen;
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
        repaint();
    }

    /**
     * Cambia los pokemon que se ven en el campo (por un cambio o un reemplazo).
     * Si son los mismos, solo anima las barras de vida.
     */
    public void setCombatants(Combatant player, Combatant rival) {
        if (this.player == player && this.rival == rival) {
            refresh();
            return;
        }
        this.player = player;
        this.rival = rival;
        this.playerShownHp = player.getHitPoints().getCurrent();
        this.rivalShownHp = rival.getHitPoints().getCurrent();
        requestSprites();
        animator.syncHidden(player, rival);
        this.selected = 0;
        repaint();
    }

    public void refresh() {
        if (!hpTimer.isRunning()) {
            hpTimer.start();
        }
        repaint();
    }

    /**
     * Juega el turno con animaciones: los mensajes salen uno por uno, cada ataque
     * se ve sobre su objetivo y la vida baja despues del golpe. Vuelo y Excavar
     * esconden al pokemon hasta que ataca.
     *
     * @param messages mensajes del turno
     * @param events   ataques del turno, en orden (BattleController.getLastRoundEvents)
     * @param onDone   que hacer cuando termina todo
     */
    public void playTurn(List<String> messages, List<MoveEvent> events, Runnable onDone) {
        animator.play(messages, events, onDone);
    }

    public void showPrompt() {
        promptLines = new ArrayList<>();
        promptLines.add("¿QUE HARA " + player.getName().toUpperCase() + "?");
        repaint();
    }

    public void showMessages(List<String> messages) {
        promptLines = new ArrayList<>();
        for (String m : messages) {
            if (!m.isBlank()) {
                promptLines.add(m.toUpperCase());
            }
        }
        repaint();
    }

    /**
     * Muestra los ataques del pokemon activo. ESC o el boton VOLVER ejecutan onBack.
     */
    public void showMoves(IntConsumer onMove, Runnable onBack) {
        this.movesMode = true;
        this.onChoice = onMove;
        this.onBack = onBack;
        this.selected = 0;
        this.inputEnabled = true;
        showPrompt();
    }

    /**
     * Muestra hasta 4 opciones de texto (menu de acciones, equipo, confirmaciones).
     * ESC o el boton VOLVER ejecutan onBack; si es null, no hay a donde volver.
     */
    public void showChoices(String prompt, String[] labels, IntConsumer onChosen, Runnable onBack) {
        this.movesMode = false;
        this.choices = labels;
        this.onChoice = onChosen;
        this.onBack = onBack;
        this.selected = 0;
        this.inputEnabled = true;
        promptLines = new ArrayList<>();
        promptLines.add(prompt.toUpperCase());
        repaint();
    }

    // ------------------------------------------------------------------ RetroScreen

    @Override
    protected void goBack() {
        if (inputEnabled && onBack != null) {
            onBack.run();
        }
    }

    @Override
    protected void paintFooterButtons(Graphics2D g) {
        if (inputEnabled && onBack != null) {
            super.paintFooterButtons(g);   // boton "ESC VOLVER"
        }
        RetroStyle.drawArrowKeyButton(g, true, "MOVER", 214, 598, 170, 36);
        RetroStyle.drawKeyButton(g, "ENTER  ELEGIR", 404, 598, 190, 36);
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        if (!inputEnabled) {
            return;
        }
        int index = optionAt(x, y);
        if (index >= 0) {
            selected = index;
            repaint();
            onChoice.accept(index);
        }
    }

    @Override
    protected void paintContent(Graphics2D g) {
        drawField(g);
        drawPromptBox(g);
        drawOptionsBox(g);
        drawDetailBox(g);
    }

    // ------------------------------------------------------------------ teclado y mouse

    private void confirm() {
        if (inputEnabled && selected < optionCount()) {
            onChoice.accept(selected);
        }
    }

    // Las opciones estan en cuadricula 2x2: se mueve por fila y columna
    private void moveSelection(int dRow, int dCol) {
        if (!inputEnabled) {
            return;
        }
        int row = Math.floorMod(selected / 2 + dRow, 2);
        int col = Math.floorMod(selected % 2 + dCol, 2);
        int target = row * 2 + col;
        if (target < optionCount()) {
            selected = target;
            repaint();
        }
    }

    private void onHover(int px, int py) {
        int[] p = toBoardPoint(px, py);
        int index = inputEnabled ? optionAt(p[0], p[1]) : -1;
        boolean overBack = inputEnabled && onBack != null && BACK_BUTTON.contains(p[0], p[1]);
        if (index >= 0 && index != selected) {
            selected = index;
            repaint();
        }
        setCursor(index >= 0 || overBack
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
    }

    // Convierte un punto de la ventana a coordenadas del tablero de 960x640
    private int[] toBoardPoint(int x, int y) {
        double scale = Math.min(getWidth() / (double) BOARD_W, getHeight() / (double) BOARD_H);
        double offsetX = (getWidth() - BOARD_W * scale) / 2;
        double offsetY = (getHeight() - BOARD_H * scale) / 2;
        return new int[]{(int) ((x - offsetX) / scale), (int) ((y - offsetY) / scale)};
    }

    private int optionCount() {
        return movesMode ? player.getPokemon().getMoves().size() : choices.length;
    }

    private String optionText(int i) {
        return movesMode ? player.getPokemon().getMove(i).getName().toUpperCase() : choices[i];
    }

    private Rectangle optionCell(int i) {
        int col = i % 2;
        int row = i / 2;
        return new Rectangle(OPTIONS_BOX.x + 12 + col * 116, OPTIONS_BOX.y + 10 + row * 48, 108, 44);
    }

    private int optionAt(int x, int y) {
        for (int i = 0; i < optionCount() && i < 4; i++) {
            if (optionCell(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ animacion de la vida

    private void animateHp() {
        playerShownHp = step(playerShownHp, targetHp(player));
        rivalShownHp = step(rivalShownHp, targetHp(rival));
        repaint();
        if (hpSettled()) {
            hpTimer.stop();
        }
    }

    private int targetHp(Combatant c) {
        Integer target = hpTargets.get(c);
        return target != null ? target : c.getHitPoints().getCurrent();
    }

    // ------------------------------------------------------------------ lo que usa BattleAnimator

    Combatant playerCombatant() {
        return player;
    }

    Combatant rivalCombatant() {
        return rival;
    }

    int feetXOf(Combatant c) {
        return c == player ? PLAYER_X : RIVAL_X;
    }

    // Donde quedan dibujados los pies del sprite (ya bajados sobre la plataforma)
    int feetYOf(Combatant c) {
        boolean back = c == player;
        return groundOf(c, back, drawnSize(c, back).height);
    }

    // Altura de los pies: centro de la plataforma + la bajada de cada lado
    // (+5% del alto si el pokemon flota), sin salirse del campo
    private int groundOf(Combatant c, boolean back, int h) {
        int ground = back ? PLAYER_FEET_Y + PLAYER_FEET_DROP : RIVAL_FEET_Y + RIVAL_FEET_DROP;
        if (c != null && FLOATING.contains(c.getName().trim().toUpperCase())) {
            ground += (int) Math.round(h * FLOATING_DROP);
        }
        return Math.min(ground, FIELD.y + FIELD.height - 2);
    }

    // Alto con el que se dibuja el pokemon (sirve para centrar los efectos de ataque)
    int spriteHeightOf(Combatant c) {
        return drawnSize(c, c == player).height;
    }

    // Lado mas largo con el que se dibuja el pokemon (sirve para el tamano de los efectos)
    int spriteSizeOf(Combatant c) {
        Dimension d = drawnSize(c, c == player);
        return Math.max(d.width, d.height);
    }

    // Tamano en pantalla del sprite: tamano real del GIF por la escala fija,
    // sin pasar del maximo. Si el GIF aun no carga, devuelve un tamano aproximado.
    private Dimension drawnSize(Combatant c, boolean back) {
        double scale = back ? PLAYER_SCALE : RIVAL_SCALE;
        int max = back ? PLAYER_MAX : RIVAL_MAX;
        GifAnimation sprite = (c != null && sprites.isLoaded(c.getName(), back))
                ? sprites.getOrLoad(c.getName(), back, this::repaint) : null;
        if (sprite == null) {
            int guess = (int) (60 * scale);
            return new Dimension(guess, guess);
        }
        double factor = scale;
        if (sprite.width() * factor > max || sprite.height() * factor > max) {
            factor = Math.min(max / (double) sprite.width(), max / (double) sprite.height());
        }
        return new Dimension((int) Math.round(sprite.width() * factor),
                (int) Math.round(sprite.height() * factor));
    }

    void clearLines() {
        promptLines = new ArrayList<>();
        repaint();
    }

    void addLine(String message) {
        promptLines.add(message.toUpperCase());
        repaint();
    }

    // La vida se queda donde se ve ahora hasta que el animador diga a donde bajar
    void freezeHp() {
        hpTargets.put(player, (int) Math.round(playerShownHp));
        hpTargets.put(rival, (int) Math.round(rivalShownHp));
    }

    void setHpTarget(Combatant c, int hp) {
        if (c == player || c == rival) {
            hpTargets.put(c, Math.max(0, hp));
            refresh();
        }
    }

    // Vuelve a seguir la vida real
    void releaseHp() {
        hpTargets.clear();
        refresh();
    }

    boolean hpSettled() {
        return playerShownHp == targetHp(player) && rivalShownHp == targetHp(rival);
    }

    private double step(double shown, int target) {
        double diff = target - shown;
        if (Math.abs(diff) < 0.5) {
            return target;
        }
        return shown + Math.signum(diff) * Math.max(0.5, Math.abs(diff) * 0.15);
    }

    // ------------------------------------------------------------------ dibujo: campo de batalla

    private void drawField(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.clip(new RoundRectangle2D.Double(FIELD.x, FIELD.y, FIELD.width, FIELD.height, 16, 16));

        // Fondo pixel art (cielo, colinas, arboles, pasto y plataformas)
        BufferedImage background = BattleBackground.get(FIELD.width, FIELD.height,
                RIVAL_X - FIELD.x, RIVAL_FEET_Y - FIELD.y, PLAYER_X - FIELD.x, PLAYER_FEET_Y - FIELD.y);
        RetroStyle.drawPixelImage(g, background, FIELD.x, FIELD.y,
                background.getWidth() * BattleBackground.SCALE, background.getHeight() * BattleBackground.SCALE);

        // Sprites animados con los pies sobre la sombra (rival de frente, jugador de espalda)
        drawCombatant(g, rival, false, RIVAL_X);
        drawCombatant(g, player, true, PLAYER_X);

        // Efectos de los ataques encima de los pokemon
        animator.drawEffects(g);

        // Cuadros de informacion
        drawInfoBox(g, 48, 92, 340, 84, rival, rivalShownHp, false, true);
        drawInfoBox(g, 548, 350, 364, 92, player, playerShownHp, true, false);

        g.setClip(oldClip);
        g.setColor(RetroStyle.OUTLINE);
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(FIELD.x, FIELD.y, FIELD.width, FIELD.height, 16, 16);
        g.setStroke(new BasicStroke(1));
    }

    // Todos los pokemon usan la misma escala fija (ver drawnSize), asi un pokemon
    // pequeno se ve pequeno y uno grande se ve grande. Los pies quedan sobre la sombra.
    private void drawCombatant(Graphics2D g, Combatant c, boolean back, int cx) {
        // Un debilitado se sigue viendo hasta que termina su animacion de caer
        if (c == null || (c.isFainted() && !animator.isStanding(c)) || animator.isHidden(c)) {
            return;
        }
        String name = c.getName();
        if (!sprites.isLoaded(name, back)) {
            // Todavia se esta leyendo el GIF; al terminar se vuelve a dibujar
            sprites.getOrLoad(name, back, this::repaint);
            return;
        }
        GifAnimation sprite = sprites.getOrLoad(name, back, this::repaint);
        if (sprite == null) {
            return;
        }
        Dimension size = drawnSize(c, back);
        int w = size.width;
        int h = size.height;
        int feetY = groundOf(c, back, h);
        BattleAnimator.Pose pose = animator.pose(c);
        if (pose == null) {
            RetroStyle.drawPixelImage(g, sprite.frame(frame), cx - w / 2, feetY - h, w, h);
            return;
        }
        if (!pose.isVisibleNow()) {
            return;   // parpadeo al recibir el golpe
        }
        int x = cx - w / 2 + (int) pose.dx;
        int ground = feetY + (int) pose.dy;
        Shape oldClip = g.getClip();
        Composite oldComposite = BattleAnimator.applyAlpha(g, pose);
        if (pose.sink > 0) {
            // Se hunde: lo que pasa debajo de sus pies no se dibuja
            g.clipRect(FIELD.x, FIELD.y, FIELD.width, ground - FIELD.y);
        }
        RetroStyle.drawPixelImage(g, sprite.frame(frame), x, ground - h + (int) (pose.sink * h), w, h);
        g.setComposite(oldComposite);
        g.setClip(oldClip);
    }

    // tailRight: la punta de la cola sale hacia la derecha (rival) o hacia la izquierda (jugador),
    // siempre apuntando al pokemon al que pertenece el cuadro
    private void drawInfoBox(Graphics2D g, int x, int y, int w, int h, Combatant c,
                             double shownHp, boolean showNumbers, boolean tailRight) {
        drawTail(g, x, y, w, h, tailRight);
        RetroStyle.drawPanel(g, x, y, w, h);
        int pad = 18;

        // Nombre y nivel
        g.setFont(RetroStyle.font(32));
        RetroStyle.shadowText(g, c.getName().toUpperCase(), x + pad, y + 34,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        String level = "Nv" + c.getPokemon().getLevel();
        int levelW = g.getFontMetrics().stringWidth(level);
        RetroStyle.shadowText(g, level, x + w - pad - levelW, y + 34,
                RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        // Etiqueta HP y barra
        g.setFont(RetroStyle.font(24));
        RetroStyle.shadowText(g, "HP", x + pad, y + 60, RetroStyle.RED, RetroStyle.SHADOW_LIGHT);
        int barX = x + pad + 38;
        int barW = w - pad * 2 - 38;
        int barY = y + 46;
        int barH = 14;
        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, barH, barH);
        g.setColor(RetroStyle.DARK);
        g.fillRoundRect(barX, barY, barW, barH, barH - 4, barH - 4);
        double ratio = shownHp / Math.max(1, c.getHitPoints().getMax());
        ratio = Math.max(0, Math.min(1, ratio));
        if (ratio > 0) {
            g.setColor(barColor(ratio));
            g.fillRoundRect(barX, barY, Math.max(barH, (int) (barW * ratio)), barH, barH - 4, barH - 4);
        }

        // Estado (izquierda) y HP en numeros (derecha, solo el jugador)
        int textY = y + h - 10;
        String status = statusText(c.getStatus());
        if (!status.isEmpty()) {
            RetroStyle.shadowText(g, status, x + pad, textY, RetroStyle.BLUE_DARK, RetroStyle.SHADOW_LIGHT);
        }
        if (showNumbers) {
            String hp = (int) Math.ceil(shownHp) + " / " + c.getHitPoints().getMax();
            int hpW = g.getFontMetrics().stringWidth(hp);
            RetroStyle.shadowText(g, hp, x + w - pad - hpW, textY, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }
    }

    // La "flechita" del cuadro: una franja oscura bajo el cuadro que termina en punta hacia el pokemon
    private void drawTail(Graphics2D g, int x, int y, int w, int h, boolean tailRight) {
        int[] xs;
        if (tailRight) {
            xs = new int[]{x + 16, x + w - 8, x + w + 44, x + 16};
        } else {
            xs = new int[]{x + w - 16, x + 8, x - 44, x + w - 16};
        }
        int[] ys = new int[]{y + h - 8, y + h - 8, y + h + 14, y + h + 14};
        g.setColor(new Color(26, 31, 43, 150));
        g.fillPolygon(xs, ys, 4);
    }

    private Color barColor(double ratio) {
        if (ratio > 0.5) {
            return new Color(0x3DA35D);
        }
        if (ratio > 0.2) {
            return new Color(0xE0A526);
        }
        return new Color(0xC0392B);
    }

    private String statusText(StatusCondition s) {
        switch (s) {
            case PARALYSIS:
                return "PARALIZADO";
            case SLEEP:
                return "DORMIDO";
            case POISON:
                return "ENVENENADO";
            case BURN:
                return "QUEMADO";
            case FREEZE:
                return "CONGELADO";
            default:
                return "";
        }
    }

    // ------------------------------------------------------------------ dibujo: cajas de abajo

    // Caja roja de la izquierda: mensajes y pregunta ("¿QUE HARA ...?")
    private void drawPromptBox(Graphics2D g) {
        RetroStyle.drawRedFrame(g, PROMPT_BOX.x, PROMPT_BOX.y, PROMPT_BOX.width, PROMPT_BOX.height);
        g.setFont(RetroStyle.font(22));
        List<String> lines = wrap(g, promptLines, PROMPT_BOX.width - 44);
        int lineHeight = 24;
        int maxLines = 3;
        int first = Math.max(0, lines.size() - maxLines);   // si hay de mas, se ven los ultimos
        int y = PROMPT_BOX.y + 34;
        for (int i = first; i < lines.size(); i++) {
            RetroStyle.shadowText(g, lines.get(i), PROMPT_BOX.x + 22, y,
                    RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
            y += lineHeight;
        }
    }

    private List<String> wrap(Graphics2D g, List<String> lines, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            StringBuilder current = new StringBuilder();
            for (String word : line.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (current.length() == 0 || fm.stringWidth(candidate) <= maxWidth) {
                    current = new StringBuilder(candidate);
                } else {
                    out.add(current.toString());
                    current = new StringBuilder(word);
                }
            }
            out.add(current.toString());
        }
        return out;
    }

    // Caja del centro: hasta 4 opciones en cuadricula 2x2
    private void drawOptionsBox(Graphics2D g) {
        RetroStyle.drawPanel(g, OPTIONS_BOX.x, OPTIONS_BOX.y, OPTIONS_BOX.width, OPTIONS_BOX.height);
        int count = optionCount();
        for (int i = 0; i < 4; i++) {
            Rectangle cell = optionCell(i);
            if (i >= count) {
                if (movesMode) {
                    drawFitted(g, "-", cell, RetroStyle.SHADOW_LIGHT);
                }
                continue;
            }
            boolean isSelected = inputEnabled && i == selected;
            if (isSelected) {
                RetroStyle.drawSelectedRow(g, cell.x, cell.y, cell.width, cell.height);
            }
            RetroStyle.drawListDot(g, cell.x + 16, cell.y + cell.height / 2, isSelected);
            drawFitted(g, optionText(i), cell, inputEnabled ? RetroStyle.DARK : RetroStyle.SHADOW_LIGHT);
        }
    }

    // Escribe el texto dentro de la celda: primero intenta una sola linea, despues dos lineas
    // (por ejemplo "PISTOLA / AGUA") y solo si no cabe reduce mas la letra
    private void drawFitted(Graphics2D g, String text, Rectangle cell, Color color) {
        int textX = cell.x + 32;
        int maxWidth = cell.width - 32 - 6;

        for (int size = 24; size >= 16; size--) {
            g.setFont(RetroStyle.font(size));
            List<String> lines = wrapWords(g.getFontMetrics(), text, maxWidth, 1);
            if (lines != null) {
                drawLines(g, lines, textX, cell, color);
                return;
            }
        }
        for (int size = 18; size >= 13; size--) {
            g.setFont(RetroStyle.font(size));
            List<String> lines = wrapWords(g.getFontMetrics(), text, maxWidth, 2);
            if (lines != null) {
                drawLines(g, lines, textX, cell, color);
                return;
            }
        }
        g.setFont(RetroStyle.font(14));
        List<String> single = new ArrayList<>();
        single.add(text);
        drawLines(g, single, textX, cell, color);
    }

    private void drawLines(Graphics2D g, List<String> lines, int x, Rectangle cell, Color color) {
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getAscent() + fm.getDescent();
        int top = cell.y + (cell.height - lineHeight * lines.size()) / 2;
        for (int i = 0; i < lines.size(); i++) {
            RetroStyle.shadowText(g, lines.get(i), x, top + i * lineHeight + fm.getAscent(),
                    color, RetroStyle.WHITE);
        }
    }

    // Parte el texto por palabras. Devuelve null si necesita mas lineas de las permitidas
    // o si una sola palabra es mas ancha que el espacio
    private List<String> wrapWords(FontMetrics fm, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (fm.stringWidth(word) > maxWidth) {
                return null;
            }
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        lines.add(current.toString());
        return lines.size() <= maxLines ? lines : null;
    }

    // Caja de la derecha: info del ataque elegido (tipo, categoria y potencia).
    // Los tipos del pokemon no se muestran en ninguna parte del combate.
    private void drawDetailBox(Graphics2D g) {
        RetroStyle.drawPanel(g, DETAIL_BOX.x, DETAIL_BOX.y, DETAIL_BOX.width, DETAIL_BOX.height);
        if (!movesMode || !inputEnabled || selected >= optionCount()) {
            return;   // sin un ataque resaltado (o fuera del menu de ataques) la caja queda vacia
        }
        Move move = player.getPokemon().getMove(selected);
        int x = DETAIL_BOX.x + 18;
        int y = DETAIL_BOX.y;

        // Fila 1: tipo del ataque
        g.setFont(RetroStyle.font(20));
        RetroStyle.shadowText(g, "TIPO", x, y + 32, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        RetroStyle.drawTypeBadge(g, move.getType(), x + 92, y + 11, 90, 28);

        // Fila 2: categoria (estado, fisico o especial)
        RetroStyle.shadowText(g, "CATEGORIA", x, y + 66, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        RetroStyle.drawCategory(g, move.getCategory(), x + 116, y + 45, 56, 28);

        // Fila 3: potencia
        RetroStyle.shadowText(g, "POTENCIA " + (move.getPower() > 0 ? String.valueOf(move.getPower()) : "-"),
                x, y + 100, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
    }
}
