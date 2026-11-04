package com.pokemon.battle.validation;

import com.pokemon.battle.exceptions.InvalidDataException;

/**
 * Validation rules related to the choices made during a battle.
 */
public final class BattleValidator {

    private BattleValidator() {
    }

    /**
     * Checks that the selected menu option corresponds to an existing move.
     *
     * @param option         option typed by the player (starting at 1)
     * @param availableMoves number of moves the Pokemon knows
     * @throws InvalidDataException when the option is out of range
     */
    public static void validateMoveChoice(int option, int availableMoves) {
        if (option < 1 || option > availableMoves) {
            throw new InvalidDataException("ataque", "debe ser un numero entre 1 y " + availableMoves);
        }
    }
}
