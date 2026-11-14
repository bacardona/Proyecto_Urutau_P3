package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.RetroStyle;
import com.pokemon.battle.ui.SpriteLoader;

import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws the arena and its fighters without changing session state. */
public class FightingRenderer {

    static final int RESULT_OPTION_X = 440;
    static final int RESULT_OPTION_Y = 390;
    static final int RESULT_OPTION_W = 400;
    static final int RESULT_OPTION_H = 44;
    static final int RESULT_OPTION_GAP = 12;

    private final SpriteLoader spriteLoader;

    public FightingRenderer(SpriteLoader spriteLoader) {
        this.spriteLoader = spriteLoader;
    }

    public void draw(Graphics2D g, FightingGameSession session, int frameIndex, Runnable repaint) {
        drawArena(g, session, frameIndex);
        drawBars(g, session);
        drawAttackEffects(g, session);
        for (Fighter fighter : session.getFighters()) {
            drawPokemon(g, fighter, fighter.getRenderHeight(), frameIndex, repaint,
                    session.getGroundY(), session.getCombatSystem(), session.getFighters());
        }
        drawPhysicalImpacts(g, session.getCombatSystem());
        drawUltimateEffects(g, session, frameIndex);
        drawFighterStatusIndicators(g, session);
        drawTeamIndicators(g, session, true, frameIndex, repaint);
        drawTeamIndicators(g, session, false, frameIndex, repaint);
    }

    public void drawResults(Graphics2D g, FightingGameSession session, int selectedOption) {
        int panelWidth = 500;
        int panelHeight = 390;
        int panelX = (session.getWidth() - panelWidth) / 2;
        int panelY = (session.getHeight() - panelHeight) / 2;
        int centerX = session.getWidth() / 2;

        g.setColor(RetroStyle.OUTLINE);
        g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 22, 22);
        g.setColor(new Color(0xF4F1E4));
        g.fillRoundRect(panelX + 7, panelY + 7, panelWidth - 14, panelHeight - 14, 16, 16);

        drawCentered(g, "COMBATE FINALIZADO", centerX, panelY + 70, 36, RetroStyle.DARK);
        String result = switch (session.getOutcome()) {
            case PLAYER_ONE_WINS -> session.getTrainerName(0) + " GANA!";
            case PLAYER_TWO_WINS -> session.getTrainerName(1) + " GANA!";
            case DRAW -> "EMPATE!";
        };
        drawCentered(g, result, centerX, panelY + 140, 32, RetroStyle.DARK);

        String[] options = {"REVANCHA", "VOLVER AL MENU"};
        for (int i = 0; i < options.length; i++) {
            int y = RESULT_OPTION_Y + i * (RESULT_OPTION_H + RESULT_OPTION_GAP);
            g.setColor(i == selectedOption ? new Color(0xBED7FF) : Color.WHITE);
            g.fillRoundRect(RESULT_OPTION_X, y, RESULT_OPTION_W, RESULT_OPTION_H, 8, 8);
            g.setColor(RetroStyle.OUTLINE);
            g.drawRoundRect(RESULT_OPTION_X, y, RESULT_OPTION_W, RESULT_OPTION_H, 8, 8);
            drawCentered(g, options[i], centerX, y + 29, 23, RetroStyle.DARK);
        }

