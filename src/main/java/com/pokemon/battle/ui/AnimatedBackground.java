package com.pokemon.battle.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
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

    AnimatedBackground(String path) {
        this.path = path;
    }

    /** Dibuja el GIF entero dentro del area, centrado y sin deformarlo. */
    void draw(Graphics2D g, int x, int y, int w, int h, ImageObserver observer) {
        if (image == null) {
            URL url = AnimatedBackground.class.getResource(path);
            if (url == null) {
                return;   // sin imagen, la pantalla se ve como antes
            }
            image = Toolkit.getDefaultToolkit().createImage(url);
        }
        int iw = image.getWidth(observer);
        int ih = image.getHeight(observer);
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
        g.drawImage(image, dx, dy, dw, dh, observer);
    }

    /** Detiene la animacion y libera memoria; vuelve a empezar la proxima vez que se dibuje. */
    void stop() {
        if (image != null) {
            image.flush();
        }
    }
}