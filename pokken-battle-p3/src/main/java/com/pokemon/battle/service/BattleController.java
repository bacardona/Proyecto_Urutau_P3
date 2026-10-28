package com.pokemon.battle.service;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.validation.BattleValidator;
import com.pokemon.battle.validation.PokemonValidator;

/**
 * Receives the events of the user interface and coordinates a battle
 * (GRASP Controller). It keeps the battle state so the UI only reads and shows.
 */
public class BattleController {

    private final PokemonCatalog pokemonCatalog;
    private final BattleEngine battleEngine;

    private Combatant first;
    private Combatant second;
    private boolean finished = true;

    /**
     * @param pokemonCatalog catalog of Pokemon (injected)
     * @param battleEngine   battle rules (injected)
     */
    public BattleController(PokemonCatalog pokemonCatalog, BattleEngine battleEngine) {
        this.pokemonCatalog = pokemonCatalog;
        this.battleEngine = battleEngine;
    }

    /**
     * Starts a new battle between two Pokemon of the catalog.
     *
     * @param name1 name of the first player's Pokemon
     * @param name2 name of the second player's Pokemon
     */
    public void startBattle(String name1, String name2) {
        Pokemon p1 = pokemonCatalog.findByName(name1);
        Pokemon p2 = pokemonCatalog.findByName(name2);
        PokemonValidator.validateHasMoves(p1);
        PokemonValidator.validateHasMoves(p2);
        first = new Combatant(p1);
        second = new Combatant(p2);
        finished = false;
    }

    /**
     * @return the first player's combatant
     */
    public Combatant getFirstCombatant() {
        ensureStarted();
        return first;
    }

    /**
     * @return the second player's combatant
     */
    public Combatant getSecondCombatant() {
        ensureStarted();
        return second;
    }

    /**
     * @return true when the battle is over (or was never started)
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * @param combatant combatant to check
     * @return the move it must finish (Fly, Dig...), or null when it can choose
     */
    public Move getForcedMove(Combatant combatant) {
        return combatant.getChargingMove();
    }

    /**
     * Translates a menu option into a move.
     *
     * @param combatant combatant that chooses
     * @param option    option typed by the player, starting at 1
     * @return the chosen move
     * @throws com.pokemon.battle.exceptions.InvalidDataException when the option does not exist
     */
    public Move chooseMove(Combatant combatant, int option) {
        int available = combatant.getPokemon().getMoves().size();
        BattleValidator.validateMoveChoice(option, available);
        return combatant.getPokemon().getMove(option - 1);
    }

    /**
     * Plays a round with the moves chosen by both players.
     *
     * @param move1 move of the first player
     * @param move2 move of the second player
     * @return the result of the round
     */
    public RoundResult playRound(Move move1, Move move2) {
        ensureStarted();
        RoundResult result = battleEngine.playRound(first, second, move1, move2);
        if (result.isBattleOver()) {
            finished = true;
        }
        return result;
    }

    private void ensureStarted() {
        if (first == null || second == null) {
            throw new BusinessRuleException("La batalla todavia no ha sido iniciada");
        }
    }
}
