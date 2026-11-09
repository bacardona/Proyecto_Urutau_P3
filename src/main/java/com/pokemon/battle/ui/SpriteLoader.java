package com.pokemon.battle.ui;

import com.pokemon.battle.model.MoveCategory;
import com.pokemon.battle.model.Type;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carga los sprites del juego. Tiene dos estilos que conviven:
 * <ul>
 *   <li>Metodos estaticos (pokemonFront, pokemonBack, type, category, typeIcon):
 *       usados por la pantalla de batalla y el armado de equipo. Leen
 *       /sprites/pokemon/front, /sprites/pokemon/back, /sprites/types y /sprites/categories.</li>
 *   <li>Metodos de instancia (front, back, isLoaded): usados por la Pokedex.
 *       Leen /sprites/PIKACHU.gif y /sprites/PIKACHU_ESPALDA.gif y solo guardan
 *       en memoria las ultimas animaciones usadas.</li>
 * </ul>
 */
public class SpriteLoader {

    // ---------- Parte estatica (batalla y armado de equipo) ----------

    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    private static final Map<String, Icon> ICON_CACHE = new HashMap<>();

    public static Image pokemonFront(String name) {
        return loadImage("/sprites/pokemon/front/" + clean(name) + ".gif");
    }

    public static Image pokemonBack(String name) {
        return loadImage("/sprites/pokemon/back/" + clean(name) + ".gif");
    }

    public static Image type(Type type) {
        return loadImage("/sprites/types/" + type.name().toLowerCase() + ".png");
    }

    public static Image category(MoveCategory category) {
        return loadImage("/sprites/categories/" + category.name().toLowerCase() + ".png");
    }

    // Icono de tipo escalado al ancho pedido, manteniendo la proporcion.
    // Devuelve null si la imagen no existe (la pantalla usa texto en ese caso).
    public static Icon typeIcon(Type type, int width) {
        String key = type.name() + "@" + width;
        if (ICON_CACHE.containsKey(key)) {
            return ICON_CACHE.get(key);
        }
        Image image = type(type);
        Icon icon = null;
        if (image != null) {
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            if (w > 0 && h > 0) {
                int height = Math.max(1, width * h / w);
                icon = new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
            } else {
                icon = new ImageIcon(image);
            }
        }
        ICON_CACHE.put(key, icon);
        return icon;
    }

    private static String clean(String name) {
        return name.trim().toLowerCase();
    }

    // Devuelve null si la imagen no existe, para que la pantalla use un marcador
    private static Image loadImage(String path) {
        if (IMAGE_CACHE.containsKey(path)) {
            return IMAGE_CACHE.get(path);
        }
        URL url = SpriteLoader.class.getResource(path);
        Image image = url == null ? null : new ImageIcon(url).getImage();
        IMAGE_CACHE.put(path, image);
        return image;
    }

    // ---------- Parte de instancia (Pokedex) ----------

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
        return loadAnimation(fileName(pokemonName, ""));
    }

    /**
     * @param pokemonName name of the Pokemon, in any case
     * @return the back animation, or null when there is no file for it
     */
    public synchronized GifAnimation back(String pokemonName) {
        return loadAnimation(fileName(pokemonName, BACK_SUFFIX));
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

    private GifAnimation loadAnimation(String file) {
        if (cache.containsKey(file)) {
            return cache.get(file);
        }
        URL url = SpriteLoader.class.getResource(FOLDER + file);
        GifAnimation animation = url == null ? null : GifAnimation.read(url);
        cache.put(file, animation);
        return animation;
    }
}
