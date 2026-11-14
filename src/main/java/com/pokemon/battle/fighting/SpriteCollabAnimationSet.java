package com.pokemon.battle.fighting;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cached reader for one PMD SpriteCollab multi-sheet animation package. */
final class SpriteCollabAnimationSet {

    static final int PIXEL_SCALE = 4;
    static final int GAME_TICKS_PER_SECOND = 60;
    static final int EAST_ROW = 2;
    static final int WEST_ROW = 6;
    static final int SINGLE_ROW = 0;
    static final int FRAME_EMPTY = -1;

    record Frame(BufferedImage image, int bodyX, int bodyY, int headX, int headY,
                 int shadowX, int shadowY) { }

    static final class Animation {
        private final String name;
        private final int frameWidth;
        private final int frameHeight;
        private final int[] durations;
        private final int hitFrame;
        private final int returnFrame;
        private final Frame[][] directions;
        private final double durationSeconds;

        private Animation(String name, int frameWidth, int frameHeight, int[] durations,
                          int hitFrame, int returnFrame, Frame[][] directions) {
            this.name = name;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.durations = durations;
            this.hitFrame = hitFrame;
            this.returnFrame = returnFrame;
            this.directions = directions;
            int totalTicks = 0;
            for (int duration : durations) totalTicks += duration;
            this.durationSeconds = totalTicks / (double) GAME_TICKS_PER_SECOND;
        }

        String name() { return name; }
        int frameWidth() { return frameWidth; }
        int frameHeight() { return frameHeight; }
        int frameCount() { return durations.length; }
        int directionCount() { return directions.length; }
        int hitFrame() { return hitFrame; }
        int returnFrame() { return returnFrame; }
        double durationSeconds() { return durationSeconds; }

        Frame frameAtTime(double elapsedSeconds, boolean facingLeft, boolean loop) {
            int index = frameIndexAtTime(elapsedSeconds, loop);
            return frame(facingLeft, index);
        }

        Frame frameAtProgress(double progress, boolean facingLeft) {
            double clamped = Math.max(0, Math.min(1, progress));
            int totalTicks = 0;
            for (int duration : durations) totalTicks += duration;
            int tick = Math.min(totalTicks - 1,
                    (int) Math.floor(clamped * totalTicks));
            int index = indexAtTick(tick);
            return frame(facingLeft, index);
        }

        Frame frameAtProgressInRange(double progress, boolean facingLeft,
                                     int firstFrame, int pastLastFrame) {
            int start = Math.max(0, Math.min(frameCount() - 1, firstFrame));
            int end = Math.max(start + 1, Math.min(frameCount(), pastLastFrame));
            int totalTicks = 0;
            for (int i = start; i < end; i++) totalTicks += durations[i];
            int tick = Math.min(totalTicks - 1,
                    (int) Math.floor(Math.max(0, Math.min(1, progress)) * totalTicks));
            for (int i = start; i < end; i++) {
                if (tick < durations[i]) return frame(facingLeft, i);
                tick -= durations[i];
            }
            return frame(facingLeft, end - 1);
        }

        Frame frame(boolean facingLeft, int frameIndex) {
            int row = directions.length > WEST_ROW
                    ? (facingLeft ? WEST_ROW : EAST_ROW) : SINGLE_ROW;
            row = Math.min(row, directions.length - 1);
            return directions[row][Math.max(0, Math.min(frameCount() - 1, frameIndex))];
        }

        private int frameIndexAtTime(double elapsedSeconds, boolean loop) {
            int totalTicks = 0;
            for (int duration : durations) totalTicks += duration;
            int tick = Math.max(0, (int) Math.floor(elapsedSeconds * GAME_TICKS_PER_SECOND));
            if (loop) tick %= totalTicks;
            else tick = Math.min(tick, totalTicks - 1);
            return indexAtTick(tick);
        }

        private int indexAtTick(int tick) {
            int elapsed = 0;
            for (int i = 0; i < durations.length; i++) {
                elapsed += durations[i];
                if (tick < elapsed) return i;
            }
            return durations.length - 1;
        }
    }

