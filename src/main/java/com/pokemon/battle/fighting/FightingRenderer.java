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
            drawPokemon(g, fighter, fighter.getRenderHeight(), frameIndex, repaint, session.getGroundY());
        }
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
        for (FightingCombatSystem.ImpactEffect effect : session.getCombatSystem().getImpactEffects()) {
            double progress = 1 - effect.getRemainingSeconds() / 0.22;
            int radius = 7 + (int) Math.round(progress * 15);
            int alpha = (int) Math.round(220 * (1 - progress));
            g.setColor(new Color(211, 159, 245, Math.max(0, Math.min(255, alpha))));
            g.drawOval((int) effect.getX() - radius, (int) effect.getY() - radius,
                    radius * 2, radius * 2);
        }

        for (Fighter attacker : session.getFighters()) {
            AttackDefinition definition = session.getCombatSystem()
                    .getActiveDefinition(attacker, session.getFighters());
            if (definition == null) continue;
            if ("VINE_WHIP".equals(definition.moveId())) {
                drawVineWhip(g, attacker, definition);
            } else if ("BITE".equals(definition.moveId())) {
                drawBite(g, attacker);
            } else if ("TAIL_WHIP".equals(definition.moveId())) {
                drawTailWhip(g, attacker, definition);
            } else if ("GROWL".equals(definition.moveId())) {
                drawSonicWaves(g, attacker,
                        session.getCombatSystem().getActiveProgress(attacker, session.getFighters()));
            }
            if (session.getCombatSystem().isDashing(attacker, session.getFighters())) {
                int direction = attacker.isFacingLeft() ? -1 : 1;
                int trailX = (int) attacker.getX() - direction * 52;
                int trailY = (int) attacker.getY() - 82;
                g.setColor(new Color(definition.effectColor().getRed(),
                        definition.effectColor().getGreen(), definition.effectColor().getBlue(), 170));
                g.fillRect(trailX - 8, trailY, 16, 8);
                g.fillRect(trailX, trailY + 12, 24, 7);
                g.fillRect(trailX - 5, trailY + 24, 13, 6);
            }
        }
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

    private void drawVineWhip(Graphics2D g, Fighter attacker, AttackDefinition definition) {
        int direction = attacker.isFacingLeft() ? -1 : 1;
        int startX = (int) Math.round(attacker.getX() + direction * attacker.getBodyHalfWidth());
        int y = (int) Math.round(attacker.getY() - definition.hitboxHeight() / 2);
        int reach = (int) definition.hitboxWidth();
        var oldStroke = g.getStroke();
        g.setColor(new Color(0x347B3B));
        g.setStroke(new java.awt.BasicStroke(7, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.drawLine(startX, y, startX + direction * (reach / 3), y - 8);
        g.drawLine(startX + direction * (reach / 3), y - 8,
                startX + direction * (2 * reach / 3), y + 5);
        g.drawLine(startX + direction * (2 * reach / 3), y + 5,
                startX + direction * reach, y - 6);
        g.setColor(new Color(0x79D45E));
        g.setStroke(new java.awt.BasicStroke(3, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.drawLine(startX, y - 2, startX + direction * (reach / 3), y - 10);
        g.drawLine(startX + direction * (reach / 3), y - 10,
                startX + direction * (2 * reach / 3), y + 3);
        g.drawLine(startX + direction * (2 * reach / 3), y + 3,
                startX + direction * reach, y - 8);
        g.setStroke(oldStroke);
    }

    private void drawBite(Graphics2D g, Fighter attacker) {
        int direction = attacker.isFacingLeft() ? -1 : 1;
        int centerX = (int) Math.round(attacker.getX()
                + direction * (attacker.getBodyHalfWidth() + 28));
        int centerY = (int) Math.round(attacker.getY() - 78);
        g.setColor(new Color(0xE7E2D8));
        g.drawArc(centerX - 18, centerY - 12, 24, 24,
                direction > 0 ? 35 : 145, 110);
        g.drawArc(centerX - 6, centerY - 12, 24, 24,
                direction > 0 ? 215 : 35, 110);
    }

    private void drawTailWhip(Graphics2D g, Fighter attacker, AttackDefinition definition) {
        int direction = attacker.isFacingLeft() ? -1 : 1;
        int anchorX = (int) Math.round(attacker.getX() + direction * attacker.getBodyHalfWidth());
        int centerY = (int) Math.round(attacker.getY() - definition.hitboxHeight() / 2);
        int reach = (int) definition.hitboxWidth();
        var oldStroke = g.getStroke();
        g.setColor(new Color(0xF5E8C8));
        g.setStroke(new java.awt.BasicStroke(8, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawArc(direction > 0 ? anchorX : anchorX - reach,
                centerY - 50, reach, 100, direction > 0 ? 300 : 120, 120);
        g.setColor(new Color(0xD6D6C2));
        g.setStroke(new java.awt.BasicStroke(4, java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.drawArc(direction > 0 ? anchorX : anchorX - reach,
                centerY - 40, reach - 10, 80, direction > 0 ? 300 : 120, 120);
        g.setStroke(oldStroke);
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
        for (Fighter fighter : session.getFighters()) {
            StringBuilder status = new StringBuilder();
            if (fighter.isAsleep()) status.append("DORMIDO  ");
            if (fighter.hasAttackBoost()) status.append("ATQ+  ");
            if (fighter.hasAttackReduction()) status.append("ATQ-  ");
            if (fighter.hasDefenseReduction()) status.append("DEF-  ");
            AttackDefinition attack = session.getCombatSystem()
                    .getActiveDefinition(fighter, session.getFighters());
            if (attack != null && "GROWL".equals(attack.moveId())) status.append("GRITANDO  ");
            if (status.isEmpty()) continue;
            Color color = fighter.isAsleep() ? new Color(0xD9B9F2)
                    : fighter.hasAttackBoost() ? new Color(0xA7EE77)
                    : new Color(0xFFE28A);
            drawCentered(g, status.toString().trim(), (int) fighter.getX(),
                    (int) fighter.getY() - fighter.getRenderHeight() - 8, 14, color);
        }
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
                             int frameIndex, Runnable repaint, int groundY) {
        String name = fighter.getPokemonId();
        if (fighter.isEvolutionAnimating() && fighter.getEvolutionProfile() != null
                && ((int) (fighter.getEvolutionAnimationProgress() * 12) % 2 == 0)) {
            name = fighter.getBasePokemonId();
        }
        GifAnimation animation = spriteLoader.getOrLoad(name, false, repaint);
        int centerX = (int) fighter.getX();
        if (animation == null) {
            drawCentered(g, "CARGANDO " + name, centerX, groundY - 45, 21, RetroStyle.DARK);
            return;
        }
        BufferedImage frame = animation.frame(frameIndex);
        int width = Math.round(targetHeight * (frame.getWidth() / (float) frame.getHeight()));
        int x = centerX - width / 2;
        int y = (int) fighter.getY() - targetHeight;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (shouldFlipFrontSprite(fighter)) {
            g.drawImage(frame, x + width, y, -width, targetHeight, null);
        } else {
            g.drawImage(frame, x, y, width, targetHeight, null);
        }
        if (fighter.isEvolutionAnimating()) drawEvolutionFlash(g, centerX,
                y + targetHeight / 2, targetHeight, fighter.getEvolutionAnimationProgress());
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
