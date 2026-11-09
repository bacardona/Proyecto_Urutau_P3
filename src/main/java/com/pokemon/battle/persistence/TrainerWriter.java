package com.pokemon.battle.persistence;

import com.pokemon.battle.model.Player;

/**
 * Write access to the stored trainers.
 */
public interface TrainerWriter {

    /**
     * Stores a new human player (ENTRENADOR and JUGADOR) with its counters in zero.
     * Both rows are saved together or none is saved.
     *
     * @param name name of the new player
     * @return the player created, with the identifier given by the database
     */
    Player createPlayer(String name);
}