package com.pokemon.battle.gamepad;

/**
 * Foto de un mando en un instante: que botones estan apretados.
 * Es inmutable, asi el hilo del mando la publica y el juego la lee sin problemas.
 */
public record PadSnapshot(boolean connected,
                          boolean left, boolean right, boolean up, boolean down,
                          boolean a, boolean b, boolean x, boolean y,
                          boolean lb, boolean rb, boolean lt, boolean rt,
                          boolean start, boolean back) {

    /** Mando desconectado: nada apretado. */
    public static final PadSnapshot NONE = new PadSnapshot(false,
            false, false, false, false, false, false, false, false,
            false, false, false, false, false, false);
}
