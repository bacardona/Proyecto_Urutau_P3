package com.pokemon.battle.service;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.validation.BattleValidator;
import com.pokemon.battle.validation.PokemonValidator;

import java.util.List;

/**
 * Receives the events of the user interface and coordinates a battle
 * (GRASP Controller). It keeps the battle state so the UI only reads and shows.
 */
public class BattleController {

    private final PokemonCatalog pokemonCatalog;
    private final BattleEngine battleEngine;
    private final TrainerCatalog trainerCatalog;
    private final CombatRegistry combatRegistry;
    private final MachineStrategy machineStrategy;

    private Player player;
    private Machine machine;
    private Combatant first;
    private Combatant second;
    private boolean finished = true;

    /**
     * @param pokemonCatalog  catalog of Pokemon (injected)
     * @param battleEngine    battle rules (injected)
     * @param trainerCatalog  catalog of players and machines (injected)
     * @param combatRegistry  record of the finished combats (injected)
     * @param machineStrategy decisions of the machine trainer (injected)
     */
    public BattleController(PokemonCatalog pokemonCatalog, BattleEngine battleEngine,
                            TrainerCatalog trainerCatalog, CombatRegistry combatRegistry,
                            MachineStrategy machineStrategy) {
        this.pokemonCatalog = pokemonCatalog;
        this.battleEngine = battleEngine;
        this.trainerCatalog = trainerCatalog;
        this.combatRegistry = combatRegistry;
        this.machineStrategy = machineStrategy;
    }

    /**
     * @return the players stored in the database
     */
    public List<Player> listPlayers() {
        return trainerCatalog.listPlayers();
    }

    /**
     * @return the machines stored in the database
     */
    public List<Machine> listMachines() {
        return trainerCatalog.listMachines();
    }

        /**
     * Creates and stores a new human player.
     *
     * @param name name of the new player
     * @return the player created
     */
    public Player createPlayer(String name) {
        return trainerCatalog.createPlayer(name);
    }
    
    /**
     * @return the Pokemon stored in the database
     */
    public List<Pokemon> listPokemon() {
        return pokemonCatalog.listAll();
    }

    /**
     * Starts a new battle between a player and a machine. The machine takes a
     * random Pokemon of the catalog.
     *
     * @param player         player that fights
     * @param playerPokemon  Pokemon chosen by the player
     * @param machine        machine that fights
     */
    public void startBattle(Player player, Pokemon playerPokemon, Machine machine) {
        List<Pokemon> all = pokemonCatalog.listAll();
        if (all.isEmpty()) {
            throw new BusinessRuleException("No hay Pokemon cargados en la base de datos");
        }
        Pokemon machinePokemon = machineStrategy.choosePokemon(all);
        PokemonValidator.validateHasMoves(playerPokemon);
        PokemonValidator.validateHasMoves(machinePokemon);
        this.player = player;
        this.machine = machine;
        first = new Combatant(playerPokemon);
        second = new Combatant(machinePokemon);
        finished = false;
    }

    /**
     * @return the player's combatant
     */
    public Combatant getFirstCombatant() {
        ensureStarted();
        return first;
    }

    /**
     * @return the machine's combatant
     */
    public Combatant getSecondCombatant() {
        ensureStarted();
        return second;
    }

    /**
     * @return the player of the current battle
     */
    public Player getPlayer() {
        ensureStarted();
        return player;
    }

    /**
     * @return the machine of the current battle
     */
    public Machine getMachine() {
        ensureStarted();
        return machine;
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
     * Chooses the move of the machine: the one it must finish, or a new one.
     *
     * @return the move of the machine for this turn
     */
    public Move chooseMachineMove() {
        ensureStarted();
        Move forced = getForcedMove(second);
        return forced != null ? forced : machineStrategy.chooseMove(second);
    }

    /**
     * Plays a round with the moves chosen by both sides.
     *
     * @param move1 move of the player
     * @param move2 move of the machine
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

    /**
     * Stores the result of a finished battle in the database.
     *
     * @param result result of the last round, with the combatant that fainted
     * @return the identifier given to the combat
     * @throws BusinessRuleException when the battle is not over
     */
    public int registerResult(RoundResult result) {
        ensureStarted();
        if (!result.isBattleOver()) {
            throw new BusinessRuleException("La batalla todavia no ha terminado");
        }
        boolean playerLost = result.getFainted() == first;
        int combatId = playerLost
                ? combatRegistry.register(machine, player)
                : combatRegistry.register(player, machine);
        // Ya guardado en la BD: se actualiza tambien el contador del jugador en memoria
        if (playerLost) {
            player.incrementBattlesLost();
        } else {
            player.incrementBattlesWon();
        }
        return combatId;
    }

    /**
     * @param trainerPlayer player whose combats are wanted
     * @return its combats, newest first
     */
    public List<CombatHistoryEntry> getHistory(Player trainerPlayer) {
        return combatRegistry.historyOf(trainerPlayer);
    }

    private void ensureStarted() {
        if (first == null || second == null) {
            throw new BusinessRuleException("La batalla todavia no ha sido iniciada");
        }
    }
}
