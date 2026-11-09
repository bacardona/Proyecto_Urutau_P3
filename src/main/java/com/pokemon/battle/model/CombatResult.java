package com.pokemon.battle.model;

/**
 * Result of a trainer in a combat, as it is stored in
 * HISTORIAL_COMBATES.RESULTADO.
 */
public enum CombatResult {
    WIN("VICTORIA"),
    LOSS("DERROTA");

    private final String dbValue;

    CombatResult(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * @return the text stored in the database column RESULTADO
     */
    public String getDbValue() {
        return dbValue;
    }
}
