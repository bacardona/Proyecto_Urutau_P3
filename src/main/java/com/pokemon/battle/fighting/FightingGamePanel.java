
package com.pokemon.battle.fighting;

import com.pokemon.battle.gamepad.PadSnapshot;
import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.RetroStyle;
import com.pokemon.battle.ui.SpriteLoader;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.IntFunction;

public class FightingGamePanel extends JPanel {

    private static final boolean DEBUG_KO_ENABLED = Boolean.getBoolean("pokemon.fighting.debug");

    // ==========================================
    // 1. DIMENSIONES DEL ESCENARIO
    // ==========================================

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final long SIMULATION_STEP_NANOS = 1_000_000_000L / 60;

    // ==========================================
    // 2. MENU DE PAUSA
    // ==========================================

    private static final String[] PAUSE_OPTIONS = {
            "CONTINUAR",
            "CONTROLES",
            "RESUMEN",
            "MANDO",          // el texto real lo da pauseOptionLabel (lado izquierdo o derecho)
            "VOLVER AL MENU"
    };

    /**
     * Lado que maneja el mando 1. Normal: mando 1 = jugador izquierdo, mando 2 = derecho.
     * Cambiado: al reves, asi uno puede jugar con el teclado de la izquierda y el mando a la derecha.
     * Es static para que se recuerde entre un combate y otro.
     */
    private static boolean padsSwapped;

    private static final int MENU_W = 380;
    private static final int MENU_X = (WIDTH - MENU_W) / 2;
    private static final int MENU_Y = 265;
    private static final int OPTION_H = 43;
    private static final int OPTION_GAP = 7;

    private enum PausePage {
        MAIN,
        CONTROLS,
        SUMMARY
    }

    // ==========================================
    // 3. VARIABLES
    // ==========================================

    private final FreeFightSetup setup;
    private FightingGameSession session;
    private final FightingRenderer renderer =
            new FightingRenderer(SpriteLoader.shared());
    private final FightingInput input = new FightingInput();

    private final Runnable onBack;
    private final Timer animationTimer;
    private final Timer simulationTimer;
    // Mando: se lee aparte de la simulacion para que tambien funcione en pausa y resultados
    private final Timer gamepadTimer;
    private final Timer resultsTimer;
    private final GamepadFightControls gamepadControls = new GamepadFightControls();
    private IntFunction<PadSnapshot> gamepadSource;
    private final boolean[] padConnected = new boolean[2];

    private int frameIndex = 0;
    private long lastSimulationNanos;
    private long simulationAccumulatorNanos;
    private PausePage pausePage = PausePage.MAIN;
    private int selectedOption = 0;
    private int selectedResultOption = 0;

    // ==========================================
    // 4. CONSTRUCTOR
    // ==========================================

    public FightingGamePanel(Runnable onBack) {
        this(null, onBack);
    }

