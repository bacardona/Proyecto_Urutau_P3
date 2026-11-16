import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;

/** Run from the project root: java tools/ExtractStageDecorations.java. No image generation. */
public class ExtractStageDecorations {
    record Crop(String name, int x, int y, int width, int height) { }

    private static final List<Crop> CROPS = List.of(
            new Crop("terreno", 37, 144, 355, 114),
            new Crop("arbol_redondo", 45, 410, 216, 263),
            new Crop("arbol_pino", 343, 330, 172, 201),
            new Crop("arbusto_alto", 276, 541, 197, 132),
            new Crop("arbusto_ancho", 539, 532, 210, 142),
            new Crop("cesped", 208, 691, 143, 151),
            new Crop("flores_blancas", 50, 1019, 150, 138),
            new Crop("flor_azul", 605, 700, 110, 141),
            new Crop("tronco", 374, 717, 180, 106));

    public static void main(String[] args) throws Exception {
        Path directory = Path.of("src/main/resources/Escenario");
        var sheet = ImageIO.read(directory.resolve("Items_de_fondo.jpg").toFile());
        if (sheet == null || sheet.getWidth() != 800 || sheet.getHeight() != 1200)
            throw new IllegalArgumentException("Unexpected Items_de_fondo.jpg atlas dimensions");
        Path output = directory.resolve("decoraciones");
        Files.createDirectories(output);
        for (var crop : CROPS) {
            var result = extract(sheet, crop);
            ImageIO.write(result, "png", output.resolve(crop.name + ".png").toFile());
            System.out.printf("%s: %dx%d%n", crop.name, result.getWidth(), result.getHeight());
        }
    }

    private static BufferedImage extract(BufferedImage sheet, Crop crop) {
        int width = crop.width, height = crop.height;
        boolean[] background = new boolean[width * height];
        var queue = new ArrayDeque<Integer>();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1)
                enqueue(sheet, crop, x, y, background, queue);
        }
        while (!queue.isEmpty()) {
            int pixel = queue.removeFirst(), x = pixel % width, y = pixel / width;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (Math.abs(dx) + Math.abs(dy) != 1) continue;
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height)
                    enqueue(sheet, crop, nx, ny, background, queue);
            }
        }
        // Clear isolated JPEG ringing outside the connected object, without altering its pixels.
        boolean[] seen = new boolean[background.length];
        var largest = new java.util.ArrayList<Integer>();
        for (int index = 0; index < background.length; index++) {
            if (background[index] || seen[index]) continue;
            var component = new java.util.ArrayList<Integer>();
            queue.add(index); seen[index] = true;
            while (!queue.isEmpty()) {
                int pixel = queue.removeFirst(), x = pixel % width, y = pixel / width;
                component.add(pixel);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy, next = ny * width + nx;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && !background[next] && !seen[next]) {
                        seen[next] = true; queue.add(next);
                    }
                }
            }
            if (component.size() > largest.size()) largest = component;
        }
        if (largest.isEmpty()) throw new IllegalStateException("Empty crop: " + crop.name);
        int minX = width, minY = height, maxX = 0, maxY = 0;
        for (int pixel : largest) {
            minX = Math.min(minX, pixel % width); maxX = Math.max(maxX, pixel % width);
            minY = Math.min(minY, pixel / width); maxY = Math.max(maxY, pixel / width);
        }
        // One transparent pixel of padding. Copy RGB values exactly from the original JPG.
        var result = new BufferedImage(maxX - minX + 3, maxY - minY + 3, BufferedImage.TYPE_INT_ARGB);
        for (int pixel : largest) {
            int x = pixel % width, y = pixel / width;
            result.setRGB(x - minX + 1, y - minY + 1, sheet.getRGB(crop.x + x, crop.y + y));
        }
        return result;
    }

    private static void enqueue(BufferedImage sheet, Crop crop, int x, int y,
                                boolean[] background, ArrayDeque<Integer> queue) {
        int index = y * crop.width + x;
        if (background[index]) return;
        int rgb = sheet.getRGB(crop.x + x, crop.y + y);
        int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
        int min = Math.min(r, Math.min(g, b)), max = Math.max(r, Math.max(g, b));
        // Light, near-neutral graph paper AND JPEG edge ringing. White interior petals
        // remain opaque because this flood can only reach pixels from outside the object.
        if (min < 140 || max - min > 70) return;
        background[index] = true; queue.add(index);
    }
}
