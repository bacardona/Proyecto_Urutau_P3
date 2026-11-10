package com.pokemon.battle.ui;

import com.pokemon.battle.model.Type;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads the animated sprites of the Pokemon from resources/sprites. Each file
 * is named after the Pokemon in capital letters, for example PIKACHU.gif, and
 * the back view ends in _ESPALDA (PIKACHU_ESPALDA.gif).
 * <p>
 * The Pokedex, the team builder and the battle all use the same instance
 * ({@link #shared()}), so they show the same files with the same animation.
 * Only the last animations used are kept in memory, so moving through the
 * whole list does not fill the memory.
 */
public class SpriteLoader {

    private static final String FOLDER = "/sprites/";
    private static final String BACK_SUFFIX = "_ESPALDA";
    private static final int CACHE_SIZE = 20;

    private static final SpriteLoader SHARED = new SpriteLoader();

    // Iconos de tipo ya escalados, por tipo y ancho
    private static final Map<String, Icon> TYPE_ICONS = new HashMap<>();

    // LinkedHashMap en orden de acceso: al pasar el limite se borra el que mas tiempo lleva sin usarse
    private final Map<String, GifAnimation> cache = new LinkedHashMap<String, GifAnimation>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, GifAnimation> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    // Archivos que ya se estan leyendo en el otro hilo, para no pedirlos dos veces
    private final Set<String> pending = ConcurrentHashMap.newKeySet();

    // Hilo aparte para leer los GIF sin trabar la ventana
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "sprite-loader");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * @return the loader used by every screen of the game
     */
    public static SpriteLoader shared() {
        return SHARED;
    }

    /**
     * Reads the front animation, waiting if it is not in memory yet.
     *
     * @param pokemonName name of the Pokemon, in any case
     * @return the front animation, or null when there is no file for it
     */
    public GifAnimation front(String pokemonName) {
        return loadAnimation(fileName(pokemonName, false));
    }

    /**
     * Reads the back animation (used in battle), waiting if it is not in memory yet.
     *
     * @param pokemonName name of the Pokemon, in any case
     * @return the back animation, or null when there is no file for it
     */
    public GifAnimation back(String pokemonName) {
        return loadAnimation(fileName(pokemonName, true));
    }

    /**
     * @param pokemonName name of the Pokemon, in any case
     * @return true when its front animation is already in memory
     */
    public boolean isLoaded(String pokemonName) {
        return isLoaded(pokemonName, false);
    }

    /**
     * @param pokemonName name of the Pokemon, in any case
     * @param back        true for the back view
     * @return true when that animation is already in memory (or is known to be missing)
     */
    public synchronized boolean isLoaded(String pokemonName, boolean back) {
        return cache.containsKey(fileName(pokemonName, back));
    }

    /**
     * Returns the animation only if it is already in memory. If it is not, it
     * starts reading it in the background and calls {@code onReady} on the
     * Swing thread when it is done, so the screen can repaint.
     *
     * @param pokemonName name of the Pokemon, in any case
     * @param back        true for the back view
     * @param onReady     what to do when the animation is ready, usually repaint
     * @return the animation, or null while it is loading or when there is no file
     */
    public GifAnimation getOrLoad(String pokemonName, boolean back, Runnable onReady) {
        String file = fileName(pokemonName, back);
        synchronized (this) {
            if (cache.containsKey(file)) {
                return cache.get(file);
            }
        }
        if (pending.add(file)) {
            worker.submit(() -> {
                try {
                    loadAnimation(file);
                } finally {
                    pending.remove(file);
                }
                if (onReady != null) {
                    SwingUtilities.invokeLater(onReady);
                }
            });
        }
        return null;
    }

    /**
     * Type icon from resources/tipos (the same one the Pokedex shows), scaled
     * to the given width keeping its proportion.
     *
     * @param type  type of the Pokemon or the move
     * @param width width wanted, in pixels
     * @return the icon, or null when the image does not exist (the screen writes text then)
     */
    public static synchronized Icon typeIcon(Type type, int width) {
        String key = type.name() + "@" + width;
        if (TYPE_ICONS.containsKey(key)) {
            return TYPE_ICONS.get(key);
        }
        Icon icon = null;
        URL url = SpriteLoader.class.getResource("/tipos/" + RetroStyle.typeName(type) + ".png");
        if (url != null) {
            ImageIcon original = new ImageIcon(url);
            int w = original.getIconWidth();
            int h = original.getIconHeight();
            if (w > 0 && h > 0) {
                int height = Math.max(1, width * h / w);
                // Escalado sin suavizar, para que el pixel art quede nitido como en la Pokedex
                BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaled.createGraphics();
                RetroStyle.drawPixelImage(g, toBuffered(original.getImage(), w, h), 0, 0, width, height);
                g.dispose();
                icon = new ImageIcon(scaled);
            }
        }
        TYPE_ICONS.put(key, icon);
        return icon;
    }

    private static BufferedImage toBuffered(Image image, int w, int h) {
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    // Nombre en mayusculas, sin espacios a los lados: "Pikachu " -> "PIKACHU.gif"
    private static String fileName(String pokemonName, boolean back) {
        return pokemonName.trim().toUpperCase() + (back ? BACK_SUFFIX : "") + ".gif";
    }

    // El GIF se lee fuera del bloqueo, asi la ventana puede seguir preguntando
    // por otros sprites mientras este se arma
    private GifAnimation loadAnimation(String file) {
        synchronized (this) {
            if (cache.containsKey(file)) {
                return cache.get(file);
            }
        }
        URL url = SpriteLoader.class.getResource(FOLDER + file);
        GifAnimation animation = url == null ? null : GifAnimation.read(url);
        synchronized (this) {
            cache.put(file, animation);
        }
        return animation;
    }
}