    private record Definition(String name, int frameWidth, int frameHeight,
                              int[] durations, int hitFrame, int returnFrame,
                              String copyOf) { }

    private final String resourceRoot;
    private final Map<String, Animation> animations;

    SpriteCollabAnimationSet(String speciesDirectory) {
        this(speciesDirectory, "Sprites_nuevos");
    }

    SpriteCollabAnimationSet(String speciesDirectory, String packageDirectory) {
        if (speciesDirectory == null || !speciesDirectory.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("Directorio de especie inválido");
        }
        if (!"Sprites_nuevos".equals(packageDirectory) && !"Evolucion".equals(packageDirectory)) {
            throw new IllegalArgumentException("Paquete de sprites inválido");
        }
        this.resourceRoot = "/sprites/fighting/" + speciesDirectory + "/" + packageDirectory + "/";
        animations = loadAll();
    }

    Animation get(String name) {
        return animations.get(name);
    }

    private Map<String, Animation> loadAll() {
        Map<String, Animation> result = new HashMap<>();
        try (InputStream stream = SpriteCollabAnimationSet.class
                .getResourceAsStream(resourceRoot + "AnimData.xml")) {
            if (stream == null) return missing("AnimData.xml: no se encontró el recurso");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            NodeList nodes = factory.newDocumentBuilder().parse(stream)
                    .getDocumentElement().getElementsByTagName("Anim");
            Map<String, Definition> definitions = new HashMap<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String name = childText(element, "Name");
                if (name == null) continue;
                String copyOf = childText(element, "CopyOf");
                int frameWidth = parsePositive(childText(element, "FrameWidth"));
                int frameHeight = parsePositive(childText(element, "FrameHeight"));
                int[] durations = durations(element);
                int hitFrame = parseFrame(childText(element, "HitFrame"));
                int returnFrame = parseFrame(childText(element, "ReturnFrame"));
                definitions.put(name, new Definition(name, frameWidth, frameHeight,
                        durations, hitFrame, returnFrame, copyOf));
            }

            // Load concrete sheets before resolving aliases (Shoot -> Charge on Charmander).
            for (Definition definition : definitions.values()) {
                if (definition.copyOf() == null) {
                    Animation animation = loadAnimation(definition);
                    if (animation != null) result.put(definition.name(), animation);
                }
            }
            for (Definition definition : definitions.values()) {
                if (definition.copyOf() != null) {
                    Animation source = result.get(definition.copyOf());
                    if (source != null) result.put(definition.name(), source);
                }
            }
        } catch (Exception exception) {
            System.err.println("No se pudo cargar " + resourceRoot + "AnimData.xml: "
                    + exception.getMessage());
        }
        return result;
    }

    private Animation loadAnimation(Definition definition) {
        String name = definition.name();
        String imagePath = resourceRoot + name + "-Anim.png";
        String offsetsPath = resourceRoot + name + "-Offsets.png";
        String shadowPath = resourceRoot + name + "-Shadow.png";
        try (InputStream imageStream = resource(imagePath);
             InputStream offsetsStream = resource(offsetsPath);
             InputStream shadowStream = resource(shadowPath)) {
            if (imageStream == null || offsetsStream == null || shadowStream == null) {
                return missingAnimation(name, "falta uno de los PNG de la animación");
            }
            BufferedImage sheet = ImageIO.read(imageStream);
            BufferedImage offsets = ImageIO.read(offsetsStream);
            BufferedImage shadows = ImageIO.read(shadowStream);
            if (!validSheets(sheet, offsets, shadows, definition)) {
                return missingAnimation(name, "dimensiones no coinciden con AnimData.xml");
            }

            int columns = sheet.getWidth() / definition.frameWidth();
            int rows = sheet.getHeight() / definition.frameHeight();
            if (columns != definition.durations().length || (rows != 1 && rows != 8)) {
                return missingAnimation(name, "la cuadrícula no coincide con sus frames/direcciones");
            }
            Frame[][] frames = new Frame[rows][columns];
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    int x = column * definition.frameWidth();
                    int y = row * definition.frameHeight();
                    int[] body = locate(offsets, x, y, definition.frameWidth(),
                            definition.frameHeight(), Marker.BODY);
                    int[] head = locate(offsets, x, y, definition.frameWidth(),
                            definition.frameHeight(), Marker.HEAD);
                    int[] shadow = locate(shadows, x, y, definition.frameWidth(),
                            definition.frameHeight(), Marker.SHADOW);
                    if (body == null) return missingAnimation(name, "falta el ancla del cuerpo");
                    if (head == null) head = body;
                    if (shadow == null) shadow = new int[]{
                            definition.frameWidth() / 2, definition.frameHeight() - 1};
                    frames[row][column] = new Frame(
                            sheet.getSubimage(x, y, definition.frameWidth(), definition.frameHeight()),
                            body[0], body[1], head[0], head[1], shadow[0], shadow[1]);
                }
            }
            return new Animation(name, definition.frameWidth(), definition.frameHeight(),
                    definition.durations(), definition.hitFrame(), definition.returnFrame(),
                    frames);
        } catch (Exception exception) {
            return missingAnimation(name, exception.getMessage());
        }
    }

    private boolean validSheets(BufferedImage image, BufferedImage offsets,
                                BufferedImage shadows, Definition definition) {
        if (image == null || offsets == null || shadows == null
                || !image.getColorModel().hasAlpha()
                || definition.frameWidth() <= 0 || definition.frameHeight() <= 0) return false;
        return image.getWidth() == offsets.getWidth()
                && image.getHeight() == offsets.getHeight()
                && image.getWidth() == shadows.getWidth()
                && image.getHeight() == shadows.getHeight()
                && image.getWidth() % definition.frameWidth() == 0
                && image.getHeight() % definition.frameHeight() == 0;
    }

    private enum Marker { BODY, HEAD, SHADOW }

    private int[] locate(BufferedImage image, int startX, int startY,
                         int width, int height, Marker marker) {
        int fallbackX = -1;
        int fallbackY = -1;
        int fallbackScore = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(startX + x, startY + y);
                if ((pixel >>> 24) == 0) continue;
                int rgb = pixel & 0x00FFFFFF;
                if (marker == Marker.BODY && rgb == 0x0000FF00) return new int[]{x, y};
                if (marker == Marker.HEAD && rgb == 0x00000000) return new int[]{x, y};
                if (marker == Marker.SHADOW && rgb == 0x00FFFFFF) return new int[]{x, y};

                int green = (rgb >>> 8) & 0xFF;
                if (marker == Marker.BODY && green == 0xFF && green > fallbackScore) {
                    fallbackX = x;
                    fallbackY = y;
                    fallbackScore = green;
                }
            }
        }
        if (fallbackX >= 0) return new int[]{fallbackX, fallbackY};
        return null;
    }

    private int[] durations(Element element) {
        NodeList nodes = element.getElementsByTagName("Duration");
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            int value = parsePositive(nodes.item(i).getTextContent());
            if (value > 0) values.add(value);
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private String childText(Element element, String name) {
        NodeList children = element.getElementsByTagName(name);
        return children.getLength() == 0 ? null : children.item(0).getTextContent().trim();
    }

    private int parsePositive(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int parseFrame(String value) {
        if (value == null) return -1;
        try {
            int frame = Integer.parseInt(value.trim());
            return frame >= 0 ? frame : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private InputStream resource(String path) {
        return SpriteCollabAnimationSet.class.getResourceAsStream(path);
    }

    private Animation missingAnimation(String name, String reason) {
        System.err.println("Animación omitida en " + resourceRoot + " (" + reason + "): " + name);
        return null;
    }

    private Map<String, Animation> missing(String reason) {
        System.err.println("Paquete SpriteCollab omitido en " + resourceRoot + " (" + reason + ")");
        return new HashMap<>();
    }
}
