package com.pokemon.battle.exceptions;

/**
 * Thrown when an operation violates a business rule of the game.
 */
public class BusinessRuleException extends GameException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of the violated rule
     */
    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE");
    }
}
