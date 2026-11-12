
package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.RetroStyle;
import com.pokemon.battle.ui.SpriteLoader;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class FightingGamePanel extends JPanel {

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
            "VOLVER AL MENU"
    };

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

    private final FightingGameSession session = new FightingGameSession();
    private final FightingRenderer renderer =
            new FightingRenderer(SpriteLoader.shared());
    private final FightingInput input = new FightingInput();

    private final Runnable onBack;
    private final Timer animationTimer;
    private final Timer simulationTimer;

    private int frameIndex = 0;
    private long lastSimulationNanos;
    private long simulationAccumulatorNanos;
    private boolean paused = false;

    private PausePage pausePage = PausePage.MAIN;
    private int selectedOption = 0;

    // ==========================================
    // 4. CONSTRUCTOR
    // ==========================================

    public FightingGamePanel(Runnable onBack) {

        this.onBack = Objects.requireNonNull(onBack);

        setBackground(new Color(0x98C9EF));
        setFocusable(true);

        // Temporizador de las animaciones.
        animationTimer = new Timer(
                1000 / GifAnimation.FPS,
                e -> {
                    if (!paused) {
                        frameIndex++;
                        repaint();
                    }
                }
        );

        simulationTimer = new Timer(8, e -> updateSimulation());

        // Iniciar o detener las animaciones
        // cuando se muestra u oculta la pantalla.
        addHierarchyListener(e -> {

            if ((e.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) != 0) {

                if (isShowing()) {

                    paused = false;
                    pausePage = PausePage.MAIN;
                    selectedOption = 0;
                    frameIndex = 0;

                    animationTimer.start();
                    input.clear();
                    simulationAccumulatorNanos = 0;
                    lastSimulationNanos = System.nanoTime();
                    simulationTimer.start();

                } else {

                    animationTimer.stop();
                    simulationTimer.stop();
                    input.clear();
                }

                repaint();
            }
        });

        // Controles del menu.
        bindKey("ESCAPE", this::handleEscape);
        bindKey("UP", () -> {
            if (paused) {
                moveSelection(-1);
            } else {
                input.setPlayerTwoJump(true);
            }
        });
        bindKey("DOWN", () -> moveSelection(1));
        bindKey("ENTER", this::activateSelected);

        bindHeldKey("A", input::setPlayerOneLeft);
        bindHeldKey("D", input::setPlayerOneRight);
        bindHeldKey("LEFT", input::setPlayerTwoLeft);
        bindHeldKey("RIGHT", input::setPlayerTwoRight);
        bindHeldKey("W", input::setPlayerOneJump);
        bindKeyRelease("UP", input::setPlayerTwoJump);
        bindHeldKey("J", input::setQuickAttack);
        bindHeldKey("K", input::setThunderShock);
        bindHeldKey("1", input::setEmber);
        bindHeldKey("2", input::setTackle);

        // Control mediante raton.
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (!paused ||
                        pausePage != PausePage.MAIN) {
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
                if (isShowing() && !paused) setter.accept(true);
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

    private void updateSimulation() {
        long now = System.nanoTime();
        long elapsed = Math.min(now - lastSimulationNanos, 250_000_000L);
        lastSimulationNanos = now;
        simulationAccumulatorNanos += elapsed;

        while (simulationAccumulatorNanos >= SIMULATION_STEP_NANOS) {
            session.update(input, SIMULATION_STEP_NANOS / 1_000_000_000.0);
            simulationAccumulatorNanos -= SIMULATION_STEP_NANOS;
        }

        if (elapsed > 0) repaint();
    }

    // ==========================================
    // 6. PAUSAR Y REANUDAR
    // ==========================================

    private void handleEscape() {

        if (!paused) {

            // Abrir el menu de pausa.
            paused = true;
            pausePage = PausePage.MAIN;
            selectedOption = 0;

            animationTimer.stop();
            simulationTimer.stop();
            input.clear();

        } else if (pausePage != PausePage.MAIN) {

            // Regresar desde Controles o Resumen.
            pausePage = PausePage.MAIN;

        } else {

            // Cerrar el menu.
            resumeGame();
        }

        repaint();
    }

    private void resumeGame() {

        paused = false;
        pausePage = PausePage.MAIN;

        if (isShowing()) {
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

        if (!paused ||
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

        if (!paused) {
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

            case 3 -> {
                animationTimer.stop();
                simulationTimer.stop();
                input.clear();
                onBack.run();
            }

            default -> {
            }
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

        sceneGraphics.dispose();

        // ======================================
        // OSCURECIMIENTO Y MENU DE PAUSA
        // ======================================

        if (paused) {

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

            drawPause(menuGraphics);

            menuGraphics.dispose();
        }

        g.dispose();
    }

    // ==========================================
    // 15. MENU DE PAUSA
    // ==========================================

    private void drawPause(Graphics2D g) {

        int panelWidth = 450;
        int panelHeight = 400;

        int panelX = (WIDTH - panelWidth) / 2;
        int panelY = (HEIGHT - panelHeight) / 2;

        int centerX = WIDTH / 2;

        // Borde del menu.
        g.setColor(RetroStyle.OUTLINE);

        g.fillRoundRect(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                22, 22
        );

        // Fondo del menu.
        g.setColor(new Color(0xF4F1E4));

        g.fillRoundRect(
                panelX + 7,
                panelY + 7,
                panelWidth - 14,
                panelHeight - 14,
                16, 16
        );

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

                // Color del boton.
                if (i == selectedOption) {

                    g.setColor(new Color(0xBED7FF));

                } else {

                    g.setColor(Color.WHITE);
                }

                // Fondo del boton.
                g.fillRoundRect(
                        MENU_X,
                        y,
                        MENU_W,
                        OPTION_H,
                        8, 8
                );

                // Borde del boton.
                g.setColor(RetroStyle.OUTLINE);

                g.drawRoundRect(
                        MENU_X,
                        y,
                        MENU_W,
                        OPTION_H,
                        8, 8
                );

                // Nombre de la opcion.
                drawCentered(
                        g,
                        PAUSE_OPTIONS[i],
                        centerX,
                        y + 29,
                        23,
                        RetroStyle.DARK
                );
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
                    "JUGADOR 1: A / D, W, J / K",
                    centerX,
                    panelY + 130,
                    20,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "JUGADOR 2: FLECHAS, ARRIBA, 1 / 2",
                    centerX,
                    panelY + 180,
                    20,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "P1: J RAPIDO / K IMPACTRUENO",
                    centerX,
                    panelY + 250,
                    18,
                    RetroStyle.DARK
            );

            drawCentered(g, "P2: 1 ASCUAS / 2 PLACAJE", centerX,
                    panelY + 290, 18, RetroStyle.DARK);

            drawCentered(g, "ESC O ENTER PARA VOLVER", centerX,
                    panelY + 350, 18, RetroStyle.DARK);
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
                    "PIKACHU VS CHARIZARD",
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

        g.setFont(RetroStyle.font(size));
        g.setColor(color);

        FontMetrics metrics = g.getFontMetrics();

        int textWidth =
                metrics.stringWidth(text);

        g.drawString(
                text,
                centerX - textWidth / 2,
                baseline
        );
    }
}
