package com.pokemon.battle.fighting;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Colores alternos (variocolor) para el jugador 2 cuando los dos eligen el mismo Pokemon,
 * asi no se confunden en la pelea.
 *
 * Cada regla toma los pixeles cuyo matiz cae en un rango y les pone otro matiz,
 * ajustando saturacion y brillo. Los colores casi grises (ojos, contorno, blancos) no se tocan.
 */
final class AltPalette {

    /** Un rango de matiz (grados) que se cambia a otro matiz. */
    private record Rule(float fromHue, float toHue, float newHue, float satFactor, float valFactor) { }

    private record Palette(float minSaturation, List<Rule> rules) { }

    private static Palette p(Rule... rules) { return new Palette(0.18f, List.of(rules)); }
    private static Rule r(float from, float to, float hue, float sat, float val) {
        return new Rule(from, to, hue, sat, val);
    }

    private static final Map<String, Palette> PALETTES = Map.ofEntries(
            // Formas base
            Map.entry("PIKACHU", p(r(35, 70, 32, 1.15f, 0.92f))),                 // naranja dorado
            Map.entry("CHARMANDER", p(r(0, 40, 352, 1.1f, 0.85f))),               // rojo carmesi
            Map.entry("SQUIRTLE", p(r(170, 230, 195, 0.55f, 1.1f), r(20, 50, 30, 0.6f, 1f))), // celeste
            Map.entry("BULBASAUR", p(r(60, 200, 170, 1.25f, 0.9f))),              // turquesa
            Map.entry("EEVEE", p(r(15, 45, 30, 0.15f, 1.15f))),                   // plateado
            Map.entry("GASTLY", p(r(250, 320, 215, 1f, 1f))),                     // azul
            Map.entry("MACHOP", p(r(90, 260, 40, 2.2f, 1f))),                     // beige
            Map.entry("ABRA", p(r(35, 60, 345, 0.35f, 1.1f))),                    // rosado
            Map.entry("VULPIX", p(r(5, 45, 40, 0.35f, 1.15f))),                   // crema
            Map.entry("SANDSHREW", p(r(35, 60, 95, 0.7f, 0.95f))),                // verde
            // Evoluciones
            Map.entry("RAICHU", p(r(25, 70, 10, 1f, 0.85f))),                     // rojizo
            Map.entry("CHARMELEON", p(r(0, 30, 352, 1.1f, 0.85f))),               // rojo carmesi
            Map.entry("WARTORTLE", p(r(180, 250, 270, 0.55f, 1.05f))),            // lila
            Map.entry("IVYSAUR", p(r(165, 240, 85, 0.9f, 1f), r(345, 360, 28, 1f, 1f),
                    r(0, 20, 28, 1f, 1f))),                                       // verde, flor naranja
            Map.entry("FLAREON", p(r(0, 30, 50, 1f, 1.08f))),                     // dorado
            Map.entry("HAUNTER", p(r(250, 320, 215, 1f, 1f))),                    // azul
            Map.entry("MACHOKE", new Palette(0.08f, List.of(r(200, 300, 35, 2.0f, 1f)))), // beige
            Map.entry("KADABRA", p(r(35, 60, 345, 0.35f, 1.1f))),                 // rosado
            Map.entry("NINETALES", new Palette(0.06f, List.of(r(20, 70, 40, 0.35f, 1.15f)))), // crema
            Map.entry("SANDSLASH", p(r(35, 60, 95, 0.7f, 0.95f), r(15, 34, 20, 0.5f, 0.8f)))  // verde
    );

    /** Cuadros ya pintados: cada cuadro del sprite se recolorea una sola vez. */
    private static final Map<BufferedImage, BufferedImage> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private AltPalette() { }

    static boolean hasPalette(String pokemonId) {
        return pokemonId != null && PALETTES.containsKey(pokemonId.toUpperCase());
    }

    /** Devuelve el cuadro con los colores alternos (o el mismo si ese Pokemon no tiene). */
    static BufferedImage recolor(String pokemonId, BufferedImage source) {
        if (source == null || !hasPalette(pokemonId)) return source;
        Palette palette = PALETTES.get(pokemonId.toUpperCase());
        return CACHE.computeIfAbsent(source, img -> apply(palette, img));
    }

    private static BufferedImage apply(Palette palette, BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        float[] hsv = new float[3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int alpha = argb >>> 24;
                out.setRGB(x, y, alpha == 0 ? argb : recolorPixel(palette, argb, alpha, hsv));
            }
        }
        return out;
    }

    private static int recolorPixel(Palette palette, int argb, int alpha, float[] hsv) {
        Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, hsv);
        float sat = hsv[1], val = hsv[2];
        if (sat < palette.minSaturation() || val < 0.12f) return argb;
        float degrees = hsv[0] * 360f;
        for (Rule rule : palette.rules()) {
            if (degrees >= rule.fromHue() && degrees <= rule.toHue()) {
                int rgb = Color.HSBtoRGB(rule.newHue() / 360f,
                        Math.min(1f, sat * rule.satFactor()), Math.min(1f, val * rule.valFactor()));
                return (alpha << 24) | (rgb & 0xFFFFFF);
            }
        }
        return argb;
    }
}
