package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.*;

/**
 * CSV codec for {@link DamageWithFlinchMove}.
 */
public class DamageWithFlinchMoveCodec implements MoveCsvCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    public String kind() {
        return "DAMAGE_FLINCH";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Move move) {
        return move.getClass() == DamageWithFlinchMove.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move parse(MoveCommonFields c, String[] p) {
        return new DamageWithFlinchMove(c.id(), c.name(), c.type(), c.category(), c.power(), c.accuracy(), c.pp(), c.priority(), Integer.parseInt(p[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] params(Move move) {
        return new String[]{String.valueOf(((DamageWithFlinchMove) move).getFlinchChance())};
    }
}
