package com.pokemon.battle.model;

/**
 * Battle stats that can be raised or lowered with stages.
 */
public enum StatType {
    ATTACK("ATK"),
    DEFENSE("DEF"),
    SPEED("SPEED"),
    SPECIAL_ATTACK("ATK SP"),
    SPECIAL_DEFENSE("DEF SP"),
    ACCURACY("PSC");

    private final String label;

    StatType(String label) {
        this.label = label;
    }

    /**
     * @return the short label shown to the player in battle messages
     */
    public String getLabel() {
        return label;
    }
}
