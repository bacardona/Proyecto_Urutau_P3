package com.pokemon.battle.ui;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Frames of an animated GIF, already combined and cut to the visible part of
 * the Pokemon. The frames are built here (and not by Swing) because the GIF
 * player of Java leaves black boxes and cut parts on some sprites. Playing the
 * frames at a fixed speed also makes every Pokemon move at the same rhythm.
 */
public final class GifAnimation {

    /**
     * Frames per second used by every screen, so all the Pokemon move at the same rhythm.
     */
    public static final int FPS = 12;

    private final BufferedImage[] frames;

    private GifAnimation(BufferedImage[] frames) {
        this.frames = frames;
    }

    /**
     * @return number of frames of the animation
     */
    public int frameCount() {
        return frames.length;
    }

    /**
     * @param index frame number; it wraps around, so any counter can be used
     * @return the frame to draw
     */
    public BufferedImage frame(int index) {
        return frames[Math.floorMod(index, frames.length)];
    }

    /**
     * @return width of the visible part of the Pokemon, in pixels
     */
    public int width() {
        return frames[0].getWidth();
    }

    /**
     * @return height of the visible part of the Pokemon, in pixels
     */
    public int height() {
        return frames[0].getHeight();
    }

    /**
     * Reads a GIF and builds all its frames.
     *
     * @param url location of the file
     * @return the animation, or null when the file can not be read
     */
    public static GifAnimation read(URL url) {
        try (InputStream in = url.openStream();
             ImageInputStream stream = ImageIO.createImageInputStream(in)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, false);
                List<BufferedImage> full = composeFrames(reader);
                return full.isEmpty() ? null : new GifAnimation(cropToContent(full));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // Arma cada cuadro completo respetando como el GIF dice que se borra el cuadro anterior
    private static List<BufferedImage> composeFrames(ImageReader reader) throws IOException {
        int count = reader.getNumImages(true);
        int[] screen = logicalScreen(reader, count);
        BufferedImage canvas = new BufferedImage(screen[0], screen[1], BufferedImage.TYPE_INT_ARGB);
        List<BufferedImage> result = new ArrayList<>();

        String previousDisposal = "none";
        int[] previousArea = null;
        BufferedImage saved = null;

        for (int i = 0; i < count; i++) {
            BufferedImage raw = reader.read(i);
            Node meta = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
            int[] area = frameArea(meta, raw);
            String disposal = disposalOf(meta);

            Graphics2D g = canvas.createGraphics();
            // Primero se aplica lo que el cuadro anterior pidio hacer al terminar
            if ("restoreToBackgroundColor".equals(previousDisposal) && previousArea != null) {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(previousArea[0], previousArea[1], previousArea[2], previousArea[3]);
                g.setComposite(AlphaComposite.SrcOver);
            } else if ("restoreToPrevious".equals(previousDisposal) && saved != null) {
                g.setComposite(AlphaComposite.Src);
                g.drawImage(saved, 0, 0, null);
                g.setComposite(AlphaComposite.SrcOver);
            }
            if ("restoreToPrevious".equals(disposal)) {
                saved = copy(canvas);
            }
            g.drawImage(raw, area[0], area[1], null);
            g.dispose();

            result.add(copy(canvas));
            previousDisposal = disposal;
            previousArea = area;
        }
        return result;
    }

    private static int[] logicalScreen(ImageReader reader, int count) throws IOException {
        int width = 0;
        int height = 0;
        IIOMetadata streamMeta = reader.getStreamMetadata();
        if (streamMeta != null) {
            Node root = streamMeta.getAsTree("javax_imageio_gif_stream_1.0");
            Node screen = child(root, "LogicalScreenDescriptor");
            if (screen != null) {
                width = intAttr(screen, "logicalScreenWidth", 0);
                height = intAttr(screen, "logicalScreenHeight", 0);
            }
        }
        // Algunos GIF traen mal el tamano de pantalla: se agranda para que quepan todos los cuadros
        for (int i = 0; i < count; i++) {
            Node meta = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
            Node desc = child(meta, "ImageDescriptor");
            if (desc != null) {
                width = Math.max(width, intAttr(desc, "imageLeftPosition", 0) + intAttr(desc, "imageWidth", 0));
                height = Math.max(height, intAttr(desc, "imageTopPosition", 0) + intAttr(desc, "imageHeight", 0));
            }
        }
        return new int[]{Math.max(1, width), Math.max(1, height)};
    }

    private static int[] frameArea(Node meta, BufferedImage raw) {
        Node desc = child(meta, "ImageDescriptor");
        int x = desc == null ? 0 : intAttr(desc, "imageLeftPosition", 0);
        int y = desc == null ? 0 : intAttr(desc, "imageTopPosition", 0);
        return new int[]{x, y, raw.getWidth(), raw.getHeight()};
    }

    private static String disposalOf(Node meta) {
        Node gce = child(meta, "GraphicControlExtension");
        if (gce == null) {
            return "none";
        }
        Node value = gce.getAttributes().getNamedItem("disposalMethod");
        return value == null ? "none" : value.getNodeValue();
    }

    // Recorta todos los cuadros al rectangulo donde aparece el Pokemon en alguno de ellos
    private static BufferedImage[] cropToContent(List<BufferedImage> full) {
        int width = full.get(0).getWidth();
        int height = full.get(0).getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (BufferedImage frame : full) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if ((frame.getRGB(x, y) >>> 24) != 0) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
        }
        if (maxX < 0) {
            return full.toArray(new BufferedImage[0]);
        }
        BufferedImage[] cropped = new BufferedImage[full.size()];
        for (int i = 0; i < full.size(); i++) {
            cropped[i] = copy(full.get(i).getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1));
        }
        return cropped;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static Node child(Node parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (name.equals(n.getNodeName())) {
                return n;
            }
        }
        return null;
    }

    private static int intAttr(Node node, String name, int fallback) {
        NamedNodeMap attrs = node.getAttributes();
        Node value = attrs == null ? null : attrs.getNamedItem(name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.getNodeValue());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
