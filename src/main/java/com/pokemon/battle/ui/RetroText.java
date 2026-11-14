package com.pokemon.battle.ui;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/** Ayudas de texto para las pantallas retro. */
final class RetroText {

    private RetroText() {
    }

    // Parte un texto en lineas que caben en maxWidth
    static List<String> wrap(FontMetrics fm, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() == 0 || fm.stringWidth(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    // Recorta con ".." si el texto no cabe
    static String fit(FontMetrics fm, String text, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && fm.stringWidth(cut + "..") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }
}