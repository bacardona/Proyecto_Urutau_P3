package com.pokemon.battle.fighting;

import com.pokemon.battle.gamepad.PadSnapshot;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Pasa los botones del mando a FightingInput, igual que hacen las teclas.
 *
 * Botones (los mismos para el mando 1 y el mando 2):
 *   palanca o cruceta = caminar,  arriba o A = saltar,  abajo o LB = escudo,
 *   X / Y / B / RB = ataques 1 a 4,  LT = evolucionar,  RT = ultimate,
 *   SELECT (Back/View/Share) = cambiar de Pokemon en la pared,  START = pausa.
 *
 * Solo llama a FightingInput cuando un boton CAMBIA (se aprieta o se suelta).
 * Asi el teclado y el mando se pueden usar a la vez sin pisarse.
 */
final class GamepadFightControls {

    private final PadSnapshot[] previous = {PadSnapshot.NONE, PadSnapshot.NONE};

    /** Actualiza la pelea con la foto nueva del mando de un jugador (0 o 1). */
    void apply(int player, PadSnapshot now, FightingInput input) {
        PadSnapshot before = previous[player];
        if (now == null) now = PadSnapshot.NONE;
        boolean one = player == 0;
        send(before, now, PadSnapshot::left, one ? input::setPlayerOneLeft : input::setPlayerTwoLeft);
        send(before, now, PadSnapshot::right, one ? input::setPlayerOneRight : input::setPlayerTwoRight);
        send(before, now, p -> p.a() || p.up(), one ? input::setPlayerOneJump : input::setPlayerTwoJump);
        send(before, now, p -> p.lb() || p.down(), one ? input::setPlayerOneBlock : input::setPlayerTwoBlock);
        send(before, now, PadSnapshot::x, one ? input::setPlayerOneAttackOne : input::setPlayerTwoAttackOne);
        send(before, now, PadSnapshot::y, one ? input::setPlayerOneAttackTwo : input::setPlayerTwoAttackTwo);
        send(before, now, PadSnapshot::b, one ? input::setPlayerOneAttackThree : input::setPlayerTwoAttackThree);
        send(before, now, PadSnapshot::rb, one ? input::setPlayerOneAttackFour : input::setPlayerTwoAttackFour);
        send(before, now, PadSnapshot::lt, one ? input::setPlayerOneEvolution : input::setPlayerTwoEvolution);
        send(before, now, PadSnapshot::rt, one ? input::setPlayerOneUltimate : input::setPlayerTwoUltimate);
        send(before, now, PadSnapshot::back, one ? input::setPlayerOneSwitch : input::setPlayerTwoSwitch);
        previous[player] = now;
    }

    /** True solo en el instante en que el boton pasa de suelto a apretado. */
    boolean justPressed(int player, PadSnapshot now, Predicate<PadSnapshot> button) {
        return now != null && button.test(now) && !button.test(previous[player]);
    }

    /** Para menus (pausa y resultados): recuerda la foto sin tocar la pelea. */
    void remember(int player, PadSnapshot now) {
        previous[player] = now == null ? PadSnapshot.NONE : now;
    }

    /** Olvida lo que estaba apretado (al salir o entrar a la pelea). */
    void reset() {
        previous[0] = PadSnapshot.NONE;
        previous[1] = PadSnapshot.NONE;
    }

    private static void send(PadSnapshot before, PadSnapshot now, Predicate<PadSnapshot> button,
                             Consumer<Boolean> setter) {
        boolean was = button.test(before);
        boolean is = button.test(now);
        if (was != is) setter.accept(is);
    }
}
