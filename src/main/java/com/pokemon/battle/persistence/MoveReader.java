package com.pokemon.battle.persistence;

import com.pokemon.battle.model.Move;

import java.util.List;

/**
 * Read-only access to stored moves.
 */
public interface MoveReader {

    /**
     * @return every stored move, or an empty list when there is no data
     */
    List<Move> loadAll();
}