        drawCentered(g, "FLECHAS + ENTER / CLIC", centerX,
                panelY + panelHeight - 14, 17, RetroStyle.DARK);
    }

    private void drawArena(Graphics2D g, FightingGameSession session, int frameIndex) {
        g.setColor(new Color(0x98C9EF));
        g.fillRect(0, 0, session.getWidth(), session.getHeight());
        int cloudOffset = (frameIndex * 2) % (session.getWidth() + 250);
        int cloud1X = cloudOffset - 200;
        int cloud2X = (cloudOffset + 600) % (session.getWidth() + 250) - 200;
        g.setColor(new Color(0xE5F6FF));
        g.fillRoundRect(cloud1X, 235, 150, 35, 30, 30);
        g.fillRoundRect(cloud2X, 285, 160, 35, 30, 30);
        for (Platform platform : session.getPlatforms()) {
            drawPlatform(g, platform);
        }
    }

    private void drawPlatform(Graphics2D g, Platform platform) {
        int x = platform.getX();
        int y = platform.getTop();
        int width = platform.getWidth();
        int height = platform.getHeight();
        g.setColor(new Color(0x416C50));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(0x7DCC82));
        g.fillRect(x, y, width, Math.min(7, height));
        g.setColor(new Color(0x335C3F));
        g.fillRect(x, y + height - 5, width, Math.min(5, height));
    }

    private void drawBars(Graphics2D g, FightingGameSession session) {
        g.setColor(new Color(20, 35, 60, 130));
        g.fillRoundRect(24, 20, session.getWidth() - 48, 116, 18, 18);
        int leftX = 42;
        int barWidth = 360;
        int rightX = session.getWidth() - 42 - barWidth;
        g.setFont(RetroStyle.font(28));
        g.setColor(Color.WHITE);
        Fighter leftFighter = session.getFighters().get(0);
        Fighter rightFighter = session.getFighters().get(1);
        String leftName = leftFighter.getName().toUpperCase();
        String rightName = rightFighter.getName().toUpperCase();
        g.drawString(leftName, leftX, 51);
        int nameWidth = g.getFontMetrics().stringWidth(rightName);
        g.drawString(rightName, rightX + barWidth - nameWidth, 51);
        drawBar(g, leftX, 60, barWidth, 22, new Color(0x56B75C),
                leftFighter.getHealth() / (double) leftFighter.getMaxHealth());
        drawBar(g, rightX, 60, barWidth, 22, new Color(0x56B75C),
                rightFighter.getHealth() / (double) rightFighter.getMaxHealth());
        g.setFont(RetroStyle.font(15));
        g.setColor(Color.WHITE);
        String leftEnergyLabel = energyLabel(leftFighter, "R");
        String rightEnergyLabel = energyLabel(rightFighter, "P");
        g.setColor(leftFighter.isEvolutionReady() ? new Color(0xFFD45A) : Color.WHITE);
        g.drawString(leftEnergyLabel, leftX, 108);
        g.setColor(rightFighter.isEvolutionReady() ? new Color(0xFFD45A) : Color.WHITE);
        int rightLabelWidth = g.getFontMetrics().stringWidth(rightEnergyLabel);
        g.drawString(rightEnergyLabel, rightX + barWidth - rightLabelWidth, 108);
        drawBar(g, leftX, 113, barWidth, 12, new Color(0xFFD45A),
                leftFighter.getEvolutionEnergy() / 100.0);
        drawBar(g, rightX, 113, barWidth, 12, new Color(0xFFD45A),
                rightFighter.getEvolutionEnergy() / 100.0);
        drawCentered(g, "VS", session.getWidth() / 2, 83, 31, Color.WHITE);
    }

    private String energyLabel(Fighter fighter, String evolutionKey) {
        if (fighter.isEvolved()) {
            return fighter.isUltimateReady() ? "ULTI DISPONIBLE" : "ENERGIA ULTI";
        }
        return fighter.isEvolutionReady()
                ? "EVOLUCION DISPONIBLE (" + evolutionKey + ")"
                : "EVOLUCION (" + evolutionKey + ")";
    }

    private void drawBar(Graphics2D g, int x, int y, int width, int height, Color fill, double ratio) {
        g.setColor(RetroStyle.OUTLINE);
        g.fillRect(x, y, width, height);
        g.setColor(new Color(0xB7B7B7));
        g.fillRect(x + 3, y + 3, width - 6, height - 6);
        if (ratio > 0) {
            g.setColor(fill);
            int fillWidth = (int) Math.round((width - 6) * Math.min(1, ratio));
            g.fillRect(x + 3, y + 3, fillWidth, height - 6);
        }
    }

    private void drawAttackEffects(Graphics2D g, FightingGameSession session) {
        for (FightingProjectile projectile : session.getProjectiles()) {
            int x = (int) Math.round(projectile.getX() - projectile.getSize() / 2);
            int y = (int) Math.round(projectile.getY() - projectile.getSize() / 2);
            int size = (int) Math.round(projectile.getSize());
            drawProjectileEffect(g, projectile, x, y, size);
        }
        for (Fighter attacker : session.getFighters()) {
            FightingCombatSystem.AttackVisual visual = session.getCombatSystem()
                    .getAttackVisual(attacker, session.getFighters());
            if (visual == null) continue;
            AttackDefinition definition = visual.definition();
            if ("VINE_WHIP".equals(definition.moveId())) {
                drawVineWhip(g, attacker, definition, visual);
            } else if ("BITE".equals(definition.moveId())) {
                drawBite(g, attacker, visual);
            } else if ("TAIL_WHIP".equals(definition.moveId())) {
                drawTailWhip(g, attacker, definition, visual);
            } else if ("TACKLE".equals(definition.moveId())) {
                drawTackleMotion(g, attacker, visual);
            } else if ("FLY".equals(definition.moveId())) {
                drawFlyMotion(g, attacker, visual);
            } else if ("GROWL".equals(definition.moveId())
                    && "ACTIVE".equals(visual.phase())) {
                drawSonicWaves(g, attacker,
                        visual.activeProgress());
            }
        }
    }

    private void drawPhysicalImpacts(Graphics2D g, FightingCombatSystem combat) {
        for (FightingCombatSystem.ImpactEffect effect : combat.getImpactEffects()) {
            if ("PROJECTILE".equals(effect.getMoveId())) {
                double progress = effect.getProgress();
                int radius = 7 + (int) Math.round(progress * 15);
                int alpha = (int) Math.round(220 * (1 - progress));
                g.setColor(new Color(211, 159, 245, Math.max(0, Math.min(255, alpha))));
                g.drawOval((int) effect.getX() - radius, (int) effect.getY() - radius,
                        radius * 2, radius * 2);
                continue;
            }
            drawPixelImpact(g, effect);
        }
    }

    private void drawPixelImpact(Graphics2D g, FightingCombatSystem.ImpactEffect effect) {
        double progress = effect.getProgress();
        double fade = 1 - progress;
        int cx = (int) Math.round(effect.getX());
        int cy = (int) Math.round(effect.getY());
        int radius = 5 + (int) (effect.getSize() * (0.18 + progress * 0.34));
        Color base = effect.getColor();
        var oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float) Math.max(0.08, fade)));
        if (effect.isBlocked()) {
            g.setColor(new Color(0xB7F0FF));
            g.fillRect(cx - 4, cy - radius, 8, 5);
            g.fillRect(cx - 4, cy + radius - 5, 8, 5);
            g.fillRect(cx - radius, cy - 4, 5, 8);
            g.fillRect(cx + radius - 5, cy - 4, 5, 8);
            g.setColor(new Color(0xFFFFFF));
            g.fillRect(cx - 3, cy - 3, 6, 6);
            drawCentered(g, "BLOCK", cx, cy - radius - 5, 12, new Color(0xCFF7FF));
        } else {
            g.setColor(base);
            for (int ray = 0; ray < 8; ray++) {
                double angle = ray * Math.PI / 4;
                int x = cx + (int) (Math.cos(angle) * radius);
                int y = cy + (int) (Math.sin(angle) * radius);
                int length = ray % 2 == 0 ? 8 : 5;
                if (Math.abs(Math.cos(angle)) > 0.7) {
                    g.fillRect(x - length / 2, y - 2, length, 5);
                } else if (Math.abs(Math.sin(angle)) > 0.7) {
                    g.fillRect(x - 2, y - length / 2, 5, length);
                } else {
                    g.fillRect(x - 2, y - 2, 5, 5);
                }
            }
            g.setColor(Color.WHITE);
            g.fillRect(cx - 4, cy - 4, 8, 8);
            g.setColor(base.brighter());
            g.fillRect(cx - 2, cy - 10, 4, 20);
            g.fillRect(cx - 10, cy - 2, 20, 4);
        }
        g.setComposite(oldComposite);
    }

    private void drawUltimateEffects(Graphics2D g, FightingGameSession session, int frameIndex) {
        for (UltimateController.Visual visual : session.getCombatSystem().getUltimateVisuals()) {
            Fighter owner = visual.owner();
            Fighter target = visual.target();
            int ownerX = (int) Math.round(owner.getX());
            int ownerY = (int) Math.round(owner.getY() - owner.getRenderHeight() * 0.55);
            Color primary = visual.definition().primaryColor();
            Color secondary = visual.definition().secondaryColor();
            if (visual.phase() == UltimateController.Phase.CHARGING) {
                drawUltimateCharge(g, ownerX, ownerY, owner.getRenderHeight(),
                        visual.progress(), primary, secondary, frameIndex);
                drawCentered(g, "CARGANDO ULTI", ownerX,
                        ownerY - owner.getRenderHeight() / 2 - 12, 18, secondary);
            } else if (visual.phase() == UltimateController.Phase.INTERRUPTED) {
                drawCentered(g, "ULTI INTERRUMPIDA", ownerX,
                        ownerY - owner.getRenderHeight() / 2 - 12, 16, new Color(0xFFF0A0));
            } else if (visual.phase() == UltimateController.Phase.RELEASING) {
                if (visual.definition().shape() == UltimateDefinition.Shape.RINGS) {
                    drawUltimateRings(g, visual, primary, secondary);
                } else {
                    drawUltimateBeam(g, visual, primary, secondary, frameIndex);
                }
                if (visual.pulseFlash() > 0) {
                    int radius = 34 + (int) (visual.pulseFlash() * 160);
                    int tx = (int) Math.round(target.getX());
                    int ty = (int) Math.round(target.getY() - target.getRenderHeight() * 0.55);
                    g.setColor(new Color(secondary.getRed(), secondary.getGreen(),
                            secondary.getBlue(), 155));
                    g.fillOval(tx - radius, ty - radius, radius * 2, radius * 2);
                    g.setColor(Color.WHITE);
                    g.drawOval(tx - radius, ty - radius, radius * 2, radius * 2);
                }
            } else if (visual.phase() == UltimateController.Phase.FINISHING) {
                int radius = 32 + (int) ((1 - visual.progress()) * 90);
                g.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 155));
                g.fillOval(ownerX - radius, ownerY - radius, radius * 2, radius * 2);
            }
        }
    }

    private void drawUltimateCharge(Graphics2D g, int x, int y, int height,
                                    double progress, Color primary, Color secondary,
                                    int frameIndex) {
        int radius = height / 3 + (int) (progress * 30);
        var oldStroke = g.getStroke();
        var oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float) (0.48 + 0.42 * progress)));
        g.setColor(primary);
        g.setStroke(new java.awt.BasicStroke(4 + (float) (progress * 5)));
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setColor(secondary);
        g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI / 4) + frameIndex * 0.08;
            int outerX = x + (int) (Math.cos(angle) * (radius + 15));
            int outerY = y + (int) (Math.sin(angle) * (radius + 15));
            int innerX = x + (int) (Math.cos(angle) * Math.max(8, radius * 0.35));
            int innerY = y + (int) (Math.sin(angle) * Math.max(8, radius * 0.35));
            g.drawLine(outerX, outerY, innerX, innerY);
            g.fillOval(outerX - 3, outerY - 3, 6, 6);
        }
        g.setColor(Color.WHITE);
        int core = 8 + (int) (progress * 12);
        g.fillOval(x - core / 2, y - core / 2, core, core);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawUltimateBeam(Graphics2D g, UltimateController.Visual visual,
                                  Color primary, Color secondary, int frameIndex) {
        Fighter owner = visual.owner();
        Fighter target = visual.target();
        int direction = owner.isFacingLeft() ? -1 : 1;
        int x1 = (int) Math.round(owner.getX() + direction * owner.getBodyHalfWidth());
        int y1 = (int) Math.round(owner.getY() - owner.getRenderHeight() * 0.58);
        int x2 = (int) Math.round(target.getX());
        int y2 = (int) Math.round(target.getY() - target.getRenderHeight() * 0.55);
        var oldStroke = g.getStroke();
        var oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        g.setColor(primary);
        g.setStroke(new java.awt.BasicStroke(42, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawLine(x1, y1, x2, y2);
        g.setColor(secondary);
        g.setStroke(new java.awt.BasicStroke(25, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawLine(x1, y1, x2, y2);
        g.setColor(Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(8, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new java.awt.BasicStroke(3));
        int segments = 9;
        for (int i = 1; i < segments; i++) {
            double ratio = i / (double) segments;
            int px = x1 + (int) ((x2 - x1) * ratio);
            int py = y1 + (int) ((y2 - y1) * ratio);
            int sway = (int) (Math.sin(frameIndex * 0.37 + i) * 13);
            g.setColor((i + frameIndex / 3) % 2 == 0 ? secondary : primary);
            if ("LEAF_STORM".equals(visual.definition().moveId())) {
                g.drawLine(px - 8, py + sway - 5, px + 8, py + sway + 5);
                g.drawLine(px - 8, py + sway + 5, px + 8, py + sway - 5);
            } else {
                int size = "HYDRO_PUMP".equals(visual.definition().moveId()) ? 7 : 9;
                g.fillOval(px - size / 2, py + sway - size / 2, size, size);
            }
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private void drawUltimateRings(Graphics2D g, UltimateController.Visual visual,
                                   Color primary, Color secondary) {
        int centerX = (int) Math.round(visual.centerX());
        int centerY = (int) Math.round(visual.centerY());
        var oldStroke = g.getStroke();
        for (int i = 0; i < visual.ringRadii().size(); i++) {
            int radius = (int) Math.round(visual.ringRadii().get(i));
            if (radius <= 0) continue;
            int alpha = Math.max(55, 220 - i * 28);
            g.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), alpha));
            g.setStroke(new java.awt.BasicStroke(11 - Math.min(i, 5)));
            g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            g.setColor(new Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), alpha));
            g.setStroke(new java.awt.BasicStroke(4));
            int inner = Math.max(0, radius - 12);
            g.drawOval(centerX - inner, centerY - inner, inner * 2, inner * 2);
        }
        g.setStroke(oldStroke);
    }

    private void drawProjectileEffect(Graphics2D g, FightingProjectile projectile,
                                      int x, int y, int size) {
        String moveId = projectile.getMoveId();
        if ("EMBER".equals(moveId)) {
            g.setColor(new Color(0xE84A20));
            g.fillOval(x + size / 5, y + size / 5, size * 3 / 5, size * 4 / 5);
            g.fillOval(x + size / 3, y, size / 3, size * 2 / 3);
            g.setColor(new Color(0xFFD75E));
            g.fillOval(x + size / 3, y + size / 3, size / 3, size / 2);
        } else if ("WATER_GUN".equals(moveId)) {
            int direction = projectile.getVelocityX() < 0 ? -1 : 1;
            g.setColor(new Color(0x2087D8));
            g.fillOval(x + size / 5, y + size / 4, size * 3 / 5, size / 2);
            g.setColor(new Color(0x9CEBFF));
            g.fillOval(x + size / 3, y + size / 3, size / 3, size / 3);
            g.setColor(new Color(0x5BCBFF));
            g.fillOval(x - direction * size / 3, y + size / 3, size / 3, size / 3);
        } else if ("SLEEP_POWDER".equals(moveId)) {
            g.setColor(new Color(0x854BB0));
            g.fillOval(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4);
            g.setColor(new Color(0xE8C8FF));
            g.fillOval(x + size / 3, y + size / 3, size / 3, size / 3);
            g.setColor(new Color(0xFFFFFF));
            g.fillRect(x + size / 4, y + size / 5, Math.max(2, size / 6), Math.max(2, size / 6));
        } else {
            g.setColor(projectile.getColor());
            g.fillRect(x + size / 4, y, size / 2, size);
            g.fillRect(x, y + size / 4, size, size / 2);
            g.setColor(Color.WHITE);
            g.fillRect(x + size / 3, y + size / 3, size / 3, size / 3);
        }
    }

    private void drawVineWhip(Graphics2D g, Fighter attacker, AttackDefinition definition,
                              FightingCombatSystem.AttackVisual visual) {
        if (!"ACTIVE".equals(visual.phase())) return;
        int direction = attacker.isFacingLeft() ? -1 : 1;
        int startX = (int) Math.round(attacker.getX() + direction * attacker.getBodyHalfWidth());
        int y = (int) Math.round(attacker.getY() - definition.hitboxHeight() * 0.60);
        int reach = (int) definition.hitboxWidth();
        double progress = visual.activeProgress();
        double extension = progress < 0.56 ? progress / 0.56 : (1 - progress) / 0.44;
        extension = Math.max(0.05, Math.min(1, extension));
        int tipX = startX + (int) (direction * reach * extension);
        int midX = (startX + tipX) / 2;
        double lift = Math.sin(progress * Math.PI) * 22;
        java.awt.geom.Path2D.Double vine = new java.awt.geom.Path2D.Double();
        vine.moveTo(startX, y);
        vine.quadTo((startX + midX) / 2.0, y - 24 - lift, midX, y - 7 - lift);
        vine.quadTo((midX + tipX) / 2.0, y + 10 - lift, tipX, y - 18);
        java.awt.geom.Path2D.Double second = new java.awt.geom.Path2D.Double();
        second.moveTo(startX, y + 7);
        second.quadTo((startX + midX) / 2.0, y + 26 + lift, midX, y + 14 + lift);
        second.quadTo((midX + tipX) / 2.0, y + 1 + lift, tipX - direction * 7, y - 6);
        var oldStroke = g.getStroke();
        var oldColor = g.getColor();
        g.setColor(new Color(0x347B3B));
        g.setStroke(new java.awt.BasicStroke(8, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.draw(vine);
        g.setStroke(new java.awt.BasicStroke(5, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.draw(second);
        g.setColor(new Color(0x79D45E));
        g.setStroke(new java.awt.BasicStroke(3, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.draw(vine);
        g.fillRect(tipX - 3, y - 20, 8, 5);
        g.fillRect(tipX + direction * 3, y - 17, 6, 4);
        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    private void drawBite(Graphics2D g, Fighter attacker,
                          FightingCombatSystem.AttackVisual visual) {
        if (!"ACTIVE".equals(visual.phase())) return;
        int direction = attacker.isFacingLeft() ? -1 : 1;
        double progress = visual.phase().equals("ACTIVE") ? visual.activeProgress() : visual.progress();
        int centerX = (int) Math.round(attacker.getX()
                + direction * (attacker.getBodyHalfWidth() + 20));
        int centerY = (int) Math.round(attacker.getY() - attacker.getRenderHeight() * 0.67);
        int open = (int) Math.round(10 * (1 - Math.sin(Math.PI * progress)));
        int jawW = 26;
        java.awt.Polygon upper = new java.awt.Polygon(
                new int[]{centerX, centerX + direction * jawW, centerX + direction * (jawW - 4)},
                new int[]{centerY - open / 2, centerY - open - 2, centerY - 2}, 3);
        java.awt.Polygon lower = new java.awt.Polygon(
                new int[]{centerX, centerX + direction * jawW, centerX + direction * (jawW - 4)},
                new int[]{centerY + open / 2, centerY + open + 3, centerY + 2}, 3);
        g.setColor(new Color(0x5B392D));
        g.fillPolygon(upper);
        g.fillPolygon(lower);
        g.setColor(new Color(0xF4E9CF));
        g.fillPolygon(new int[]{centerX + direction * 8, centerX + direction * 14,
                        centerX + direction * 17},
                new int[]{centerY - 2, centerY - open - 1, centerY - 3}, 3);
        g.fillPolygon(new int[]{centerX + direction * 8, centerX + direction * 14,
                        centerX + direction * 17},
                new int[]{centerY + 2, centerY + open + 2, centerY + 3}, 3);
    }

    private void drawTailWhip(Graphics2D g, Fighter attacker, AttackDefinition definition,
                              FightingCombatSystem.AttackVisual visual) {
        if (!"ACTIVE".equals(visual.phase())) return;
        int direction = attacker.isFacingLeft() ? -1 : 1;
        int anchorX = (int) Math.round(attacker.getX() + direction * attacker.getBodyHalfWidth());
        int centerY = (int) Math.round(attacker.getY() - definition.hitboxHeight() / 2);
        int reach = (int) (definition.hitboxWidth() * (0.65 + 0.35
                * Math.sin(Math.PI * visual.activeProgress())));
        double progress = visual.activeProgress();
        var oldStroke = g.getStroke();
        var oldColor = g.getColor();
        g.setColor(new Color(0x7A674C));
        g.setStroke(new java.awt.BasicStroke(10, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawArc(direction > 0 ? anchorX : anchorX - reach,
                centerY - 48, reach, 96, direction > 0 ? 280 : 80,
                (int) (70 + 100 * Math.sin(progress * Math.PI)));
        g.setColor(new Color(0xE7D7A8));
        g.setStroke(new java.awt.BasicStroke(5, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawArc(direction > 0 ? anchorX : anchorX - reach,
                centerY - 38, reach - 8, 76, direction > 0 ? 280 : 80,
                (int) (70 + 100 * Math.sin(progress * Math.PI)));
        int tipX = anchorX + direction * reach;
        int tipY = centerY + (int) (Math.sin(progress * Math.PI * 2) * 21);
        g.setColor(new Color(0xFFF3D4));
        g.fillRect(tipX - 4, tipY - 3, 8, 7);
        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    private void drawTackleMotion(Graphics2D g, Fighter fighter,
                                  FightingCombatSystem.AttackVisual visual) {
        if (!"ACTIVE".equals(visual.phase())) return;
        int direction = (int) visual.direction();
        int x = (int) fighter.getX() - direction * (int) (fighter.getBodyHalfWidth() * 0.7);
        int y = (int) (fighter.getY() - fighter.getRenderHeight() * 0.55);
        float fade = (float) Math.max(0.25, Math.sin(visual.activeProgress() * Math.PI));
        var oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        g.setColor(new Color(0xE9F0E8));
        g.fillRect(x - direction * 20, y - 12, 15, 3);
        g.fillRect(x - direction * 31, y, 24, 4);
        g.fillRect(x - direction * 22, y + 12, 13, 3);
        g.setComposite(oldComposite);
    }

    private void drawFlyMotion(Graphics2D g, Fighter fighter,
                               FightingCombatSystem.AttackVisual visual) {
        if (!"ACTIVE".equals(visual.phase())) return;
        int direction = (int) visual.direction();
        int x = (int) fighter.getX();
        int y = (int) fighter.getY();
        double p = visual.activeProgress();
        var oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.62f));
        if (p < 0.20) {
            g.setColor(new Color(0xE6F7FF));
            g.fillOval(x - 27, y - 13, 19, 8);
            g.fillOval(x + 8, y - 16, 22, 9);
            g.fillRect(x - 13, y - 22, 27, 4);
        } else {
            g.setColor(new Color(0xD8F1FF));
            for (int i = 0; i < 3; i++) {
                int tailX = x - direction * (34 + i * 16);
                int tailY = y - fighter.getRenderHeight() / 2 + i * 11;
                g.fillRect(tailX, tailY, 17 + i * 4, 4);
            }
            if (p > 0.48) {
                g.setColor(Color.WHITE);
                g.fillRect(x - 5, y - fighter.getRenderHeight() - 15, 4, 24);
                g.fillRect(x + direction * 12, y - fighter.getRenderHeight() - 4, 3, 18);
            }
        }
        g.setComposite(oldComposite);
    }

    private void drawSonicWaves(Graphics2D g, Fighter attacker, double progress) {
        int direction = attacker.isFacingLeft() ? -1 : 1;
        int mouthX = (int) Math.round(attacker.getX()
                + direction * (attacker.getBodyHalfWidth() - 4));
        int mouthY = (int) Math.round(attacker.getY() - attacker.getRenderHeight() * 0.68);
        for (int wave = 0; wave < 3; wave++) {
            double waveProgress = progress - wave * 0.25;
            if (waveProgress < 0 || waveProgress > 1) continue;
            int distance = (int) Math.round(waveProgress * 1000);
            int radius = 12 + (int) Math.round(waveProgress * 14);
            int alpha = Math.max(45, Math.min(230, 220 - (int) (waveProgress * 150)));
            g.setColor(new Color(0xF2E89A | (alpha << 24), true));
            int centerX = mouthX + direction * distance;
            int startAngle = direction > 0 ? 285 : 105;
            g.drawArc(centerX - radius, mouthY - radius, radius * 2, radius * 2,
                    startAngle, 150);
        }
    }

    private void drawFighterStatusIndicators(Graphics2D g, FightingGameSession session) {
        for (int player = 0; player < session.getFighters().size(); player++) {
            Fighter fighter = session.getFighters().get(player);
            StringBuilder status = new StringBuilder();
            if (fighter.isAsleep()) status.append("DORMIDO  ");
            if (fighter.isBlocking()) status.append("BLOQUEO  ");
            else if (fighter.isHitStunned()) status.append("GOLPEADO  ");
            if (fighter.hasAttackBoost()) status.append("ATQ+  ");
            if (fighter.hasAttackReduction()) status.append("ATQ-  ");
            if (fighter.hasDefenseReduction()) status.append("DEF-  ");
            if (fighter.isUltimateStunned()) status.append("ATURDIDO  ");
            AttackDefinition attack = session.getCombatSystem()
                    .getActiveDefinition(fighter, session.getFighters());
            if (attack != null && "GROWL".equals(attack.moveId())) status.append("GRITANDO  ");
            if (!status.isEmpty()) {
                Color color = fighter.isAsleep() ? new Color(0xD9B9F2)
                        : fighter.isBlocking() ? new Color(0x83DEFF)
                        : fighter.hasAttackBoost() ? new Color(0xA7EE77)
                        : new Color(0xFFE28A);
                drawCentered(g, status.toString().trim(), (int) fighter.getX(),
                        (int) fighter.getY() - fighter.getRenderHeight() - 8, 14, color);
            }

            if (fighter.isBlocking()) drawBlockGuard(g, fighter);
            FightingCombatSystem.ComboInfo combo = session.getCombatSystem().getComboInfo(player);
            if (combo.hits() >= 2 && combo.remainingSeconds() > 0) {
                int centerX = player == 0 ? 225 : session.getWidth() - 225;
                drawCentered(g, combo.hits() + " HITS COMBO", centerX, 244, 17,
                        player == 0 ? new Color(0xFFF07A) : new Color(0x8FE7FF));
            }
        }
    }

    private void drawBlockGuard(Graphics2D g, Fighter fighter) {
        Color oldColor = g.getColor();
        var oldStroke = g.getStroke();
        int direction = fighter.isFacingLeft() ? -1 : 1;
        int x = (int) Math.round(fighter.getX() + direction * (fighter.getBodyHalfWidth() + 10));
        int y = (int) Math.round(fighter.getY() - fighter.getRenderHeight() * 0.63);
        g.setColor(new Color(123, 220, 255, 210));
        g.drawArc(x - 15, y - 26, 30, 52, direction > 0 ? 270 : 90, 180);
        g.drawArc(x - 10, y - 19, 20, 38, direction > 0 ? 270 : 90, 180);
        g.setColor(oldColor);
        g.setStroke(oldStroke);
    }

    private void drawTeamIndicators(Graphics2D g, FightingGameSession session,
                                    boolean playerOne, int frameIndex, Runnable repaint) {
        FightingTeam team = playerOne ? session.getPlayerOneTeam() : session.getPlayerTwoTeam();
        int slotWidth = 112;
        int startX = playerOne ? 42 : session.getWidth() - 3 * slotWidth - 42;
        int spriteTop = 145;
        int spriteHeight = 50;
        for (int i = 0; i < team.getMembers().size(); i++) {
            Fighter fighter = team.getMembers().get(i);
            int x = startX + i * slotWidth;
            GifAnimation icon = spriteLoader.getOrLoad(fighter.getPokemonId(), false, repaint);
            if (icon != null) {
                BufferedImage image = icon.frame(frameIndex);
                int h = spriteHeight;
                int w = Math.min(68, Math.max(1, Math.round(h * image.getWidth() / (float) image.getHeight())));
                int spriteX = x + (slotWidth - w) / 2;
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                if (fighter.isDefeated()) {
                    var oldComposite = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.38f));
                    g.drawImage(image, spriteX, spriteTop, w, h, null);
                    g.setComposite(oldComposite);
                } else {
                    g.drawImage(image, spriteX, spriteTop, w, h, null);
                }
            }
            String status = fighter.isDefeated() ? "KO" : i == team.getActiveIndex() ? "ACTIVO" : "RESERVA";
            Color statusColor = fighter.isDefeated() ? new Color(0xFF9999)
                    : i == team.getActiveIndex() ? new Color(0xFFE36E) : Color.WHITE;
            String label = (i == team.getActiveIndex() && !fighter.isDefeated() ? "> " : "")
                    + (i + 1) + "  " + status;
            drawCentered(g, label, x + slotWidth / 2, 214, 14, statusColor);
        }
    }

    private void drawPokemon(Graphics2D g, Fighter fighter, int targetHeight,
                             int frameIndex, Runnable repaint, int groundY,
                             FightingCombatSystem combat, java.util.List<Fighter> fighters) {
        String name = fighter.getPokemonId();
        if (fighter.isEvolutionAnimating() && fighter.getEvolutionProfile() != null) {
            boolean oldFormFrame = (int) (fighter.getEvolutionAnimationProgress() * 12) % 2 == 0;
            if (fighter.isReturningToBase()) {
                name = oldFormFrame ? fighter.getEvolutionProfile().evolvedPokemonId()
                        : fighter.getBasePokemonId();
            } else if (oldFormFrame) {
                name = fighter.getBasePokemonId();
            }
        }
        GifAnimation animation = spriteLoader.getOrLoad(name, false, repaint);
        int centerX = (int) fighter.getX();
        if (animation == null) {
            drawCentered(g, "CARGANDO " + name, centerX, groundY - 45, 21, RetroStyle.DARK);
            return;
        }
        BufferedImage frame = animation.frame(frameIndex);
        int width = Math.round(targetHeight * (frame.getWidth() / (float) frame.getHeight()));
        FightingCombatSystem.AttackVisual visual = combat.getAttackVisual(fighter, fighters);
        double shakeX = combat.getVisualShakeX(fighter);
        double renderX = centerX + shakeX + visualOffsetX(visual);
        int x = (int) Math.round(renderX) - width / 2;
        int y = (int) fighter.getY() - targetHeight;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (visual != null && "QUICK_ATTACK".equals(visual.definition().moveId())
                && "ACTIVE".equals(visual.phase())) {
            drawQuickAfterimages(g, frame, fighter, width, targetHeight,
                    renderX, visual.direction());
        }
        double poseAngle = visualAngle(visual);
        double scaleX = visualScaleX(visual);
        double scaleY = visualScaleY(visual);
        Graphics2D spriteGraphics = (Graphics2D) g.create();
        spriteGraphics.translate(renderX, fighter.getY());
        spriteGraphics.rotate(poseAngle);
        spriteGraphics.scale(scaleX, scaleY);
        spriteGraphics.translate(-renderX, -fighter.getY());
        if (shouldFlipFrontSprite(fighter)) {
            spriteGraphics.drawImage(frame, x + width, y, -width, targetHeight, null);
        } else {
            spriteGraphics.drawImage(frame, x, y, width, targetHeight, null);
        }
        spriteGraphics.dispose();
        if (fighter.isEvolutionAnimating()) drawEvolutionFlash(g, (int) Math.round(renderX),
                y + targetHeight / 2, targetHeight, fighter.getEvolutionAnimationProgress());
    }

    private void drawQuickAfterimages(Graphics2D g, BufferedImage frame, Fighter fighter,
                                     int width, int height, double renderX, double direction) {
        var oldComposite = g.getComposite();
        float[] alpha = {0.18f, 0.28f, 0.40f};
        for (int i = 0; i < alpha.length; i++) {
            int offset = 13 + i * 15;
            int cx = (int) Math.round(renderX - direction * offset);
            int y = (int) Math.round(fighter.getY() - height);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha[i]));
            if (shouldFlipFrontSprite(fighter)) {
                g.drawImage(frame, cx + width / 2, y, -width, height, null);
            } else {
                g.drawImage(frame, cx - width / 2, y, width, height, null);
            }
        }
        g.setComposite(oldComposite);
    }

    private double visualOffsetX(FightingCombatSystem.AttackVisual visual) {
        if (visual == null) return 0;
        double phaseProgress = visual.progress();
        double direction = visual.direction();
        boolean active = "ACTIVE".equals(visual.phase());
        if ("TACKLE".equals(visual.definition().moveId())) {
            return active ? direction * 8 * Math.sin(Math.PI * visual.activeProgress())
                    : "STARTUP".equals(visual.phase()) ? -direction * 3 * (1 - phaseProgress)
                    : direction * 2 * (1 - phaseProgress);
        }
        if ("BITE".equals(visual.definition().moveId())) {
            return active ? direction * 4 * Math.sin(Math.PI * visual.activeProgress()) : 0;
        }
        return 0;
    }

    private double visualAngle(FightingCombatSystem.AttackVisual visual) {
        if (visual == null) return 0;
        double magnitude = switch (visual.definition().moveId()) {
            case "TACKLE" -> 0.12;
            case "BITE" -> 0.09;
            case "QUICK_ATTACK" -> 0.16;
            case "TAIL_WHIP" -> 0.10;
            case "VINE_WHIP" -> 0.055;
            case "FLY" -> 0.19;
            default -> 0;
        };
        if ("ACTIVE".equals(visual.phase())) {
            double progress = visual.activeProgress();
            double shape = "FLY".equals(visual.definition().moveId())
                    ? Math.sin((progress - 0.25) * Math.PI) : Math.sin(progress * Math.PI);
            return visual.direction() * magnitude * shape;
        }
        if ("STARTUP".equals(visual.phase())) {
            return -visual.direction() * magnitude * 0.55 * (1 - visual.progress());
        }
        return visual.direction() * magnitude * 0.50 * (1 - visual.progress());
    }

    private double visualScaleX(FightingCombatSystem.AttackVisual visual) {
        if (visual == null || !"ACTIVE".equals(visual.phase())) return 1;
        return switch (visual.definition().moveId()) {
            case "TACKLE", "QUICK_ATTACK" -> 1.0 + 0.08 * Math.sin(Math.PI * visual.activeProgress());
            case "FLY" -> 1.0 + 0.04 * Math.sin(Math.PI * visual.activeProgress());
            default -> 1;
        };
    }

    private double visualScaleY(FightingCombatSystem.AttackVisual visual) {
        if (visual == null || !"ACTIVE".equals(visual.phase())) return 1;
        return switch (visual.definition().moveId()) {
            case "TACKLE", "QUICK_ATTACK" -> 1.0 - 0.06 * Math.sin(Math.PI * visual.activeProgress());
            default -> 1;
        };
    }

    private void drawEvolutionFlash(Graphics2D g, int centerX, int centerY,
                                    int spriteHeight, double progress) {
        int radius = Math.max(34, spriteHeight / 2 + (int) (18 * Math.sin(progress * Math.PI)));
        var oldComposite = g.getComposite();
        var oldStroke = g.getStroke();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float) (0.35 + 0.45 * Math.sin(progress * Math.PI))));
        g.setColor(new Color(0xFFF2A0));
        g.setStroke(new java.awt.BasicStroke(4));
        g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g.setStroke(new java.awt.BasicStroke(2));
        for (int i = 0; i < 4; i++) {
            double angle = progress * Math.PI * 4 + i * Math.PI / 2;
            int sx = centerX + (int) (Math.cos(angle) * (radius + 8));
            int sy = centerY + (int) (Math.sin(angle) * (radius + 8));
            g.drawLine(sx - 5, sy, sx + 5, sy);
            g.drawLine(sx, sy - 5, sx, sy + 5);
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    private boolean shouldFlipFrontSprite(Fighter fighter) {
        // The available front GIFs face left in their source files.
        boolean sourceFacesLeft = switch (fighter.getPokemonId()) {
            case "BULBASAUR", "CHARMANDER", "SQUIRTLE", "EEVEE", "PIKACHU", "CHARIZARD" -> true;
            default -> true;
        };
        return fighter.isFacingLeft() != sourceFacesLeft;
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int baseline, int size, Color color) {
        g.setFont(RetroStyle.font(size));
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}
