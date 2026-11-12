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
        for (Fighter fighter : session.getFighters()) {
            int height = "PIKACHU".equals(fighter.getPokemonId()) ? 170 : 220;
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
        drawPlatform(g, 340, 490, 190);
        drawPlatform(g, 750, 490, 190);
        drawPlatform(g, 0, session.getGroundY(), session.getWidth());
    }

    private void drawPlatform(Graphics2D g, int x, int y, int width) {
        g.setColor(new Color(0x416C50));
        g.fillRect(x, y, width, 24);
        g.setColor(new Color(0x7DCC82));
        g.fillRect(x, y, width, 7);
        g.setColor(new Color(0x335C3F));
        g.fillRect(x, y + 19, width, 5);
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
        drawBar(g, leftX, 60, barWidth, 22, new Color(0x56B75C), true);
        drawBar(g, rightX, 60, barWidth, 22, new Color(0x56B75C), true);
        g.setFont(RetroStyle.font(20));
        g.setColor(Color.WHITE);
        g.drawString("EVOLUCION", leftX, 108);
        String evolutionText = "EVOLUCION";
        int evolutionWidth = g.getFontMetrics().stringWidth(evolutionText);
        g.drawString(evolutionText, rightX + barWidth - evolutionWidth, 108);
        drawBar(g, leftX, 113, barWidth, 12, new Color(0xFFD45A), false);
        drawBar(g, rightX, 113, barWidth, 12, new Color(0xFFD45A), false);
        drawCentered(g, "VS", session.getWidth() / 2, 83, 31, Color.WHITE);
    }

    private void drawBar(Graphics2D g, int x, int y, int width, int height, Color fill, boolean full) {
        g.setColor(RetroStyle.OUTLINE);
        g.fillRect(x, y, width, height);
        g.setColor(new Color(0xB7B7B7));
        g.fillRect(x + 3, y + 3, width - 6, height - 6);
        if (full) {
            g.setColor(fill);
            g.fillRect(x + 3, y + 3, width - 6, height - 6);
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
