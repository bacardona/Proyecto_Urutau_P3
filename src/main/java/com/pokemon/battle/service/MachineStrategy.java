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
     * @param size      number of Pokemon the team needs
     * @return the Pokemon the machine takes to the battle, without repeating any
     */
    List<Pokemon> chooseTeam(List<Pokemon> available, int size);

    /**
     * @param combatant combatant controlled by the machine
     * @return the move it uses this turn
     */
    Move chooseMove(Combatant combatant);

    /**
     * @param options how many Pokemon of its team can come in
     * @return the position (starting at 0) of the one it sends, between 0 and options - 1
     */
    int chooseReplacement(int options);
}