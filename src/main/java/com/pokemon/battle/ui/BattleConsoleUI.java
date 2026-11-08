package com.pokemon.battle.ui;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.exceptions.ResourceNotFoundException;
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
import com.pokemon.battle.service.BattleController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console view of the game. It only reads input and prints; every rule lives
 * in the controller and the services.
 */
public class BattleConsoleUI {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final int BAR_WIDTH = 20;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BattleController controller;
    private final Scanner scanner;

    /**
     * @param controller battle controller (injected)
     * @param scanner    shared input reader (injected)
     */
    public BattleConsoleUI(BattleController controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
    }

    /**
     * Shows the main menu until the user leaves. Each option runs on its own,
     * so an error returns to the menu with a message for that kind of error.
     */
    public void start() {
        boolean exit = false;
        while (!exit) {
            System.out.println("=== POKKEN ===");
            System.out.println("1. Jugar contra la maquina");
            System.out.println("2. Jugador contra jugador");
            System.out.println("3. Ver historial de combates");
            System.out.println("4. Salir");
            System.out.print("Opcion: ");
            try {
                switch (readOption()) {
                    case 1:
                        runBattleVsMachine();
                        break;
                    case 2:
                        runBattleVsPlayer();
                        break;
                    case 3:
                        showHistory();
                        break;
                    case 4:
                        exit = true;
                        break;
                    default:
                        throw new InvalidDataException("opcion", "debe ser un numero entre 1 y 4");
                }
            } catch (ResourceNotFoundException e) {
                System.out.println("No se encontro el recurso solicitado: " + e.getMessage());
            } catch (BusinessRuleException e) {
                System.out.println("No se puede completar la operacion: " + e.getMessage());
            } catch (InvalidDataException e) {
                System.out.println("Los datos ingresados no son validos: " + e.getMessage());
            } catch (PersistenceException e) {
                System.out.println("Ocurrio un problema al acceder a los datos del sistema. Contacte al administrador.");
            }
            System.out.println();
        }
    }

    private void runBattleVsMachine() {
        // Se valida primero que haya Pokemon suficientes, antes de pedir nada al usuario
        List<Pokemon> catalog = controller.listPokemon();
        Player player = chooseOrCreatePlayer("JUGADOR", controller.listPlayers());
        Machine machine = chooseFrom("MAQUINA", controller.listMachines());
        List<Pokemon> team = chooseTeam(player.getName(), catalog);

        playBattle(controller.startBattleVsMachine(player, team, machine));
    }

    private void runBattleVsPlayer() {
        List<Pokemon> catalog = controller.listPokemon();

        System.out.println("--- JUGADOR 1 ---");
        Player player1 = chooseOrCreatePlayer("JUGADOR 1", controller.listPlayers());

        // El segundo jugador no puede ser el mismo: si no hay otros, debe crear uno nuevo
        List<Player> others = new ArrayList<>();
        for (Player candidate : controller.listPlayers()) {
            if (candidate.getId() != player1.getId()) {
                others.add(candidate);
            }
        }
        System.out.println("--- JUGADOR 2 ---");
        Player player2 = chooseOrCreatePlayer("JUGADOR 2", others);

        List<Pokemon> team1 = chooseTeam(player1.getName(), catalog);
        System.out.print("Presione ENTER para que elija el jugador 2...");
        scanner.nextLine();
        clearScreen();
        List<Pokemon> team2 = chooseTeam(player2.getName(), catalog);

        playBattle(controller.startBattleVsPlayer(player1, team1, player2, team2));
    }

    private void playBattle(Battle battle) {
        BattleSide side1 = battle.getFirst();
        BattleSide side2 = battle.getSecond();
        System.out.println(side1.getTrainer().getName() + " vs " + side2.getTrainer().getName());
        System.out.print("Presione ENTER para comenzar...");
        scanner.nextLine();
        clearScreen();

        while (!controller.isFinished()) {
            showBattleData(battle);
            TurnAction action1 = askAction(side1);
            TurnAction action2;
            if (side2.isMachine()) {
                action2 = controller.chooseMachineAction();
            } else {
                // Cada jugador elige sin ver la jugada del otro
                clearScreen();
                showBattleData(battle);
                action2 = askAction(side2);
            }
            clearScreen();

            showBattleData(battle);
            printMessages(controller.playRound(action1, action2));
            announceFainted(side1);
            announceFainted(side2);

            if (controller.isFinished()) {
                System.out.print("Presione ENTER para ver el resultado...");
                scanner.nextLine();
                break;
            }
            replaceFainted(side1);
            replaceFainted(side2);
            System.out.print("Presione ENTER para continuar...");
            scanner.nextLine();
            clearScreen();
        }

        clearScreen();
        showBattleData(battle);
        System.out.println(battle.getWinner().getTrainer().getName() + " gana el combate!");
        int combatId = controller.registerResult();
        System.out.println("Combate #" + combatId + " guardado en el historial.");
    }

