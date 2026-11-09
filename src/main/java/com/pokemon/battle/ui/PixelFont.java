package com.pokemon.battle.ui;

import java.awt.Font;
import java.io.InputStream;

public final class PixelFont {

    // Para cambiar de fuente en todo el programa, solo se cambia esta linea
    private static final String FONT_FILE = "/fonts/Jersey10-Regular.ttf";

    private static Font base;

    private PixelFont() {
    }

    public static Font get(float size) {
        try {
            if (base == null) {
                try (InputStream in = PixelFont.class.getResourceAsStream(FONT_FILE)) {
                    base = Font.createFont(Font.TRUETYPE_FONT, in);
                }
            }
            return base.deriveFont(size);
        } catch (Exception e) {
            // Si falla la carga, se usa una fuente normal para que el programa no se caiga
            return new Font(Font.MONOSPACED, Font.BOLD, (int) size);
        }
    }
}