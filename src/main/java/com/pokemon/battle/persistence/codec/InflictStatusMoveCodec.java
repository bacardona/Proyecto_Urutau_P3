package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.*;

/**
 * CSV codec for {@link InflictStatusMove}.
 */
public class InflictStatusMoveCodec implements MoveCsvCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    public String kind() {
        return "INFLICT_STATUS";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Move move) {
        return move.getClass() == InflictStatusMove.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move parse(MoveCommonFields c, String[] p) {
        return new InflictStatusMove(c.id(), c.name(), c.type(), c.accuracy(), c.pp(), c.priority(), StatusCondition.valueOf(p[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] params(Move move) {
        return new String[]{((InflictStatusMove) move).getStatus().name()};
    }
}
