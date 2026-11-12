package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.RetroStyle;
import com.pokemon.battle.ui.SpriteLoader;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws the arena and its fighters without changing session state. */
public class FightingRenderer {

    private final SpriteLoader spriteLoader;

    public FightingRenderer(SpriteLoader spriteLoader) {
        this.spriteLoader = spriteLoader;
    }

    public void draw(Graphics2D g, FightingGameSession session, int frameIndex, Runnable repaint) {
        drawArena(g, session, frameIndex);
        drawBars(g, session);
        drawAttackEffects(g, session);
        for (Fighter fighter : session.getFighters()) {
            int height = "PIKACHU".equals(fighter.getPokemonId()) ? 145 : 190;
            drawPokemon(g, fighter, height, frameIndex, repaint, session.getGroundY());
        }
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
        g.drawString("PIKACHU", leftX, 51);
        String rightName = "CHARIZARD";
        int nameWidth = g.getFontMetrics().stringWidth(rightName);
        g.drawString(rightName, rightX + barWidth - nameWidth, 51);
        drawBar(g, leftX, 60, barWidth, 22, new Color(0x56B75C),
                session.getFighters().get(0).getHealth() / (double) session.getFighters().get(0).getMaxHealth());
        drawBar(g, rightX, 60, barWidth, 22, new Color(0x56B75C),
                session.getFighters().get(1).getHealth() / (double) session.getFighters().get(1).getMaxHealth());
        g.setFont(RetroStyle.font(20));
        g.setColor(Color.WHITE);
        g.drawString("EVOLUCION", leftX, 108);
        String evolutionText = "EVOLUCION";
        int evolutionWidth = g.getFontMetrics().stringWidth(evolutionText);
        g.drawString(evolutionText, rightX + barWidth - evolutionWidth, 108);
        drawBar(g, leftX, 113, barWidth, 12, new Color(0xFFD45A), 0);
        drawBar(g, rightX, 113, barWidth, 12, new Color(0xFFD45A), 0);
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

        Fighter pikachu = session.getFighters().get(0);
        if (session.getCombatSystem().isDashing(pikachu, session.getFighters())) {
            AttackDefinition definition = session.getCombatSystem()
                    .getActiveDefinition(pikachu, session.getFighters());
            int direction = pikachu.isFacingLeft() ? -1 : 1;
            int trailX = (int) pikachu.getX() - direction * 52;
            int trailY = (int) pikachu.getY() - 82;
            g.setColor(new Color(definition.effectColor().getRed(),
                    definition.effectColor().getGreen(), definition.effectColor().getBlue(), 170));
            g.fillRect(trailX - 8, trailY, 16, 8);
            g.fillRect(trailX, trailY + 12, 24, 7);
            g.fillRect(trailX - 5, trailY + 24, 13, 6);
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
        if (fighter.isFacingLeft()) {
            g.drawImage(frame, x + width, y, -width, targetHeight, null);
        } else {
            g.drawImage(frame, x, y, width, targetHeight, null);
        }
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int baseline, int size, Color color) {
        g.setFont(RetroStyle.font(size));
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}
