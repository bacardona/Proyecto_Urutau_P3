package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.*;

/**
 * CSV codec for {@link DamageMove}.
 */
public class DamageMoveCodec implements MoveCsvCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    public String kind() {
        return "DAMAGE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Move move) {
        return move.getClass() == DamageMove.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move parse(MoveCommonFields c, String[] p) {
        return new DamageMove(c.id(), c.name(), c.type(), c.category(), c.power(), c.accuracy(), c.pp(), c.priority());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] params(Move move) {
        return new String[0];
    }
}
