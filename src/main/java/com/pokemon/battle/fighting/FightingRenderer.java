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
        g.setFont(RetroStyle.font(20));
        g.setColor(Color.WHITE);
        g.drawString("EVOLUCION", leftX, 108);
        if (leftFighter.isEvolutionReady()) {
            g.setColor(new Color(0xFFD45A));
            g.drawString("LISTA!", leftX + barWidth - 82, 108);
        }
        g.setColor(Color.WHITE);
        String evolutionText = "EVOLUCION";
        int evolutionWidth = g.getFontMetrics().stringWidth(evolutionText);
        g.drawString(evolutionText, rightX + barWidth - evolutionWidth, 108);
        if (rightFighter.isEvolutionReady()) {
            g.setColor(new Color(0xFFD45A));
            g.drawString("LISTA!", rightX + 8, 108);
        }
        drawBar(g, leftX, 113, barWidth, 12, new Color(0xFFD45A),
                leftFighter.getEvolutionEnergy() / 100.0);
        drawBar(g, rightX, 113, barWidth, 12, new Color(0xFFD45A),
                rightFighter.getEvolutionEnergy() / 100.0);
        drawCentered(g, "VS", session.getWidth() / 2, 83, 31, Color.WHITE);
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
            g.setColor(projectile.getColor());
            g.fillRect(x + size / 4, y, size / 2, size);
            g.fillRect(x, y + size / 4, size, size / 2);
            g.setColor(Color.WHITE);
            g.fillRect(x + size / 3, y + size / 3, size / 3, size / 3);
        }

        for (Fighter attacker : session.getFighters()) {
          if (session.getCombatSystem().isDashing(attacker, session.getFighters())) {
            AttackDefinition definition = session.getCombatSystem()
                    .getActiveDefinition(attacker, session.getFighters());
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
