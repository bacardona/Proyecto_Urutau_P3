package com.pokemon.battle.service;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;

import java.util.List;

/**
 * Decisions taken by a machine trainer instead of a person.
 */
public interface MachineStrategy {

    /**
     * @param available Pokemon the machine can use
     * @return the Pokemon the machine takes to the battle
     */
    Pokemon choosePokemon(List<Pokemon> available);

    /**
     * @param combatant combatant controlled by the machine
     * @return the move it uses this turn
     */
    Move chooseMove(Combatant combatant);
}
