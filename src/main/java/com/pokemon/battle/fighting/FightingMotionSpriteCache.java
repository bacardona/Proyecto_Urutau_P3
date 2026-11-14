package com.pokemon.battle.fighting;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Loads and crops the optional base-form locomotion sheets once per renderer. */
final class FightingMotionSpriteCache {

    static final int FRAME_SIZE = 64;
    static final int WALK_FRAMES_PER_SECOND = 11;
    static final int BLOCK_FRAMES_PER_SECOND = 14;
    static final double BLOCK_RELEASE_SECONDS = 0.20;

    enum Action {
        WALK("caminar.png", 8),
        JUMP("saltar.png", 9),
        BLOCK("bloqueo.png", 6);

        private final String file;
        private final int frameCount;

        Action(String file, int frameCount) {
            this.file = file;
            this.frameCount = frameCount;
        }

        int frameCount() { return frameCount; }
    }

    record Frame(BufferedImage image, int offsetX) { }

    static final class Sheet {
        private final Frame[] frames;
        private final int contentWidth;
        private final int contentHeight;

        private Sheet(Frame[] frames, int contentWidth, int contentHeight) {
            this.frames = frames;
            this.contentWidth = contentWidth;
            this.contentHeight = contentHeight;
        }

        int frameCount() { return frames.length; }
        Frame frame(int index) { return frames[Math.floorMod(index, frames.length)]; }
        int contentWidth() { return contentWidth; }
        int contentHeight() { return contentHeight; }
    }

    private static final String[] SUPPORTED_BASE_FORMS = {
            "BULBASAUR", "CHARMANDER", "SQUIRTLE", "EEVEE", "PIKACHU"
    };

    private final Map<String, EnumMap<Action, Sheet>> sheets = new HashMap<>();

    FightingMotionSpriteCache() {
        for (String species : SUPPORTED_BASE_FORMS) {
            EnumMap<Action, Sheet> speciesSheets = new EnumMap<>(Action.class);
            for (Action action : Action.values()) {
                Sheet sheet = load(species, action);
                if (sheet != null) speciesSheets.put(action, sheet);
            }
            sheets.put(species, speciesSheets);
        }
    }

    Sheet get(String species, Action action) {
        Map<Action, Sheet> speciesSheets = sheets.get(species);
        return speciesSheets == null ? null : speciesSheets.get(action);
    }

    private Sheet load(String species, Action action) {
        String path = "/sprites/fighting/" + species + "/" + action.file;
        BufferedImage source;
        try (InputStream stream = FightingMotionSpriteCache.class.getResourceAsStream(path)) {
            if (stream == null) return missing(path, "recurso no encontrado");
            source = ImageIO.read(stream);
        } catch (IOException | RuntimeException exception) {
            return missing(path, "no se pudo leer: " + exception.getMessage());
        }

        int expectedWidth = action.frameCount * FRAME_SIZE;
        if (source == null || source.getWidth() != expectedWidth
                || source.getHeight() != FRAME_SIZE || !source.getColorModel().hasAlpha()) {
            return missing(path, "se esperaba " + expectedWidth + "x" + FRAME_SIZE
                    + " con canal alfa");
        }

        Rectangle[] bounds = new Rectangle[action.frameCount];
        Rectangle union = null;
        for (int i = 0; i < action.frameCount; i++) {
            bounds[i] = alphaBounds(source, i * FRAME_SIZE);
            if (bounds[i] != null) {
                if (union == null) union = new Rectangle(bounds[i]);
                else union.add(bounds[i]);
            }
        }
        if (union == null || union.isEmpty()) return missing(path, "no contiene píxeles visibles");

        Frame[] frames = new Frame[action.frameCount];
        for (int i = 0; i < action.frameCount; i++) {
            Rectangle frameBounds = bounds[i];
            if (frameBounds == null) continue;
            BufferedImage cropped = source.getSubimage(i * FRAME_SIZE + frameBounds.x, frameBounds.y,
                    frameBounds.width, frameBounds.height);
            frames[i] = new Frame(cropped, frameBounds.x - union.x);
        }
        return new Sheet(frames, union.width, union.height);
    }

    private Rectangle alphaBounds(BufferedImage sheet, int frameX) {
        int minX = FRAME_SIZE;
        int minY = FRAME_SIZE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < FRAME_SIZE; y++) {
            for (int x = 0; x < FRAME_SIZE; x++) {
                if ((sheet.getRGB(frameX + x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < 0 ? null
                : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private Sheet missing(String path, String reason) {
        System.err.println("Spritesheet de Combate Libre omitido (" + reason + "): " + path);
        return null;
    }
}
