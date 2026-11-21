package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.RetroStyle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Estilo "arcade" de la pausa y de la pantalla de fin de combate: sin recuadro ni botones,
 * solo texto blanco con borde negro encima del fondo oscurecido. La opcion elegida
 * sale en amarillo, un poco mas grande y con flechas: {@code > CONTINUAR <}.
 */
final class ArcadeMenuStyle {

    static final Color TEXT = Color.WHITE;
    static final Color SELECTED = new Color(0xFFD447);
    private static final Color UNSELECTED = new Color(255, 255, 255, 175);
    private static final Color OUTLINE = new Color(0, 0, 0, 220);

    private ArcadeMenuStyle() {
    }

    /** Texto centrado con borde negro. Los colores oscuros pasan a blanco; los de aviso se aclaran. */
    static void text(Graphics2D g, String text, int centerX, int baseline, int size, Color color) {
        outlined(g, text, centerX, baseline, size, readable(color));
    }

    /** Una opcion del menu: la elegida en amarillo con flechas, las demas en blanco suave. */
    static void option(Graphics2D g, String label, int centerX, int baseline, int size, boolean selected) {
        if (selected) {
            outlined(g, "> " + label + " <", centerX, baseline, size + 3, SELECTED);
        } else {
            outlined(g, label, centerX, baseline, size, UNSELECTED);
        }
    }

    /** Pasa los colores pensados para el recuadro claro a colores que se leen sobre fondo oscuro. */
    static Color readable(Color color) {
        if (color == null || color.equals(RetroStyle.DARK)) return TEXT;
        if (color.getRGB() == new Color(0xC8302A).getRGB()) return new Color(0xFF6B5E);
        if (color.getRGB() == new Color(0x7A2BD0).getRGB()) return new Color(0xC79BFF);
        return color;
    }

    private static void outlined(Graphics2D g, String text, int centerX, int baseline, int size, Color color) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(RetroStyle.font(size));
        int x = centerX - g.getFontMetrics().stringWidth(text) / 2;
        int border = Math.max(2, size / 12);
        g.setColor(OUTLINE);
        for (int dx = -border; dx <= border; dx += border) {
            for (int dy = -border; dy <= border; dy += border) {
                if (dx != 0 || dy != 0) g.drawString(text, x + dx, baseline + dy);
            }
        }
        g.setColor(color);
        g.drawString(text, x, baseline);
    }
}
