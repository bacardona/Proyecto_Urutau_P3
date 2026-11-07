package com.pokemon.battle.model;

import java.time.LocalDateTime;

/**
 * One line of the history of a trainer: a combat, its date, the rival
 * and the result (tables COMBATE and HISTORIAL_COMBATES).
 */
public class CombatHistoryEntry {

    private final int combatId;
    private final LocalDateTime date;
    private final String rivalName;
    private final String result;

    /**
     * @param combatId  combat identifier
     * @param date      date of the combat
     * @param rivalName name of the other trainer, or null when it is unknown
     * @param result    result stored for the trainer
     */
    public CombatHistoryEntry(int combatId, LocalDateTime date, String rivalName, String result) {
        this.combatId = combatId;
        this.date = date;
        this.rivalName = rivalName;
        this.result = result;
    }

    /**
     * @return the combat identifier
     */
    public int getCombatId() {
        return combatId;
    }

    /**
     * @return the date of the combat
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * @return the rival name, or null when it is unknown
     */
    public String getRivalName() {
        return rivalName;
    }

    /**
     * @return the result of the trainer in that combat
     */
    public String getResult() {
        return result;
    }
}
