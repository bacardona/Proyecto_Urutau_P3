package com.pokemon.battle.gamepad;

/** Checks which pad may move the menus (both pads, or only the one of the player choosing). Run with main. */
public class GamepadMenuCheck {

    public static void main(String[] args) {
        GamepadInput.setPadsSwapped(false);
        // Menus sin jugador: los dos mandos sirven por igual
        check(GamepadInput.padMayNavigate(0, -1, true), "Pad 1 moves shared menus");
        check(GamepadInput.padMayNavigate(1, -1, true), "Pad 2 moves shared menus");
        // Eligiendo el jugador 1: solo su mando
        check(GamepadInput.padMayNavigate(0, 0, true), "Pad 1 moves player 1 screens");
        check(!GamepadInput.padMayNavigate(1, 0, true), "Pad 2 is off while player 1 chooses");
        // Eligiendo el jugador 2: solo su mando
        check(GamepadInput.padMayNavigate(1, 1, true), "Pad 2 moves player 2 screens");
        check(!GamepadInput.padMayNavigate(0, 1, true), "Pad 1 is off while player 2 chooses");
        // Con un solo mando conectado, ese mando sirve para los dos jugadores
        check(GamepadInput.padMayNavigate(0, 1, false), "With one pad it works for player 2 too");
        // MANDO: LADO DERECHO -> el mando 1 es del jugador 2
        GamepadInput.setPadsSwapped(true);
        check(GamepadInput.padForPlayer(1) == 0, "Swapped: pad 1 belongs to player 2");
        check(GamepadInput.padMayNavigate(0, 1, true), "Swapped: pad 1 moves player 2 screens");
        check(!GamepadInput.padMayNavigate(0, 0, true), "Swapped: pad 1 is off for player 1");
        GamepadInput.setPadsSwapped(false);
        System.out.println("PASS gamepad menus");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
