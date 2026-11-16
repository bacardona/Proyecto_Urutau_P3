package com.pokemon.battle.ui;

import com.pokemon.battle.model.Move;

import java.net.URL;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds the animation of each attack in resources/ataques.
 * <p>
 * The file is named after the move in capital letters, without accents and
 * with "_" instead of spaces: "Pistola de Agua" is PISTOLA_DE_AGUA.gif and
 * "Gruñido" is GRUNIDO.gif. When a move has no file of its own, the generic
 * effect of its type is used (ataques/tipos/TIPO_AGUA.gif, etc.).
 */
public final class AttackEffects {

    private static final String FOLDER = "/ataques/";
    private static final String TYPE_FOLDER = "/ataques/tipos/TIPO_";

    // Animaciones ya leidas (null = no hay archivo para ese ataque ni para su tipo)
    private static final Map<String, GifAnimation> CACHE = new HashMap<>();

    private AttackEffects() {
    }

    /**
     * @param move the move used
     * @return its animation, the one of its type, or null if there is none
     */
    public static synchronized GifAnimation forMove(Move move) {
        String key = fileName(move.getName());
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        GifAnimation animation = load(FOLDER + key + ".gif");
        if (animation == null) {
            animation = load(TYPE_FOLDER + RetroStyle.typeName(move.getType()) + ".gif");
        }
        CACHE.put(key, animation);
        return animation;
    }

    /**
     * @param name file name without the folder or ".gif", for example EXCAVAR
     * @return that animation, or null if the file does not exist
     */
    public static synchronized GifAnimation byName(String name) {
        if (!CACHE.containsKey(name)) {
            CACHE.put(name, load(FOLDER + name + ".gif"));
        }
        return CACHE.get(name);
    }

    // "Pistola de Agua" -> "PISTOLA_DE_AGUA", "Gruñido" -> "GRUNIDO"
    static String fileName(String moveName) {
        String plain = Normalizer.normalize(moveName.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return plain.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static GifAnimation load(String path) {
        URL url = AttackEffects.class.getResource(path);
        return url == null ? null : GifAnimation.readFull(url);
    }
}
