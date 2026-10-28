package com.pokemon.battle.persistence;

import com.pokemon.battle.model.Move;

import java.util.List;

/**
 * Write access to stored moves.
 */
public interface MoveWriter {

    /**
     * Replaces the stored moves.
     *
     * @param moves moves to store
     */
    void saveAll(List<Move> moves);
}