    private void announceFainted(BattleSide side) {
        Combatant active = side.getTeam().getActive();
        if (active.isFainted()) {
            System.out.println(active.getName() + " de " + side.getTrainer().getName() + " se ha debilitado!");
        }
    }

    private void replaceFainted(BattleSide side) {
        if (!controller.needsReplacement(side)) {
            return;
        }
        if (side.isMachine()) {
            Combatant next = controller.replaceMachineFainted();
            System.out.println(side.getTrainer().getName() + " envia a " + next.getName() + "!");
            return;
        }
        System.out.println(side.getTrainer().getName() + ", elija el siguiente Pokemon:");
        showTeamMenu(side.getTeam());
        while (true) {
            try {
                Combatant next = controller.replaceFainted(side, readOption());
                System.out.println(side.getTrainer().getName() + " envia a " + next.getName() + "!");
                return;
            } catch (InvalidDataException e) {
                System.out.println("Los datos ingresados no son validos: " + e.getMessage());
            }
        }
    }

    private void showHistory() {
        List<Player> players = controller.listPlayers();
        if (players.isEmpty()) {
            System.out.println("Todavia no hay jugadores registrados.");
            return;
        }
        Player player = chooseFrom("JUGADOR", players);
        List<CombatHistoryEntry> history = controller.getHistory(player);
        if (history.isEmpty()) {
            System.out.println(player.getName() + " todavia no tiene combates registrados.");
            return;
        }
        System.out.println("HISTORIAL DE " + player.getName());
        for (CombatHistoryEntry entry : history) {
            String date = entry.getDate() == null ? "sin fecha" : entry.getDate().format(DATE_FORMAT);
            String rival = entry.getRivalName() == null ? "desconocido" : entry.getRivalName();
            System.out.println("#" + entry.getCombatId() + "  " + date + "  vs " + rival
                    + "  -> " + entry.getResult());
        }
    }

    // Muestra los jugadores disponibles y, como ultima opcion, crear uno nuevo.
    // Si no hay ninguno disponible, crear uno es obligatorio.
    private Player chooseOrCreatePlayer(String title, List<Player> candidates) {
        System.out.println("Elija " + title + ":");
        if (candidates.isEmpty()) {
            System.out.println("No hay jugadores disponibles: debe crear un entrenador nuevo.");
            return createPlayer();
        }
        for (int i = 0; i < candidates.size(); i++) {
            System.out.println((i + 1) + ". " + candidates.get(i).getDescription());
        }
        int createOption = candidates.size() + 1;
        System.out.println(createOption + ". Crear un entrenador nuevo");
        System.out.print("Opcion: ");
        int option = readOption();
        if (option == createOption) {
            return createPlayer();
        }
        if (option < 1 || option > candidates.size()) {
            throw new InvalidDataException("jugador", "debe ser un numero entre 1 y " + createOption);
        }
        return candidates.get(option - 1);
    }

    private Player createPlayer() {
        System.out.print("Nombre del nuevo entrenador: ");
        Player created = controller.createPlayer(scanner.nextLine());
        System.out.println("Entrenador " + created.getName() + " creado.");
        return created;
    }

