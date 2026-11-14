package com.pokemon.battle.service;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.RandomProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Machine that chooses its team, its moves and its replacements at random.
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
    public List<Pokemon> chooseTeam(List<Pokemon> available, int size) {
        List<Pokemon> pool = new ArrayList<>(available);
        List<Pokemon> team = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            team.add(pool.remove(random.nextInt(0, pool.size() - 1)));
        }
        return team;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Move chooseMove(Combatant combatant) {
        List<Move> moves = combatant.getPokemon().getMoves();
        return moves.get(random.nextInt(0, moves.size() - 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int chooseReplacement(int options) {
        return random.nextInt(0, options - 1);
    }
}