package com.pokemon.battle.ui;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the animated sprites of the Pokemon from resources/sprites. Each file
 * is named after the Pokemon in capital letters, for example PIKACHU.gif, and
 * the back view ends in _ESPALDA (PIKACHU_ESPALDA.gif).
 * <p>
 * Only the last animations used are kept in memory, so moving through the
 * whole Pokedex does not fill the memory.
 */
public class SpriteLoader {

    private static final String FOLDER = "/sprites/";
    private static final String BACK_SUFFIX = "_ESPALDA";
    private static final int CACHE_SIZE = 10;

    // LinkedHashMap en orden de acceso: al pasar el limite se borra el que mas tiempo lleva sin usarse
    private final Map<String, GifAnimation> cache = new LinkedHashMap<String, GifAnimation>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, GifAnimation> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /**
     * @param pokemonName name of the Pokemon, in any case
     * @return the front animation, or null when there is no file for it
     */
    public synchronized GifAnimation front(String pokemonName) {
        return load(fileName(pokemonName, ""));
    }

    /**
     * @param pokemonName name of the Pokemon, in any case
     * @return the back animation (used in battle), or null when there is no file for it
     */
    public synchronized GifAnimation back(String pokemonName) {
        return load(fileName(pokemonName, BACK_SUFFIX));
    }

    /**
     * @param pokemonName name of the Pokemon, in any case
     * @return true when its front animation is already in memory
     */
    public synchronized boolean isLoaded(String pokemonName) {
        return cache.containsKey(fileName(pokemonName, ""));
    }

    // Nombre en mayusculas, sin espacios a los lados: "Pikachu " -> "PIKACHU.gif"
    private String fileName(String pokemonName, String suffix) {
        return pokemonName.trim().toUpperCase() + suffix + ".gif";
    }

    private GifAnimation load(String file) {
        if (cache.containsKey(file)) {
            return cache.get(file);
        }
        URL url = SpriteLoader.class.getResource(FOLDER + file);
        GifAnimation animation = url == null ? null : GifAnimation.read(url);
        cache.put(file, animation);
        return animation;
    }
}
