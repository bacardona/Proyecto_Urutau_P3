package com.pokemon.battle.service;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.model.Battle;
import com.pokemon.battle.model.BattleSide;
import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Team;
import com.pokemon.battle.model.Trainer;
import com.pokemon.battle.model.TurnAction;
import com.pokemon.battle.validation.BattleValidator;
import com.pokemon.battle.validation.CombatValidator;
import com.pokemon.battle.validation.TeamValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives the events of the user interface and coordinates a 3 vs 3 battle
 * (GRASP Controller). It keeps the battle state so the UI only reads and shows.
 * Each side does one action per turn: attack or switch Pokemon.
 */
public class BattleController {

    private final PokemonCatalog pokemonCatalog;
    private final BattleEngine battleEngine;
    private final TrainerCatalog trainerCatalog;
    private final CombatRegistry combatRegistry;
    private final MachineStrategy machineStrategy;

    private Battle battle;
    // Ataques del ultimo turno, para que la pantalla los anime
    private List<MoveEvent> lastEvents = new ArrayList<>();

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
     * @throws BusinessRuleException when there are not enough Pokemon to build a team
     */
    public List<Pokemon> listPokemon() {
        List<Pokemon> all = pokemonCatalog.listAll();
        TeamValidator.validateCatalogSize(all);
        return all;
    }

    /**
     * Starts a battle between a player and a machine. The machine takes a random
     * team from the catalog.
     *
     * @param player      player that fights
     * @param playerTeam  the Pokemon chosen by the player (exactly 3)
     * @param machine     machine that fights
     * @return the battle that started
     */
    public Battle startBattleVsMachine(Player player, List<Pokemon> playerTeam, Machine machine) {
        TeamValidator.validateTeam(playerTeam);
        List<Pokemon> machineTeam = machineStrategy.chooseTeam(listPokemon(), Team.SIZE);
        TeamValidator.validateTeam(machineTeam);
        battle = new Battle(new BattleSide(player, new Team(playerTeam)),
                new BattleSide(machine, new Team(machineTeam)));
        return battle;
    }

    /**
     * Starts a battle between two players.
     *
     * @param player1 first player
     * @param team1   the Pokemon chosen by the first player (exactly 3)
     * @param player2 second player
     * @param team2   the Pokemon chosen by the second player (exactly 3)
     * @return the battle that started
     */
    public Battle startBattleVsPlayer(Player player1, List<Pokemon> team1,
                                      Player player2, List<Pokemon> team2) {
        CombatValidator.validateParticipants(player1, player2);
        TeamValidator.validateTeam(team1);
        TeamValidator.validateTeam(team2);
        battle = new Battle(new BattleSide(player1, new Team(team1)),
                new BattleSide(player2, new Team(team2)));
        return battle;
    }

    /**
     * @return the battle in progress
     */
    public Battle getBattle() {
        ensureStarted();
        return battle;
    }

    /**
     * @return true when the battle is over (or was never started)
     */
    public boolean isFinished() {
        return battle == null || battle.isOver();
    }

    /**
     * @param side side to check
     * @return the action it must repeat (Fly, Dig...), or null when it can choose
     */
    public TurnAction getForcedAction(BattleSide side) {
        Move forced = side.getTeam().getActive().getChargingMove();
        return forced == null ? null : TurnAction.attack(forced);
    }

    /**
     * Translates a menu option into an attack.
     *
     * @param side   side that chooses
     * @param option option typed by the player, starting at 1
     * @return the attack action
     * @throws InvalidDataException when the option does not exist
     */
    public TurnAction chooseAttack(BattleSide side, int option) {
        Combatant active = side.getTeam().getActive();
        BattleValidator.validateMoveChoice(option, active.getPokemon().getMoves().size());
        return TurnAction.attack(active.getPokemon().getMove(option - 1));
    }

    /**
     * Translates a team position into a switch.
     *
     * @param side         side that chooses
     * @param memberNumber position typed by the player, starting at 1
     * @return the switch action
     * @throws InvalidDataException when that Pokemon cannot come in
     */
    public TurnAction chooseSwitch(BattleSide side, int memberNumber) {
        int index = memberNumber - 1;
        if (!side.getTeam().canSwitchTo(index)) {
            throw new InvalidDataException("pokemon", "no puede entrar al combate (activo, debilitado o inexistente)");
        }
        return TurnAction.switchTo(index);
    }

    /**
     * Chooses the action of the machine: the one it must repeat, or a random move.
     *
     * @return the action of the machine for this turn
     */
    public TurnAction chooseMachineAction() {
        ensureStarted();
        BattleSide machineSide = battle.getSecond();
        TurnAction forced = getForcedAction(machineSide);
        if (forced != null) {
            return forced;
        }
        return TurnAction.attack(machineStrategy.chooseMove(machineSide.getTeam().getActive()));
    }