    public FightingGamePanel(FreeFightSetup setup, Runnable onBack) {

        this.setup = setup;
        this.session = createSession();
        this.onBack = Objects.requireNonNull(onBack);

        // Franjas de arriba y abajo cuando la ventana es 960x640 (el combate es 16:9)
        setBackground(new Color(0x1A1F2B));
        setFocusable(true);

        // Temporizador de las animaciones.
        animationTimer = new Timer(
                1000 / GifAnimation.FPS,
                e -> {
                if (session.getState() == FightingGameSession.State.RUNNING) {
                        frameIndex++;
                        repaint();
                    }
                }
        );

        simulationTimer = new Timer(8, e -> updateSimulation());
        gamepadTimer = new Timer(16, e -> readGamepads());
        // Despues del combate: sigue animando la pantalla final (dialogo, confeti, risa)
        resultsTimer = new Timer(33, e -> {
            if (session.getState() != FightingGameSession.State.FINISHED || !isShowing()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            renderer.fx().tickResults(0.033);
            repaint();
        });

        // Iniciar o detener las animaciones
        // cuando se muestra u oculta la pantalla.
        addHierarchyListener(e -> {

            if ((e.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) != 0) {

                if (isShowing()) {

                    session = createSession();
                    pausePage = PausePage.MAIN;
                    selectedOption = 0;
                    selectedResultOption = 0;
                    frameIndex = 0;

                    animationTimer.start();
                    input.clear();
                    simulationAccumulatorNanos = 0;
                    lastSimulationNanos = System.nanoTime();
                    simulationTimer.start();
                    gamepadControls.reset();
                    gamepadTimer.start();

                } else {

                    animationTimer.stop();
                    simulationTimer.stop();
                    gamepadTimer.stop();
                    gamepadControls.reset();
                    input.clear();
                    FightingSound.get().stopMusic();
                }

                repaint();
            }
        });

        // Controles del menu.
        bindKey("ESCAPE", this::handleEscape);
        bindKey("UP", () -> {
            if (session.getState() == FightingGameSession.State.PAUSED) {
                moveSelection(-1);
            } else if (session.getState() == FightingGameSession.State.RUNNING) {
                input.setPlayerTwoJump(true);
            } else {
                moveResultSelection(-1);
            }
        });
        bindKey("DOWN", () -> {
            if (session.getState() == FightingGameSession.State.PAUSED) moveSelection(1);
            else if (session.getState() == FightingGameSession.State.FINISHED) moveResultSelection(1);
            else if (session.getState() == FightingGameSession.State.RUNNING) input.setPlayerTwoBlock(true);
        });
        bindKeyRelease("DOWN", input::setPlayerTwoBlock);
        bindKey("ENTER", this::activateSelected);
        // M: silenciar o activar los sonidos del combate
        // Silenciar con F1 (lejos de las teclas de ataque, para no tocarla sin querer)
        bindKey("F1", () -> FightingSound.get().setMuted(!FightingSound.get().isMuted()));
        // Teclas de prueba, solo con -Dpokemon.fighting.debug=true (VM options de IntelliJ):
        // F6/F7 K.O. al jugador 1/2, F8/F9 llenan su barra EVO, F10 Mewtwo normal, F11 Mewtwo dificil
        if (DEBUG_KO_ENABLED) {
            bindKey("F6", () -> debugDefeat(0));
            bindKey("F7", () -> debugDefeat(1));
            bindKey("F8", () -> session.debugFillActiveEnergy(0));
            bindKey("F9", () -> session.debugFillActiveEnergy(1));
            bindKey("F10", () -> debugStartBoss(false));
            bindKey("F11", () -> debugStartBoss(true));
        }

        bindHeldKey("A", input::setPlayerOneLeft);
        bindHeldKey("D", input::setPlayerOneRight);
        bindHeldKey("LEFT", input::setPlayerTwoLeft);
        bindHeldKey("RIGHT", input::setPlayerTwoRight);
        bindHeldKey("W", input::setPlayerOneJump);
        bindHeldKey("S", input::setPlayerOneBlock);
        bindKeyRelease("UP", input::setPlayerTwoJump);
        bindHeldKey("F", input::setPlayerOneAttackOne);
        bindHeldKey("G", input::setPlayerOneAttackTwo);
        bindHeldKey("H", input::setPlayerOneAttackThree);
        bindHeldKey("V", input::setPlayerOneAttackFour);
        bindHeldKey("R", input::setPlayerOneEvolution);
        bindHeldKey("T", input::setPlayerOneUltimate);
        bindHeldKey("I", input::setPlayerTwoAttackOne);
        bindHeldKey("J", input::setPlayerTwoAttackTwo);
        bindHeldKey("K", input::setPlayerTwoAttackThree);
        bindHeldKey("L", input::setPlayerTwoAttackFour);
        bindHeldKey("P", input::setPlayerTwoEvolution);
        bindHeldKey("O", input::setPlayerTwoUltimate);
        // Cambiar de Pokemon tocando la pared
        bindHeldKey("Q", input::setPlayerOneSwitch);
        bindHeldKey("U", input::setPlayerTwoSwitch);

        // Resaltar el boton de pausa al pasar el raton
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean hover = session.getState() == FightingGameSession.State.RUNNING && isOnPauseButton(e);
                renderer.setPauseHover(hover);
                setCursor(java.awt.Cursor.getPredefinedCursor(
                        hover ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
            }
        });

        // Control mediante raton.
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (session.getState() == FightingGameSession.State.FINISHED) {
                    handleResultClick(e);
                    return;
                }
                // Boton de pausa entre las dos barras de vida
                if (session.getState() == FightingGameSession.State.RUNNING) {
                    if (isOnPauseButton(e)) handleEscape();
                    return;
                }
                if (session.getState() != FightingGameSession.State.PAUSED
                        || pausePage != PausePage.MAIN) {
                    return;
                }

                double scale = boardScale();

                if (scale <= 0) {
                    return;
                }

                // Convertir la posicion real
                // del raton a coordenadas del escenario.
                int x = (int) (
                        (e.getX() - boardOffsetX(scale))
                                / scale
                );

                int y = (int) (
                        (e.getY() - boardOffsetY(scale))
                                / scale
                );

                // Comprobar si el clic esta
                // dentro del ancho de los botones.
                if (x < MENU_X ||
                        x >= MENU_X + MENU_W) {
                    return;
                }

                // Buscar el boton seleccionado.
                for (int i = 0;
                     i < PAUSE_OPTIONS.length;
                     i++) {

                    int top = MENU_Y +
                            i * (OPTION_H + OPTION_GAP);

                    if (y >= top &&
                            y < top + OPTION_H) {

                        selectedOption = i;
                        activateSelected();
                        return;
                    }
                }
            }
        });
    }

    // ==========================================
    // 5. CONFIGURACION DEL TECLADO
    // ==========================================

    private void bindKey(String key, Runnable action) {

        String id = "fighting-" + key;

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), id);

        getActionMap().put(id, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (isShowing()) {
                    action.run();
                }
            }
        });
    }

    private void bindHeldKey(String key, java.util.function.Consumer<Boolean> setter) {
        String pressedId = "fighting-move-pressed-" + key;
        String releasedId = "fighting-move-released-" + key;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("pressed " + key), pressedId);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("released " + key), releasedId);
        getActionMap().put(pressedId, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing() && session.getState() == FightingGameSession.State.RUNNING) setter.accept(true);
            }
        });
        getActionMap().put(releasedId, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setter.accept(false);
            }
        });
    }

    private void bindKeyRelease(String key, java.util.function.Consumer<Boolean> setter) {
        String releasedId = "fighting-release-" + key;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("released " + key), releasedId);
        getActionMap().put(releasedId, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setter.accept(false);
            }
        });
    }

    /** De donde sale la foto de cada mando (la ventana la conecta al iniciar el juego). */
    public void setGamepadSource(IntFunction<PadSnapshot> source) {
        this.gamepadSource = source;
    }

    /**
     * Lee los dos mandos. En la pelea van a FightingInput (ver GamepadFightControls);
     * en pausa y en resultados mueven el menu: cruceta, A elige, B o START vuelve.
     */
    private void readGamepads() {
        if (gamepadSource == null) return;
        for (int player = 0; player < 2; player++) {
            PadSnapshot pad = gamepadSource.apply(padIndexFor(player));
            if (pad == null) pad = PadSnapshot.NONE;
            if (padConnected[player] != pad.connected()) {
                padConnected[player] = pad.connected();
                renderer.setPadConnected(player, pad.connected());
                repaint();
            }
            FightingGameSession.State state = session.getState();
            if (state == FightingGameSession.State.RUNNING) {
                boolean pause = gamepadControls.justPressed(player, pad, PadSnapshot::start);
                gamepadControls.apply(player, pad, input);
                if (pause) handleEscape();
                continue;
            }
            boolean up = gamepadControls.justPressed(player, pad, PadSnapshot::up);
            boolean down = gamepadControls.justPressed(player, pad, PadSnapshot::down);
            boolean confirm = gamepadControls.justPressed(player, pad, PadSnapshot::a);
            boolean back = gamepadControls.justPressed(player, pad, p -> p.b() || p.start());
            gamepadControls.remember(player, pad);
            if (state == FightingGameSession.State.PAUSED) {
                if (up) moveSelection(-1);
                if (down) moveSelection(1);
                if (confirm) activateSelected();
                else if (back) handleEscape();
            } else if (state == FightingGameSession.State.FINISHED) {
                if (up) moveResultSelection(-1);
                if (down) moveResultSelection(1);
                if (confirm) activateSelected();
            }
        }
    }

    /** Que mando (0 o 1) maneja a este jugador (0 = izquierda, 1 = derecha). */
    int padIndexFor(int player) {
        return padsSwapped ? 1 - player : player;
    }

    /** Cambia de lado los mandos. Primero suelta todo lo que tenian apretado. */
    void togglePadSide() {
        for (int player = 0; player < 2; player++) {
            gamepadControls.apply(player, PadSnapshot.NONE, input);
            padConnected[player] = false;
            renderer.setPadConnected(player, false);
        }
        padsSwapped = !padsSwapped;
        repaint();
    }

    /** Texto de cada boton de la pausa (el del mando dice a que lado esta). */
    private String pauseOptionLabel(int index) {
        if (index == 3) return padsSwapped ? "MANDO: LADO DERECHO" : "MANDO: LADO IZQUIERDO";
        return PAUSE_OPTIONS[index];
    }

    private void updateSimulation() {
        if (session.getState() != FightingGameSession.State.RUNNING) return;
        long now = System.nanoTime();
        long elapsed = Math.min(now - lastSimulationNanos, 250_000_000L);
        lastSimulationNanos = now;
        simulationAccumulatorNanos += elapsed;

        while (simulationAccumulatorNanos >= SIMULATION_STEP_NANOS) {
            // FightingFx avanza la simulacion y agrega efectos (y el congelado al golpear)
            renderer.fx().step(session, input, SIMULATION_STEP_NANOS / 1_000_000_000.0);
            simulationAccumulatorNanos -= SIMULATION_STEP_NANOS;
            if (session.getState() == FightingGameSession.State.FINISHED) {
                stopGameTimersAndClearInput();
                simulationAccumulatorNanos = 0;
                onMatchFinished();
                break;
            }
        }

        if (elapsed > 0) repaint();
    }

    // ==========================================
    // 6. PAUSAR Y REANUDAR
    // ==========================================

    private void handleEscape() {

        if (session.getState() == FightingGameSession.State.RUNNING) {

            // Abrir el menu de pausa.
            session.pause();
            pausePage = PausePage.MAIN;
            selectedOption = 0;

            animationTimer.stop();
            simulationTimer.stop();
            input.clear();

        } else if (session.getState() == FightingGameSession.State.PAUSED
                && pausePage != PausePage.MAIN) {

            // Regresar desde Controles o Resumen.
            pausePage = PausePage.MAIN;

        } else if (session.getState() == FightingGameSession.State.PAUSED) {

            // Cerrar el menu.
            resumeGame();
        }

        repaint();
    }

    private void resumeGame() {

        session.resume();
        pausePage = PausePage.MAIN;

        if (isShowing() && session.getState() == FightingGameSession.State.RUNNING) {
            input.clear();
            simulationAccumulatorNanos = 0;
            lastSimulationNanos = System.nanoTime();
            simulationTimer.start();
            animationTimer.start();
        }

        repaint();
    }

    // ==========================================
    // 7. NAVEGACION DEL MENU
    // ==========================================

    private void moveSelection(int delta) {

        if (session.getState() != FightingGameSession.State.PAUSED ||
                pausePage != PausePage.MAIN) {
            return;
        }

        selectedOption = Math.floorMod(
                selectedOption + delta,
                PAUSE_OPTIONS.length
        );

        repaint();
    }

    private void activateSelected() {

        if (session.getState() == FightingGameSession.State.FINISHED) {
            activateResultOption();
            return;
        }

        if (session.getState() != FightingGameSession.State.PAUSED) {
            return;
        }

        if (pausePage != PausePage.MAIN) {

            pausePage = PausePage.MAIN;
            repaint();
            return;
        }

        switch (selectedOption) {

            case 0 -> resumeGame();

            case 1 -> pausePage = PausePage.CONTROLS;

            case 2 -> pausePage = PausePage.SUMMARY;

            case 3 -> togglePadSide();

            case 4 -> {
                stopGameTimersAndClearInput();
                onBack.run();
            }

            default -> {
            }
        }

        repaint();
    }

    private void moveResultSelection(int delta) {
        if (session.getState() != FightingGameSession.State.FINISHED) return;
        selectedResultOption = Math.floorMod(selectedResultOption + delta, 2);
        repaint();
    }

    private void activateResultOption() {
        if (selectedResultOption == 0) {
            if (hardOffered) bossHard = true;
            if (bossOffered) bossHard = false;
            session = bossOffered || bossRetry || hardOffered ? createBossSession() : createSession();
            bossOffered = false;
            bossRetry = false;
            hardOffered = false;
            input.clear();
            frameIndex = 0;
            selectedResultOption = 0;
            simulationAccumulatorNanos = 0;
            lastSimulationNanos = System.nanoTime();
            if (isShowing()) {
                animationTimer.start();
                simulationTimer.start();
            }
        } else {
            // "AHORA NO" o volver: se pierde la revancha (otra vez 3 victorias seguidas)
            bossOffered = false;
            bossRetry = false;
            hardOffered = false;
            stopGameTimersAndClearInput();
            onBack.run();
        }
        repaint();
    }

    private boolean isOnPauseButton(MouseEvent event) {
        double scale = boardScale();
        if (scale <= 0) return false;
        int x = (int) ((event.getX() - boardOffsetX(scale)) / scale);
        int y = (int) ((event.getY() - boardOffsetY(scale)) / scale);
        return FightingHud.isOnPauseButton(x, y);
    }

    private void handleResultClick(MouseEvent event) {
        double scale = boardScale();
        int x = (int) ((event.getX() - boardOffsetX(scale)) / scale);
        int y = (int) ((event.getY() - boardOffsetY(scale)) / scale);
        if (x < FightingRenderer.RESULT_OPTION_X
                || x >= FightingRenderer.RESULT_OPTION_X + FightingRenderer.RESULT_OPTION_W) return;

        for (int option = 0; option < 2; option++) {
            int top = FightingRenderer.RESULT_OPTION_Y
                    + option * (FightingRenderer.RESULT_OPTION_H + FightingRenderer.RESULT_OPTION_GAP);
            if (y >= top && y < top + FightingRenderer.RESULT_OPTION_H) {
                selectedResultOption = option;
                activateResultOption();
                return;
            }
        }
    }

    private void stopGameTimersAndClearInput() {
        animationTimer.stop();
        simulationTimer.stop();
        input.clear();
    }

    // ------------------------------------------------------------ jefe secreto
    /** Victorias seguidas en Combate Libre para que aparezca Mewtwo. */
    static final int BOSS_WIN_STREAK = 3;
    // Rachas por entrenador mientras el juego esta abierto
    private static final java.util.Map<String, Integer> WIN_STREAKS = new java.util.HashMap<>();
    private boolean bossOffered;
    // Mewtwo gano: se puede reintentar sin volver a ganar 3 seguidas
    private boolean bossRetry;
    // Le ganaste a Mewtwo normal: te reta a la revancha dificil
    private boolean hardOffered;
    // La pelea de jefe que se crea (o reintenta) es la dificil
    private boolean bossHard;
    private int bossChallenger = -1;

    /** Cuenta la racha del ganador; a la tercera victoria seguida Mewtwo lo desafia. */
    private void onMatchFinished() {
        resultsTimer.start();
        FightingGameSession.Outcome outcome = session.getOutcome();
        if (session.isBossFight()) {
            // Gane o pierda contra Mewtwo, la racha vuelve a cero; si perdio puede reintentar
            WIN_STREAKS.remove(session.getTrainerName(1 - session.getBossIndex()));
            bossChallenger = 1 - session.getBossIndex();
            bossRetry = !session.challengerWon();
            bossHard = session.isHardBoss();
            hardOffered = session.challengerWon() && !session.isHardBoss();
            return;
        }
        if (outcome == null || outcome == FightingGameSession.Outcome.DRAW) return;
        int winner = outcome == FightingGameSession.Outcome.PLAYER_ONE_WINS ? 0 : 1;
        String winnerName = session.getTrainerName(winner);
        WIN_STREAKS.remove(session.getTrainerName(1 - winner));
        int streak = WIN_STREAKS.merge(winnerName, 1, Integer::sum);
        if (streak >= BOSS_WIN_STREAK) {
            bossOffered = true;
            bossChallenger = winner;
        }
    }

    /** El ganador (con su equipo y la vida llena) contra Mewtwo, en el mismo mapa. */
    private FightingGameSession createBossSession() {
        renderer.reset();
        int challenger = Math.max(0, bossChallenger);
        String name = session.getTrainerName(challenger);
        FightingStage stage = session.getStage();
        if (setup != null && setup.isComplete()) {
            var team = challenger == 0 ? setup.getTeamOne() : setup.getTeamTwo();
            return FightingGameSession.bossFight(name, team, challenger == 1, stage, bossHard);
        }
        return FightingGameSession.bossFightPrototype(name, new String[]{"PIKACHU", "BULBASAUR"},
                challenger == 1, stage, bossHard);
    }

    /** F10 / F11 con -Dpokemon.fighting.debug=true: Mewtwo (normal / dificil) aparece ya. */
    private void debugStartBoss(boolean hard) {
        bossChallenger = 0;
        bossHard = hard;
        hardOffered = false;
        bossRetry = false;
        session = createBossSession();
        bossOffered = false;
        input.clear();
        frameIndex = 0;
        selectedResultOption = 0;
        simulationAccumulatorNanos = 0;
        lastSimulationNanos = System.nanoTime();
        if (isShowing()) {
            animationTimer.start();
            simulationTimer.start();
        }
        repaint();
    }

    private FightingGameSession createSession() {
        renderer.reset();
        return setup != null && setup.isComplete()
                ? new FightingGameSession(setup)
                : new FightingGameSession();
    }

    private void debugDefeat(int playerIndex) {
        if (!DEBUG_KO_ENABLED || session.getState() != FightingGameSession.State.RUNNING) return;
        session.debugDefeatActiveFighter(playerIndex);
        if (session.getState() == FightingGameSession.State.FINISHED) {
            stopGameTimersAndClearInput();
            onMatchFinished();
        }
        repaint();
    }

    // ==========================================
    // 8. ESCALADO DEL ESCENARIO
    // ==========================================

    private double boardScale() {

        if (getWidth() <= 0 || getHeight() <= 0) {
            return 1.0;
        }

        return Math.min(
                getWidth() / (double) WIDTH,
                getHeight() / (double) HEIGHT
        );
    }

    private double boardOffsetX(double scale) {

        return (getWidth() - WIDTH * scale) / 2.0;
    }

    private double boardOffsetY(double scale) {

        return (getHeight() - HEIGHT * scale) / 2.0;
    }

    // ==========================================
    // 9. DIBUJAR TODA LA PANTALLA
    // ==========================================

    @Override
    protected void paintComponent(Graphics graphics) {

        super.paintComponent(graphics);

        Graphics2D g =
                (Graphics2D) graphics.create();

        double scale = boardScale();

        // Dibujar el escenario con su escala.
        Graphics2D sceneGraphics =
                (Graphics2D) g.create();

        sceneGraphics.translate(
                boardOffsetX(scale),
                boardOffsetY(scale)
        );

        sceneGraphics.scale(scale, scale);

        // Mantener los pixeles definidos.
        sceneGraphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        renderer.draw(sceneGraphics, session, frameIndex, this::repaint);
        // Las teclas de prueba (F6 a F11) funcionan sin mostrar ningun aviso en pantalla

        sceneGraphics.dispose();

        // ======================================
        // OSCURECIMIENTO Y MENU DE PAUSA
        // ======================================

        if (session.getState() == FightingGameSession.State.PAUSED
                || session.getState() == FightingGameSession.State.FINISHED) {

            // IMPORTANTE:
            // Este Graphics2D no tiene escalado.
            // Por eso oscurece toda la ventana,
            // incluyendo las franjas laterales.

            g.setColor(
                    new Color(10, 17, 28, 175)
            );

            g.fillRect(
                    0,
                    0,
                    getWidth(),
                    getHeight()
            );

            // Dibujamos el menu por encima
            // del oscurecimiento.
            Graphics2D menuGraphics =
                    (Graphics2D) g.create();

            menuGraphics.translate(
                    boardOffsetX(scale),
                    boardOffsetY(scale)
            );

            menuGraphics.scale(scale, scale);

            if (session.getState() == FightingGameSession.State.PAUSED) {
                drawPause(menuGraphics);
            } else {
                renderer.drawWinnerBackdrop(menuGraphics, session);
                renderer.drawResults(menuGraphics, session, selectedResultOption, bossOffered, hardOffered);
            }

            menuGraphics.dispose();
        }

        g.dispose();
    }

    // ==========================================
    // 15. MENU DE PAUSA
    // ==========================================

    private void drawPause(Graphics2D g) {

        int panelHeight = 400;

        int panelY = (HEIGHT - panelHeight) / 2;

        int centerX = WIDTH / 2;

        // Estilo arcade: sin recuadro, el texto va directo sobre el fondo oscurecido
        // (ver ArcadeMenuStyle). panelY solo sirve para ubicar los textos.

        // MENU PRINCIPAL.
        if (pausePage == PausePage.MAIN) {

            drawCentered(
                    g,
                    "PAUSA",
                    centerX,
                    panelY + 65,
                    45,
                    RetroStyle.DARK
            );

            for (int i = 0;
                 i < PAUSE_OPTIONS.length;
                 i++) {

                int y = MENU_Y +
                        i * (OPTION_H + OPTION_GAP);

                // Sin caja: la opcion elegida va en amarillo con flechas.
                // El clic sigue usando el mismo rectangulo de cada opcion.
                ArcadeMenuStyle.option(g, pauseOptionLabel(i), centerX, y + 29, 23,
                        i == selectedOption);
            }

            drawCentered(
                    g,
                    "FLECHAS + ENTER / CLIC",
                    centerX,
                    panelY + panelHeight - 25,
                    17,
                    RetroStyle.DARK
            );
        }

        // MENU DE CONTROLES.
        else if (pausePage == PausePage.CONTROLS) {

            drawCentered(
                    g,
                    "CONTROLES",
                    centerX,
                    panelY + 65,
                    36,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "JUGADOR 1: A / D, W, F / G / H / V",
                    centerX,
                    panelY + 112,
                    18,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "JUGADOR 2: FLECHAS, ARRIBA, I / J / K / L",
                    centerX,
                    panelY + 142,
                    17,
                    RetroStyle.DARK
            );

            drawCentered(g, "JUGADOR 1: R EVOLUCIONAR  |  JUGADOR 2: P EVOLUCIONAR",
                    centerX, panelY + 166, 16, RetroStyle.DARK);
            drawCentered(g, "JUGADOR 1: T ULTI  |  JUGADOR 2: O ULTI",
                    centerX, panelY + 188, 16, RetroStyle.DARK);
            drawCentered(g, "ESCUDO: J1 S  |  J2 ABAJO   (ESCUDO + LADO = ESQUIVA)",
                    centerX, panelY + 210, 16, RetroStyle.DARK);

            Fighter playerOne = session.getFighters().get(0);
            Fighter playerTwo = session.getFighters().get(1);
            String[] playerOneAttacks = attackSummary(playerOne, true);
            String[] playerTwoAttacks = attackSummary(playerTwo, false);
            drawCentered(g, playerOneAttacks[0], centerX, panelY + 232, 15, RetroStyle.DARK);
            drawCentered(g, playerOneAttacks[1], centerX, panelY + 254, 15, RetroStyle.DARK);
            drawCentered(g, playerTwoAttacks[0], centerX, panelY + 282, 15, RetroStyle.DARK);
            drawCentered(g, playerTwoAttacks[1], centerX, panelY + 304, 15, RetroStyle.DARK);

            drawCentered(g, "MANDO: A SALTO  X Y B RB ATAQUES  LB ESCUDO (+ LADO ESQUIVA)  LT EVO  RT ULTI  SELECT CAMBIO",
                    centerX, panelY + 328, 14, RetroStyle.DARK);
            drawCentered(g, "ESC O ENTER PARA VOLVER  |  F1 SONIDO", centerX,
                    panelY + 348, 16, RetroStyle.DARK);
        }

        // MENU DE RESUMEN.
        else if (pausePage == PausePage.SUMMARY) {

            drawCentered(
                    g,
                    "RESUMEN",
                    centerX,
                    panelY + 65,
                    36,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    session.getFighters().get(0).getName().toUpperCase()
                            + " VS " + session.getFighters().get(1).getName().toUpperCase(),
                    centerX,
                    panelY + 140,
                    24,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "COMBATE SIN INICIAR",
                    centerX,
                    panelY + 200,
                    22,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "ESTADISTICAS EN PROXIMAS ETAPAS",
                    centerX,
                    panelY + 260,
                    17,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "ESC O ENTER PARA VOLVER",
                    centerX,
                    panelY + 350,
                    18,
                    RetroStyle.DARK
            );
        }
    }

    private String[] attackSummary(Fighter fighter, boolean playerOne) {
        boolean pad = padConnected[playerOne ? 0 : 1];
        String keyOne = pad ? "X" : playerOne ? "F" : "I";
        String keyTwo = pad ? "Y" : playerOne ? "G" : "J";
        String keyThree = pad ? "B" : playerOne ? "H" : "K";
        String keyFour = pad ? "RB" : playerOne ? "V" : "L";
        String moveOne;
        String moveTwo;
        String moveThree;
        String moveFour;
        switch (fighter.getBasePokemonId()) {
            case "CHARIZARD" -> { moveOne = "ASCUAS"; moveTwo = "PLACAJE"; moveThree = "—"; moveFour = "—"; }
            case "BULBASAUR" -> { moveOne = "PLACAJE"; moveTwo = "LATIGO CEPA"; moveThree = "SOMNIFERO"; moveFour = "DESARROLLO"; }
            case "CHARMANDER" -> { moveOne = "PLACAJE"; moveTwo = "ASCUAS"; moveThree = "GRUNIDO"; moveFour = "VUELO"; }
            case "SQUIRTLE" -> { moveOne = "PLACAJE"; moveTwo = "PISTOLA AGUA"; moveThree = "LATIGO"; moveFour = "MORDISCO"; }
            case "EEVEE" -> { moveOne = "PLACAJE"; moveTwo = "RAPIDO"; moveThree = "GRUNIDO"; moveFour = "LATIGO"; }
            case "VULPIX" -> { moveOne = "RAPIDO"; moveTwo = "ASCUAS"; moveThree = "GRUNIDO"; moveFour = "COLMILLO IGNEO"; }
            case "SANDSHREW" -> { moveOne = "PLACAJE"; moveTwo = "EXCAVAR"; moveThree = "LATIGO"; moveFour = "AVALANCHA"; }
            case "GASTLY" -> { moveOne = "LENGUETAZO"; moveTwo = "BOLA SOMBRA"; moveThree = "HIPNOSIS"; moveFour = "CARA SUSTO"; }
            case "MACHOP" -> { moveOne = "PLACAJE"; moveTwo = "A BOCAJARRO"; moveThree = "GRUNIDO"; moveFour = "GOLPE CUERPO"; }
            case "ABRA" -> { moveOne = "CABEZAZO ZEN"; moveTwo = "PSIQUICO"; moveThree = "AMNESIA"; moveFour = "TELETRANSPORTE"; }
            case "PIKACHU" -> { moveOne = "RAPIDO"; moveTwo = "IMPACTRUENO"; moveThree = "LATIGO"; moveFour = "ONDA TRUENO"; }
            default -> { return new String[]{"ATAQUES NO DISPONIBLES", ""}; }
        }
        String prefix = playerOne ? "P1 " : "P2 ";
        return new String[]{prefix + keyOne + " " + moveOne + " / " + keyTwo + " " + moveTwo,
                prefix + keyThree + " " + moveThree + " / " + keyFour + " " + moveFour};
    }

    // ==========================================
    // 16. CENTRAR TEXTOS
    // ==========================================

    private void drawCentered(
            Graphics2D g,
            String text,
            int centerX,
            int baseline,
            int size,
            Color color
    ) {
        // Texto blanco con borde negro (estilo arcade del menu de pausa)
        ArcadeMenuStyle.text(g, text, centerX, baseline, size, color);
    }
}