    private <T extends Trainer> T chooseFrom(String title, List<T> options) {
        System.out.println("Elija " + title + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i).getDescription());
        }
        System.out.print("Opcion: ");
        int option = readOption();
        if (option < 1 || option > options.size()) {
            throw new InvalidDataException(title.toLowerCase(), "debe ser un numero entre 1 y " + options.size());
        }
        return options.get(option - 1);
    }

    // El entrenador elige sus 3 Pokemon, uno por uno, sin repetir
    private List<Pokemon> chooseTeam(String ownerName, List<Pokemon> catalog) {
        List<Pokemon> remaining = new ArrayList<>(catalog);
        List<Pokemon> team = new ArrayList<>();
        System.out.println(ownerName + ", arme su equipo de " + Team.SIZE + " POKKEN:");
        for (int slot = 1; slot <= Team.SIZE; slot++) {
            System.out.println("Elija el POKKEN " + slot + " de " + Team.SIZE + ":");
            for (int i = 0; i < remaining.size(); i++) {
                Pokemon p = remaining.get(i);
                System.out.println((i + 1) + ". " + p.getName() + " (" + p.getPrimaryType() + ")");
            }
            while (true) {
                System.out.print("Opcion: ");
                try {
                    int option = readOption();
                    if (option < 1 || option > remaining.size()) {
                        throw new InvalidDataException("pokken", "debe ser un numero entre 1 y " + remaining.size());
                    }
                    team.add(remaining.remove(option - 1));
                    break;
                } catch (InvalidDataException e) {
                    System.out.println("Los datos ingresados no son validos: " + e.getMessage());
                }
            }
        }
        return team;
    }

    // Cada lado hace una sola accion por turno: atacar o cambiar de Pokemon
    private TurnAction askAction(BattleSide side) {
        Team team = side.getTeam();
        Combatant active = team.getActive();
        TurnAction forced = controller.getForcedAction(side);
        if (forced != null) {
            System.out.println(active.getName() + " continua con " + forced.getMove().getName() + ".");
            System.out.print("Presione ENTER para continuar...");
            scanner.nextLine();
            return forced;
        }
        while (true) {
            System.out.println("TURNO \t" + side.getTrainer().getName() + " - " + active.getName()
                    + " [" + active.getHitPoints().getCurrent() + " PS]\nATAQUES:\n");
            showMoveMenu(active);
            int moves = active.getPokemon().getMoves().size();
            boolean canSwitch = team.hasBench();
            if (canSwitch) {
                System.out.println((moves + 1) + ". Cambiar de Pokemon\n");
            }
            System.out.print("Opcion: ");
            try {
                int option = readOption();
                if (canSwitch && option == moves + 1) {
                    TurnAction switchAction = askSwitch(side);
                    if (switchAction != null) {
                        return switchAction;
                    }
                    continue;
                }
                return controller.chooseAttack(side, option);
            } catch (InvalidDataException e) {
                System.out.println("Los datos ingresados no son validos: " + e.getMessage());
            }
        }
    }

    // Devuelve null si el jugador prefiere volver al menu de ataques
    private TurnAction askSwitch(BattleSide side) {
        System.out.println("Elija el Pokemon que entra (0 para volver):");
        showTeamMenu(side.getTeam());
        while (true) {
            try {
                int option = readOption();
                if (option == 0) {
                    return null;
                }
                return controller.chooseSwitch(side, option);
            } catch (InvalidDataException e) {
                System.out.println("Los datos ingresados no son validos: " + e.getMessage());
            }
        }
    }

    private void showTeamMenu(Team team) {
        List<Combatant> members = team.getMembers();
        for (int i = 0; i < members.size(); i++) {
            Combatant member = members.get(i);
            String state;
            if (member.isFainted()) {
                state = "debilitado";
            } else if (i == team.getActiveIndex()) {
                state = "en combate";
            } else {
                state = member.getHitPoints().getCurrent() + "/" + member.getHitPoints().getMax() + " PS";
            }
            System.out.println((i + 1) + ". " + member.getName() + " (" + state + ")");
        }
    }

    private int readOption() {
        String text = scanner.nextLine().trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidDataException("opcion", "debe ser un numero");
        }
    }

    private void showMoveMenu(Combatant combatant) {
        List<Move> moves = combatant.getPokemon().getMoves();
        for (int i = 0; i < moves.size(); i++) {
            System.out.println((i + 1) + ". " + moves.get(i).getDescription());
        }
        System.out.println();
    }

    private void printMessages(List<String> messages) {
        for (String message : messages) {
            System.out.println(message);
        }
    }

    private void showBattleData(Battle battle) {
        showSide(battle.getFirst());
        System.out.println("\n--------------------------------");
        showSide(battle.getSecond());
        System.out.println("\n--------------------------------");
    }

    private void showSide(BattleSide side) {
        System.out.println(side.getTrainer().getName());
        showCombatantRow(side.getTeam().getActive());
        System.out.println("Equipo: " + teamSummary(side.getTeam()));
    }

    private String teamSummary(Team team) {
        StringBuilder summary = new StringBuilder();
        List<Combatant> members = team.getMembers();
        for (int i = 0; i < members.size(); i++) {
            Combatant member = members.get(i);
            if (i > 0) {
                summary.append(" | ");
            }
            summary.append(i == team.getActiveIndex() ? "*" : "").append(member.getName());
            summary.append(member.isFainted() ? " (debilitado)" : " " + member.getHitPoints().getCurrent()
                    + "/" + member.getHitPoints().getMax());
        }
        return summary.toString();
    }

    private void showCombatantRow(Combatant combatant) {
        System.out.printf("%-20s%9s%n", combatant.getName(), "Nv" + combatant.getPokemon().getLevel());
        System.out.print(combatant.getStatus().getAbbreviation() + "\tPS");
        showHealthBar(combatant.getHitPoints().getCurrent(), combatant.getHitPoints().getMax());
    }

    private void showHealthBar(int current, int max) {
        double percentage = max <= 0 ? 0 : Math.max(0.0, (double) current / max);
        int filled = current <= 0 ? 0 : Math.min(BAR_WIDTH, (int) Math.ceil(percentage * BAR_WIDTH));

        String color;
        if (percentage > 0.5) {
            color = GREEN;
        } else if (percentage > 0.2) {
            color = YELLOW;
        } else {
            color = RED;
        }

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < BAR_WIDTH; i++) {
            bar.append(i < filled ? '|' : ' ');
        }
        bar.append(']');
        System.out.println(color + bar + RESET);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
        System.out.flush();
    }
}