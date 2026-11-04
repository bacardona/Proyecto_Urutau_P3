package com.pokemon.battle.persistence;

/**
 * Full storage of moves: reading plus writing. Clients that only read should
 * depend on {@link MoveReader}.
 */
public interface MoveRepository extends MoveReader, MoveWriter {
}
