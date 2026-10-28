package com.pokemon.battle.model;

/**
 * Collaborators a move needs while it is executed.
 */
public class BattleContext {

    private final RandomProvider random;
    private final TypeEffectiveness effectiveness;
    private final BattleLog battleLog;

    /**
     * @param random        source of randomness
     * @param effectiveness type effectiveness rules
     * @param battleLog     log where messages are written
     */
    public BattleContext(RandomProvider random, TypeEffectiveness effectiveness, BattleLog battleLog) {
        this.random = random;
        this.effectiveness = effectiveness;
        this.battleLog = battleLog;
    }

    /**
     * @return the source of randomness
     */
    public RandomProvider getRandom() {
        return random;
    }

    /**
     * @return the type effectiveness rules
     */
    public TypeEffectiveness getEffectiveness() {
        return effectiveness;
    }

    /**
     * Writes a message in the battle log.
     *
     * @param message text to add
     */
    public void log(String message) {
        battleLog.log(message);
    }
}
