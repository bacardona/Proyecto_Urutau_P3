package com.pokemon.battle.fighting;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** Classpath-backed scenery only: all collision geometry remains in the session. */
final class FightingStageRenderer {
    private enum Decoration {
        TREE("arbol_redondo"), PINE("arbol_pino"), BUSH("arbusto_alto"), WIDE_BUSH("arbusto_ancho"),
        GRASS("cesped"), FLOWERS("flores_blancas"), BLUE_FLOWER("flor_azul"), LOG("tronco");

        final String filename;
        Decoration(String filename) { this.filename = filename; }
    }

    private record Placement(Decoration decoration, int x, double scale) { }

    private static final Placement[] GROUND_DECORATIONS = {
            new Placement(Decoration.TREE, 28, 0.72),
            new Placement(Decoration.PINE, 1250, 0.95),
            new Placement(Decoration.BUSH, 345, 0.42),
            new Placement(Decoration.WIDE_BUSH, 905, 0.38),
            new Placement(Decoration.BLUE_FLOWER, 165, 0.22),
            new Placement(Decoration.FLOWERS, 442, 0.22),
            new Placement(Decoration.GRASS, 782, 0.20),
            new Placement(Decoration.LOG, 1160, 0.30)
    };

    /** Shared lazy cache: images are decoded once and the terrain tile is resized once. */
    private static final class Resources {
        static final BufferedImage BACKGROUND = load("/Escenario/Fondo.jpg");
        static final Map<Decoration, BufferedImage> DECORATIONS = decorations();
        static final BufferedImage TERRAIN = terrain();

        private static Map<Decoration, BufferedImage> decorations() {
            var images = new EnumMap<Decoration, BufferedImage>(Decoration.class);
            for (var decoration : Decoration.values()) {
                images.put(decoration, load("/Escenario/decoraciones/" + decoration.filename + ".png"));
            }
            return Map.copyOf(images);
        }

        private static BufferedImage terrain() {
            var source = load("/Escenario/decoraciones/terreno.png");
            int height = 24;
            int width = (int) Math.round(source.getWidth() * height / (double) source.getHeight());
            var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();
            return result;
        }
    }

    private static BufferedImage load(String path) {
        try (var stream = FightingStageRenderer.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing stage resource: " + path);
            var image = ImageIO.read(stream);
            if (image == null) throw new IllegalStateException("Unsupported stage image: " + path);
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load stage resource: " + path, e);
        }
    }

    void draw(Graphics2D graphics, FightingGameSession session) {
        var g = (Graphics2D) graphics.create();
        g.clipRect(0, 0, session.getWidth(), session.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        drawBackground(g, session.getWidth(), session.getHeight());
        for (var placement : GROUND_DECORATIONS) {
            drawDecoration(g, placement.decoration, placement.x, session.getGroundY() + 1, placement.scale);
        }
        // Small details on the two outer ledges; their support comes from the existing platforms.
        var platforms = session.getPlatforms();
        if (platforms.size() > 1) {
            var left = platforms.get(0);
            var right = platforms.get(platforms.size() - 2);
            drawDecoration(g, Decoration.GRASS, left.getX() + left.getWidth() - 12, left.getTop() + 1, 0.16);
            drawDecoration(g, Decoration.FLOWERS, right.getX() + 14, right.getTop() + 1, 0.17);
        }
        for (var platform : platforms) {
            boolean ground = platform.getTop() == session.getGroundY();
            drawPlatform(g, platform, ground ? session.getHeight() - platform.getTop() : platform.getHeight(), ground);
        }
        g.dispose();
    }

    private void drawBackground(Graphics2D g, int width, int height) {
        var image = Resources.BACKGROUND;
        double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
        var transform = AffineTransform.getTranslateInstance(
                (width - image.getWidth() * scale) * 0.5, (height - image.getHeight() * scale) * 0.5);
        transform.scale(scale, scale);
        g.drawImage(image, transform, null);
    }

    private void drawDecoration(Graphics2D g, Decoration decoration, int centerX, int baseline, double scale) {
        var image = Resources.DECORATIONS.get(decoration);
        var transform = AffineTransform.getTranslateInstance(
                Math.round(centerX - image.getWidth() * scale * 0.5), Math.round(baseline - image.getHeight() * scale));
        transform.scale(scale, scale);
        g.drawImage(image, transform, null);
    }

    private void drawPlatform(Graphics2D graphics, Platform platform, int height, boolean ground) {
        var g = (Graphics2D) graphics.create();
        int x = platform.getX(), y = platform.getTop(), width = platform.getWidth();
        g.clipRect(x, y, width, height);
        g.setColor(new Color(0x362D29));
        g.fillRect(x, y, width, height);
        var tile = Resources.TERRAIN;
        // Preserve end caps; repeat the middle instead of stretching the terrain across a ledge.
        int topHeight = Math.min(height, ground ? 20 : tile.getHeight());
        drawTerrainBand(g, x, y, width, 0, topHeight);
        for (int row = topHeight; row < height; row += 8) {
            drawTerrainBand(g, x, y + row, width, 12, Math.min(8, height - row));
        }
        g.dispose();
    }

    private void drawTerrainBand(Graphics2D g, int x, int y, int width, int sourceY, int height) {
        var tile = Resources.TERRAIN;
        int cap = Math.min(6, width / 2);
        int middleWidth = tile.getWidth() - cap * 2;
        g.drawImage(tile, x, y, x + cap, y + height, 0, sourceY, cap, sourceY + height, null);
        for (int offset = cap; offset < width - cap; offset += middleWidth) {
            int count = Math.min(middleWidth, width - cap - offset);
            g.drawImage(tile, x + offset, y, x + offset + count, y + height,
                    cap, sourceY, cap + count, sourceY + height, null);
        }
        g.drawImage(tile, x + width - cap, y, x + width, y + height,
                tile.getWidth() - cap, sourceY, tile.getWidth(), sourceY + height, null);
    }
}
