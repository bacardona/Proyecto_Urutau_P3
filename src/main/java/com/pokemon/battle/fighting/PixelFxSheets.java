package com.pokemon.battle.fighting;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Efectos animados hechos a mano en pixel art (resources/efectos/fxN.png, "Pixel FX Pack"
 * de CodeManu, CC0). Cada imagen es una cuadricula de cuadros de 100x100; se reproducen
 * una vez en el punto del golpe y desaparecen.
 */
final class PixelFxSheets {

    /** Hojas que se usan y para que. */
    enum Sheet {
        SPARK("fx1"),      // chispas azules/blancas: golpe electrico o fisico
        TWIRL("fx2"),      // espiral rosada
        DROPS("fx3"),      // gotas celestes
        TWINKLE("fx4"),    // destellos pequenos
        SPLASH("fx5"),     // salpicadura hacia arriba
        BURST("fx6"),      // explosion de fuego
        FIRE_RING("fx7"),  // aro de fuego
        RING("fx8"),       // aro verde/rosado
        FLAME("fx9"),      // llamita
        SHOCKWAVE("fx10"); // onda de choque blanca

        final String file;

        Sheet(String file) {
            this.file = file;
        }
    }

    private static final int CELL = 100;
    private static final Map<Sheet, BufferedImage[]> FRAMES = new EnumMap<>(Sheet.class);

    private static final class Play {
        Sheet sheet;
        double x;
        double y;
        double scale;
        double age;
        double duration;
        boolean flip;
    }

    private final List<Play> playing = new ArrayList<>();

    /** Reproduce una hoja centrada en (x, y). {@code scale} 3 = un pixel del efecto ocupa 3x3. */
    void play(Sheet sheet, double x, double y, double scale, double duration, boolean flip) {
        if (frames(sheet).length == 0) return;
        Play p = new Play();
        p.sheet = sheet;
        p.x = x;
        p.y = y;
        p.scale = scale;
        p.duration = duration;
        p.flip = flip;
        playing.add(p);
    }

    void tick(double dt) {
        for (Iterator<Play> it = playing.iterator(); it.hasNext(); ) {
            Play p = it.next();
            p.age += dt;
            if (p.age >= p.duration) it.remove();
        }
    }

    void clear() {
        playing.clear();
    }

    void draw(Graphics2D g) {
        if (playing.isEmpty()) return;
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (Play p : playing) {
            BufferedImage[] frames = frames(p.sheet);
            int index = Math.min(frames.length - 1, (int) (p.age / p.duration * frames.length));
            BufferedImage frame = frames[index];
            int size = (int) Math.round(CELL * p.scale);
            int left = (int) Math.round(p.x - size / 2.0);
            int top = (int) Math.round(p.y - size / 2.0);
            if (p.flip) g.drawImage(frame, left + size, top, -size, size, null);
            else g.drawImage(frame, left, top, size, size, null);
        }
        if (old != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
    }

    /** Corta la hoja en cuadros de 100x100 y quita los cuadros vacios del final. */
    private static synchronized BufferedImage[] frames(Sheet sheet) {
        BufferedImage[] cached = FRAMES.get(sheet);
        if (cached != null) return cached;
        List<BufferedImage> list = new ArrayList<>();
        try (InputStream in = PixelFxSheets.class.getResourceAsStream("/efectos/" + sheet.file + ".png")) {
            BufferedImage img = in == null ? null : ImageIO.read(in);
            if (img != null) {
                int cols = img.getWidth() / CELL;
                int rows = img.getHeight() / CELL;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        list.add(img.getSubimage(c * CELL, r * CELL, CELL, CELL));
                    }
                }
                while (!list.isEmpty() && isEmpty(list.get(list.size() - 1))) list.remove(list.size() - 1);
                while (!list.isEmpty() && isEmpty(list.get(0))) list.remove(0);
            }
        } catch (Exception e) {
            list.clear();
        }
        BufferedImage[] result = list.toArray(new BufferedImage[0]);
        FRAMES.put(sheet, result);
        return result;
    }

    private static boolean isEmpty(BufferedImage frame) {
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                if ((frame.getRGB(x, y) >>> 24) > 20) return false;
            }
        }
        return true;
    }
}
