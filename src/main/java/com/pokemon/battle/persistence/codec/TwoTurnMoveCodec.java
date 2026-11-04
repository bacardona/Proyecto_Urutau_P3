package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.*;

/**
 * CSV codec for {@link TwoTurnMove}.
 */
public class TwoTurnMoveCodec implements MoveCsvCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    public String kind() {
        return "TWO_TURN";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Move move) {
        return move.getClass() == TwoTurnMove.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move parse(MoveCommonFields c, String[] p) {
        return new TwoTurnMove(c.id(), c.name(), c.type(), c.category(), c.power(), c.accuracy(), c.pp(), c.priority(), p[0], Boolean.parseBoolean(p[1]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] params(Move move) {
        TwoTurnMove m = (TwoTurnMove) move;
        return new String[]{m.getChargeMessage(), String.valueOf(m.isInvulnerableWhileCharging())};
    }
}
