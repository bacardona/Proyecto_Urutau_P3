package com.pokemon.battle.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.net.URL;

/**
 * Fondo animado a partir de un GIF. Usa la carga de AWT, que va mostrando un cuadro a la vez,
 * en vez de decodificar todos los cuadros en memoria.
 */
final class AnimatedBackground {

    // Colores de los bordes del GIF, para rellenar los lados que quedan libres
    private static final Color LEFT_FILL = new Color(0x22382D);
    private static final Color RIGHT_FILL = new Color(0x0C211E);

    private final String path;
    private Image image;

    // Imagenes pequenas intermedias para el modo difuminado (se reutilizan en cada dibujo)
    private BufferedImage stepA;
    private BufferedImage stepB;

    AnimatedBackground(String path) {
        this.path = path;
    }

    // Carga el GIF la primera vez; devuelve null si no se encuentra el archivo
    private Image load() {
        if (image == null) {
            URL url = AnimatedBackground.class.getResource(path);
            if (url == null) {
                return null;
            }
            image = Toolkit.getDefaultToolkit().createImage(url);
        }
        return image;
    }

    /** Dibuja el GIF entero dentro del area, centrado y sin deformarlo. */
    void draw(Graphics2D g, int x, int y, int w, int h, ImageObserver observer) {
        Image img = load();
        if (img == null) {
            return;   // sin imagen, la pantalla se ve como antes
        }
        int iw = img.getWidth(observer);
        int ih = img.getHeight(observer);
        if (iw <= 0 || ih <= 0) {
            return;   // todavia esta leyendo el archivo; al terminar se vuelve a dibujar
        }
        double scale = Math.min(w / (double) iw, h / (double) ih);
        int dw = (int) Math.round(iw * scale);
        int dh = (int) Math.round(ih * scale);
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        g.setColor(LEFT_FILL);
        g.fillRect(x, y, dx - x, h);
        g.setColor(RIGHT_FILL);
        g.fillRect(dx + dw, y, x + w - (dx + dw), h);
        g.drawImage(img, dx, dy, dw, dh, observer);
    }

    /**
     * Dibuja el mismo GIF pero borroso, llenando toda el area (sin franjas a los lados).
     * El desenfoque se logra reduciendo la imagen en dos pasos y agrandandola despues
     * con suavizado, asi es barato aunque el GIF se este animando.
     */
    void drawBlurred(Graphics2D g, int x, int y, int w, int h, ImageObserver observer) {
        Image img = load();
        if (img == null) {
            g.setColor(RIGHT_FILL);
            g.fillRect(x, y, w, h);
            return;
        }
        int iw = img.getWidth(observer);
        int ih = img.getHeight(observer);
        if (iw <= 0 || ih <= 0) {
            g.setColor(RIGHT_FILL);
            g.fillRect(x, y, w, h);
            return;   // todavia esta leyendo el archivo
        }

        // Paso 1: el GIF a un cuarto de su tamano
        int aw = Math.max(1, iw / 4);
        int ah = Math.max(1, ih / 4);
        if (stepA == null || stepA.getWidth() != aw || stepA.getHeight() != ah) {
            stepA = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D ga = stepA.createGraphics();
        ga.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        ga.drawImage(img, 0, 0, aw, ah, observer);
        ga.dispose();

        // Paso 2: aun mas pequeno y recortado para cubrir toda el area
        int bw = Math.max(8, w / 20);
        int bh = Math.max(8, h / 20);
        if (stepB == null || stepB.getWidth() != bw || stepB.getHeight() != bh) {
            stepB = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_RGB);
        }
        double cover = Math.max(bw / (double) aw, bh / (double) ah);
        int cw = (int) Math.round(aw * cover);
        int ch = (int) Math.round(ah * cover);
        Graphics2D gb = stepB.createGraphics();
        gb.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gb.drawImage(stepA, (bw - cw) / 2, (bh - ch) / 2, cw, ch, null);
        gb.dispose();

        // Paso 3: se agranda al tamano de la ventana con suavizado
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(stepB, x, y, w, h, null);
        if (old != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        }
    }

    /** Detiene la animacion y libera memoria; vuelve a empezar la proxima vez que se dibuje. */
    void stop() {
        if (image != null) {
            image.flush();
        }
    }
}