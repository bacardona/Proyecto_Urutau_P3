
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
import java.awt.image.BufferedImage;
import java.util.Objects;

public class FightingGamePanel extends JPanel {

    // ==========================================
    // 1. DIMENSIONES DEL ESCENARIO
    // ==========================================

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int GROUND_Y = 677;

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

    private final SpriteLoader spriteLoader =
            SpriteLoader.shared();

    private final Runnable onBack;
    private final Timer animationTimer;

    private int frameIndex = 0;
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

                } else {

                    animationTimer.stop();
                }

                repaint();
            }
        });

        // Controles del menu.
        bindKey("ESCAPE", this::handleEscape);
        bindKey("UP", () -> moveSelection(-1));
        bindKey("DOWN", () -> moveSelection(1));
        bindKey("ENTER", this::activateSelected);

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

        // Dibujar fondo.
        drawArena(sceneGraphics);

        // Dibujar barras.
        drawBars(sceneGraphics);

        // Dibujar Pokemon.
        drawPokemon(
                sceneGraphics,
                "PIKACHU",
                230,
                GROUND_Y,
                170,
                false
        );

        drawPokemon(
                sceneGraphics,
                "CHARIZARD",
                1050,
                GROUND_Y,
                220,
                true
        );

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
    // 10. ESCENARIO
    // ==========================================

    private void drawArena(Graphics2D g) {

        // Cielo.
        g.setColor(new Color(0x98C9EF));

        g.fillRect(
                0, 0,
                WIDTH, HEIGHT
        );

        // Movimiento de las nubes.
        int cloudOffset =
                (frameIndex * 2) % (WIDTH + 250);

        int cloud1X = cloudOffset - 200;

        int cloud2X =
                (cloudOffset + 600) % (WIDTH + 250) - 200;

        g.setColor(new Color(0xE5F6FF));

        // Primera nube.
        g.fillRoundRect(
                cloud1X, 235,
                150, 35,
                30, 30
        );

        // Segunda nube.
        g.fillRoundRect(
                cloud2X, 285,
                160, 35,
                30, 30
        );

        // Plataformas.
        drawPlatform(g, 340, 490, 190);
        drawPlatform(g, 750, 490, 190);

        // Suelo.
        drawPlatform(g, 0, GROUND_Y, WIDTH);
    }

    // ==========================================
    // 11. PLATAFORMAS
    // ==========================================

    private void drawPlatform(
            Graphics2D g,
            int x,
            int y,
            int width
    ) {

        g.setColor(new Color(0x416C50));
        g.fillRect(x, y, width, 24);

        g.setColor(new Color(0x7DCC82));
        g.fillRect(x, y, width, 7);

        g.setColor(new Color(0x335C3F));
        g.fillRect(x, y + 19, width, 5);
    }

    // ==========================================
    // 12. BARRAS DE VIDA Y EVOLUCION
    // ==========================================

    private void drawBars(Graphics2D g) {

        // Fondo de las barras.
        g.setColor(new Color(20, 35, 60, 130));

        g.fillRoundRect(
                24, 20,
                WIDTH - 48, 116,
                18, 18
        );

        int leftX = 42;
        int barWidth = 360;
        int rightX = WIDTH - 42 - barWidth;

        // Nombres.
        g.setFont(RetroStyle.font(28));
        g.setColor(Color.WHITE);

        g.drawString(
                "PIKACHU",
                leftX, 51
        );

        String rightName = "CHARIZARD";

        int nameWidth = g.getFontMetrics()
                .stringWidth(rightName);

        g.drawString(
                rightName,
                rightX + barWidth - nameWidth,
                51
        );

        // Vida del primer Pokemon.
        drawBar(
                g, leftX, 60,
                barWidth, 22,
                new Color(0x56B75C),
                true
        );

        // Vida del segundo Pokemon.
        drawBar(
                g, rightX, 60,
                barWidth, 22,
                new Color(0x56B75C),
                true
        );

        // Texto de evolucion.
        g.setFont(RetroStyle.font(20));
        g.setColor(Color.WHITE);

        g.drawString(
                "EVOLUCION",
                leftX, 108
        );

        String evolutionText = "EVOLUCION";

        int evolutionWidth = g.getFontMetrics()
                .stringWidth(evolutionText);

        g.drawString(
                evolutionText,
                rightX + barWidth - evolutionWidth,
                108
        );

        // Barra de evolucion izquierda.
        drawBar(
                g, leftX, 113,
                barWidth, 12,
                new Color(0xFFD45A),
                false
        );

        // Barra de evolucion derecha.
        drawBar(
                g, rightX, 113,
                barWidth, 12,
                new Color(0xFFD45A),
                false
        );

        // Indicador VS.
        drawCentered(
                g, "VS",
                WIDTH / 2,
                83,
                31,
                Color.WHITE
        );
    }

    // ==========================================
    // 13. DIBUJAR UNA BARRA
    // ==========================================

    private void drawBar(
            Graphics2D g,
            int x,
            int y,
            int width,
            int height,
            Color fill,
            boolean full
    ) {

        // Borde.
        g.setColor(RetroStyle.OUTLINE);

        g.fillRect(
                x, y,
                width, height
        );

        // Interior.
        g.setColor(new Color(0xB7B7B7));

        g.fillRect(
                x + 3, y + 3,
                width - 6, height - 6
        );

        // Relleno.
        if (full) {

            g.setColor(fill);

            g.fillRect(
                    x + 3, y + 3,
                    width - 6, height - 6
            );
        }
    }

    // ==========================================
    // 14. DIBUJAR LOS POKEMON
    // ==========================================

    private void drawPokemon(
            Graphics2D g,
            String name,
            int centerX,
            int feetY,
            int targetHeight,
            boolean flip
    ) {

        GifAnimation animation =
                spriteLoader.getOrLoad(
                        name,
                        false,
                        this::repaint
                );

        if (animation == null) {

            drawCentered(
                    g,
                    "CARGANDO " + name,
                    centerX,
                    feetY - 45,
                    21,
                    RetroStyle.DARK
            );

            return;
        }

        BufferedImage frame =
                animation.frame(frameIndex);

        int width = Math.round(
                targetHeight *
                        (frame.getWidth() /
                                (float) frame.getHeight())
        );

        int x = centerX - width / 2;
        int y = feetY - targetHeight;

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        if (flip) {

            g.drawImage(
                    frame,
                    x + width, y,
                    -width, targetHeight,
                    null
            );

        } else {

            g.drawImage(
                    frame,
                    x, y,
                    width, targetHeight,
                    null
            );
        }
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
                    "JUGADOR 2: FLECHAS, 1 / 2",
                    centerX,
                    panelY + 180,
                    20,
                    RetroStyle.DARK
            );

            drawCentered(
                    g,
                    "CONTROLES AUN NO ACTIVOS",
                    centerX,
                    panelY + 250,
                    18,
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
