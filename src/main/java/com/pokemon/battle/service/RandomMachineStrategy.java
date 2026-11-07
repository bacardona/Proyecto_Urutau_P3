package com.pokemon.battle.service;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.RandomProvider;

import java.util.List;

/**
 * Machine that chooses its Pokemon and its moves at random.
 */
public class RandomMachineStrategy implements MachineStrategy {

    private final RandomProvider random;

    /**
     * @param random source of randomness (injected)
     */
    public RandomMachineStrategy(RandomProvider random) {
        this.random = random;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pokemon choosePokemon(List<Pokemon> available) {
        return available.get(random.nextInt(0, available.size() - 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move chooseMove(Combatant combatant) {
        List<Move> moves = combatant.getPokemon().getMoves();
        return moves.get(random.nextInt(0, moves.size() - 1));
    }
}
