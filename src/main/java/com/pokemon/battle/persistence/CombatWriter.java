package com.pokemon.battle.persistence;

/**
 * Write access to the stored combats.
 */
public interface CombatWriter {

    /**
     * Stores a finished combat: the combat itself, one history line for each
     * trainer and the win/loss counters of the human players. Everything is
     * saved together or nothing is saved.
     *
     * @param winnerId identifier of the trainer that won
     * @param loserId  identifier of the trainer that lost
     * @return the identifier given to the new combat
     */
    int registerCombat(int winnerId, int loserId);
}
