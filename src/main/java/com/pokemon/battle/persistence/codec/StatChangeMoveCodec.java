package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.*;

/**
 * CSV codec for {@link StatChangeMove}.
 */
public class StatChangeMoveCodec implements MoveCsvCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    public String kind() {
        return "STAT_CHANGE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Move move) {
        return move.getClass() == StatChangeMove.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move parse(MoveCommonFields c, String[] p) {
        return new StatChangeMove(c.id(), c.name(), c.type(), c.accuracy(), c.pp(), c.priority(), StatType.valueOf(p[0]), Integer.parseInt(p[1]), p[2].equals("SELF"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] params(Move move) {
        StatChangeMove m = (StatChangeMove) move;
        return new String[]{m.getStat().name(), String.valueOf(m.getStages()), m.targetsSelf() ? "SELF" : "OPPONENT"};
    }
}
