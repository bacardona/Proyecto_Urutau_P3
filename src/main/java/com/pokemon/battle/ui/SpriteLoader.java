package com.pokemon.battle.ui;

import com.pokemon.battle.model.MoveCategory;
import com.pokemon.battle.model.Type;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class SpriteLoader {

    private static final Map<String, Image> CACHE = new HashMap<>();

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