    /**
     * Plays a turn with the action chosen by each side. Switches happen first,
     * then the attacks are resolved by priority and speed.
     *
     * @param action1 action of the first side
     * @param action2 action of the second side
     * @return the messages of the turn
     */
    public List<String> playRound(TurnAction action1, TurnAction action2) {
        ensureStarted();
        if (battle.isOver()) {
            throw new BusinessRuleException("La batalla ya termino");
        }
        BattleSide side1 = battle.getFirst();
        BattleSide side2 = battle.getSecond();
        List<String> messages = new ArrayList<>();

        applySwitch(side1, action1, messages);
        applySwitch(side2, action2, messages);

        Combatant active1 = side1.getTeam().getActive();
        Combatant active2 = side2.getTeam().getActive();
        RoundResult fight = null;
        if (!action1.isSwitch() && !action2.isSwitch()) {
            fight = battleEngine.playRound(active1, active2, action1.getMove(), action2.getMove());
        } else if (!action1.isSwitch()) {
            fight = battleEngine.playSingleAttack(active1, active2, action1.getMove());
        } else if (!action2.isSwitch()) {
            fight = battleEngine.playSingleAttack(active2, active1, action2.getMove());
        }
        lastEvents = new ArrayList<>();
        if (fight != null) {
            // Los cambios de pokemon van antes, asi que se corre la posicion de cada ataque
            int offset = messages.size();
            for (MoveEvent event : fight.getEvents()) {
                lastEvents.add(event.shifted(offset));
            }
            messages.addAll(fight.getMessages());
        }
        return messages;
    }

    /**
     * Moves used in the last call to {@link #playRound}, in order, so the
     * screen can animate them.
     *
     * @return the moves of the last round (empty if nobody attacked)
     */
    public List<MoveEvent> getLastRoundEvents() {
        return lastEvents;
    }

    /**
     * The given side gives up and loses the battle automatically. The result is
     * stored later with {@link #registerResult()}, like any other battle.
     *
     * @param side side that surrenders
     * @throws BusinessRuleException when the battle is over or the side is a machine
     */
    public void surrender(BattleSide side) {
        ensureStarted();
        if (battle.isOver()) {
            throw new BusinessRuleException("La batalla ya termino");
        }
        if (side.isMachine()) {
            throw new BusinessRuleException("La maquina no puede rendirse");
        }
        battle.surrender(side);
    }

    /**
     * @param side side to check
     * @return true when its active Pokemon fainted and it still has others to send
     */
    public boolean needsReplacement(BattleSide side) {
        ensureStarted();
        return side.getTeam().getActive().isFainted() && side.getTeam().hasAvailable();
    }

    /**
     * Sends a new Pokemon to replace the one that fainted.
     *
     * @param side         side that replaces
     * @param memberNumber position typed by the player, starting at 1
     * @return the Pokemon that came in
     * @throws InvalidDataException when that Pokemon cannot come in
     */
    public Combatant replaceFainted(BattleSide side, int memberNumber) {
        if (!needsReplacement(side)) {
            throw new BusinessRuleException("Este lado no necesita reemplazo");
        }
        chooseSwitch(side, memberNumber);
        side.getTeam().switchTo(memberNumber - 1);
        return side.getTeam().getActive();
    }

    /**
     * Sends a random Pokemon of the machine to replace the one that fainted.
     *
     * @return the Pokemon that came in
     */
    public Combatant replaceMachineFainted() {
        ensureStarted();
        BattleSide machineSide = battle.getSecond();
        if (!needsReplacement(machineSide)) {
            throw new BusinessRuleException("La maquina no necesita reemplazo");
        }
        List<Integer> bench = machineSide.getTeam().getBenchIndexes();
        int chosen = machineStrategy.chooseReplacement(bench.size());
        machineSide.getTeam().switchTo(bench.get(chosen));
        return machineSide.getTeam().getActive();
    }

    /**
     * Stores the result of the finished battle in the database and updates the
     * win/loss counter of every human player that took part.
     *
     * @return the identifier given to the combat
     * @throws BusinessRuleException when the battle is not over
     */
    public int registerResult() {
        ensureStarted();
        if (!battle.isOver()) {
            throw new BusinessRuleException("La batalla todavia no ha terminado");
        }
        Trainer winner = battle.getWinner().getTrainer();
        Trainer loser = battle.getLoser().getTrainer();
        int combatId = combatRegistry.register(winner, loser);
        // Ya guardado en la BD: se actualizan tambien los contadores en memoria
        if (winner instanceof Player) {
            ((Player) winner).incrementBattlesWon();
        }
        if (loser instanceof Player) {
            ((Player) loser).incrementBattlesLost();
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

    private void applySwitch(BattleSide side, TurnAction action, List<String> messages) {
        if (!action.isSwitch()) {
            return;
        }
        String leaving = side.getTeam().getActive().getName();
        side.getTeam().switchTo(action.getSwitchIndex());
        messages.add(side.getTrainer().getName() + " retira a " + leaving
                + " y envia a " + side.getTeam().getActive().getName() + "!");
    }

    private void ensureStarted() {
        if (battle == null) {
            throw new BusinessRuleException("La batalla todavia no ha sido iniciada");
        }
    }
}