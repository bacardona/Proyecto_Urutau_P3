package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.*;

/**
 * CSV codec for {@link DamageWithStatusMove}.
 */
public class DamageWithStatusMoveCodec implements MoveCsvCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    public String kind() {
        return "DAMAGE_STATUS";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Move move) {
        return move.getClass() == DamageWithStatusMove.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move parse(MoveCommonFields c, String[] p) {
        return new DamageWithStatusMove(c.id(), c.name(), c.type(), c.category(), c.power(), c.accuracy(), c.pp(), c.priority(), StatusCondition.valueOf(p[0]), Integer.parseInt(p[1]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] params(Move move) {
        DamageWithStatusMove m = (DamageWithStatusMove) move;
        return new String[]{m.getStatus().name(), String.valueOf(m.getEffectChance())};
    }
}
