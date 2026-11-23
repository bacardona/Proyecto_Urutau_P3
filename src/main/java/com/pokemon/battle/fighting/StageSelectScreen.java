package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.RetroScreen;
import com.pokemon.battle.ui.RetroStyle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pantalla para elegir el mapa del combate libre: tres tarjetas con una vista previa
 * de cada escenario. Flechas izquierda/derecha para moverse y ENTER para elegir.
 */
public class StageSelectScreen extends RetroScreen {

    private static final FightingStage[] STAGES = FightingStage.values();
    private static final int CARD_W = 280;
    private static final int CARD_H = 158;
    private static final int GAP = 24;
    private static final int CARDS_Y = 170;

    private final Consumer<FightingStage> onChoose;
    private final Runnable onBack;
    private final Map<FightingStage, BufferedImage> previews = new EnumMap<>(FightingStage.class);
    private int selected = 1;

    public StageSelectScreen(Consumer<FightingStage> onChoose, Runnable onBack) {
        super("ELIGE EL MAPA");
        this.onChoose = onChoose;
        this.onBack = onBack;
        bindKey("LEFT", () -> move(-1));
        bindKey("RIGHT", () -> move(1));
        bindKey("ENTER", this::choose);
    }

    /** Llamar al mostrar la pantalla: deja marcado el mapa que ya estaba elegido. */
    public void prepare(FightingStage current) {
        selected = current == null ? 1 : current.ordinal();
        repaint();
    }

    @Override
    protected void goBack() {
        onBack.run();
    }

    private void move(int delta) {
        selected = Math.floorMod(selected + delta, STAGES.length);
        repaint();
    }

    private void choose() {
        onChoose.accept(STAGES[selected]);
    }

    private Rectangle card(int i) {
        int total = STAGES.length * CARD_W + (STAGES.length - 1) * GAP;
        int x = (BOARD_W - total) / 2 + i * (CARD_W + GAP);
        return new Rectangle(x, CARDS_Y, CARD_W, CARD_H);
    }

    @Override
    protected void onBoardClick(int x, int y, int clickCount) {
        for (int i = 0; i < STAGES.length; i++) {
            if (card(i).contains(x, y)) {
                selected = i;
                repaint();
                if (clickCount >= 2) choose();
                return;
            }
        }
    }

    @Override
    protected void onWheel(int rotation) {
        move(rotation);
    }

    @Override
    protected void paintFooterButtons(Graphics2D g) {
        super.paintFooterButtons(g);
        RetroStyle.drawArrowKeyButton(g, false, "MOVER", 214, 598, 170, 36);
        RetroStyle.drawKeyButton(g, "ENTER  JUGAR", 404, 598, 190, 36);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Nombre de la pantalla dentro del cuadro rojo, como en las demas pantallas
        RetroStyle.drawRedFrame(g, 20, 28, 920, 56);
        g.setFont(RetroStyle.font(28));
        String title = screenTitle();
        int tw = g.getFontMetrics().stringWidth(title);
        RetroStyle.shadowText(g, title, (BOARD_W - tw) / 2, 65, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        g.setFont(RetroStyle.font(26));
        String hint = "TAMBIEN PUEDES HACER DOBLE CLIC EN UN MAPA";
        int hw = g.getFontMetrics().stringWidth(hint);
        RetroStyle.shadowText(g, hint, (BOARD_W - hw) / 2, 130, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);

        for (int i = 0; i < STAGES.length; i++) {
            Rectangle r = card(i);
            boolean isSelected = i == selected;
            int lift = isSelected ? 8 : 0;
            // marco
            g.setColor(RetroStyle.OUTLINE);
            g.fillRoundRect(r.x - 6, r.y - 6 - lift, r.width + 12, r.height + 12, 14, 14);
            g.setColor(isSelected ? RetroStyle.RED : new Color(0x4E6B5C));
            g.fillRoundRect(r.x - 3, r.y - 3 - lift, r.width + 6, r.height + 6, 10, 10);
            g.drawImage(preview(STAGES[i]), r.x, r.y - lift, r.width, r.height, null);
            if (!isSelected) {
                g.setColor(new Color(0, 0, 0, 110));
                g.fillRect(r.x, r.y, r.width, r.height);
            }
            // nombre debajo
            g.setFont(RetroStyle.font(isSelected ? 32 : 28));
            String name = STAGES[i].title();
            int nw = g.getFontMetrics().stringWidth(name);
            RetroStyle.shadowText(g, name, r.x + (r.width - nw) / 2, r.y + r.height + 44,
                    isSelected ? RetroStyle.ACCENT : RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
        }

        // Descripcion del mapa marcado
        int boxW = 760;
        int boxX = (BOARD_W - boxW) / 2;
        RetroStyle.drawPanel(g, boxX, 440, boxW, 90);
        g.setFont(RetroStyle.font(30));
        String desc = STAGES[selected].description().toUpperCase();
        int dw = g.getFontMetrics().stringWidth(desc);
        RetroStyle.shadowText(g, desc, (BOARD_W - dw) / 2, 494, RetroStyle.DARK, RetroStyle.SHADOW_LIGHT);
    }

    /** Vista previa: se dibuja el fondo del mapa una vez y se guarda. */
    private BufferedImage preview(FightingStage stage) {
        return previews.computeIfAbsent(stage, StageSelectScreen::renderPreview);
    }

    static BufferedImage renderPreview(FightingStage stage) {
        BufferedImage big = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = big.createGraphics();
        try {
            switch (stage) {
                case TEMPLO -> {
                    if (TempleStage.available()) {
                        TempleStage.drawBack(g, 1.0);
                        TempleStage.drawFront(g, 1.0);
                    }
                }
                case VOLCAN -> {
                    if (VolcanoStage.available()) {
                        VolcanoStage.drawBack(g, 1.0);
                        VolcanoStage.drawFront(g, 1.0);
                    }
                }
                default -> new FightingStageRenderer().draw(g, new FightingGameSession());
            }
        } catch (RuntimeException e) {
            g.setColor(new Color(0x2D3A52));
            g.fillRect(0, 0, 1280, 720);
        } finally {
            g.dispose();
        }
        BufferedImage small = new BufferedImage(CARD_W * 2, CARD_H * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = small.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(big, 0, 0, small.getWidth(), small.getHeight(), null);
        sg.dispose();
        return small;
    }
}
