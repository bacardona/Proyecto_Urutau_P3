package com.pokemon.battle.persistence.codec;

import java.util.List;

/**
 * Registry with the codecs of every kind of move. To support a new kind of
 * move, create its codec and add it here.
 */
public final class MoveCsvCodecs {

    private MoveCsvCodecs() {
    }

    /**
     * @return the codecs of all the moves known by the game
     */
    public static List<MoveCsvCodec> standard() {
        return List.of(
                new DamageMoveCodec(),
                new DamageWithStatusMoveCodec(),
                new DamageWithFlinchMoveCodec(),
                new InflictStatusMoveCodec(),
                new StatChangeMoveCodec(),
                new TwoTurnMoveCodec());
    }
}
