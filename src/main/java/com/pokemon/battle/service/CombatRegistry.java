package com.pokemon.battle.service;

import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.Trainer;

import java.util.List;

/**
 * What the rest of the system needs to keep the record of the combats.
 */
public interface CombatRegistry {

    /**
     * Stores a finished combat.
     *
     * @param winner trainer that won
     * @param loser  trainer that lost
     * @return the identifier given to the combat
     */
    int register(Trainer winner, Trainer loser);

    /**
     * @param trainer trainer whose combats are wanted
     * @return its combats, newest first
     */
    List<CombatHistoryEntry> historyOf(Trainer trainer);
}
