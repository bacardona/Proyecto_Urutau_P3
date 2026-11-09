package com.pokemon.battle.ui;

import com.pokemon.battle.model.MoveCategory;
import com.pokemon.battle.model.Type;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class SpriteLoader {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private static final Map<String, Icon> ICON_CACHE = new HashMap<>();

    // Icono de tipo escalado al ancho pedido, manteniendo la proporción.
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

    private SpriteLoader() {
    }

    public static Image pokemonFront(String name) {
        return load("/sprites/pokemon/front/" + clean(name) + ".gif");
    }

    public static Image pokemonBack(String name) {
        return load("/sprites/pokemon/back/" + clean(name) + ".gif");
    }

    public static Image type(Type type) {
        return load("/sprites/types/" + type.name().toLowerCase() + ".png");
    }

    public static Image category(MoveCategory category) {
        return load("/sprites/categories/" + category.name().toLowerCase() + ".png");
    }

    private static String clean(String name) {
        return name.trim().toLowerCase();
    }

    // Devuelve null si la imagen no existe, para que la pantalla use un marcador
    private static Image load(String path) {
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }
        URL url = SpriteLoader.class.getResource(path);
        Image image = url == null ? null : new ImageIcon(url).getImage();
        CACHE.put(path, image);
        return image;
    }
}
