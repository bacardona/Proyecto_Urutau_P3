package com.pokemon.battle.persistence;

import com.pokemon.battle.model.CombatHistoryEntry;

import java.util.List;

/**
 * Read-only access to the stored combats (COMBATE and HISTORIAL_COMBATES).
 */
public interface CombatReader {

    /**
     * @param trainerId identifier of the trainer
     * @return the combats of that trainer, newest first, or an empty list
     */
    List<CombatHistoryEntry> loadHistory(int trainerId);
}
