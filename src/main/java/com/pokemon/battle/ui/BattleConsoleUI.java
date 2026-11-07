package com.pokemon.battle.ui;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.exceptions.ResourceNotFoundException;
import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Trainer;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.service.RoundResult;

import java.time.format.DateTimeFormatter;
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
            System.out.println("1. Jugar contra una maquina");
            System.out.println("2. Ver historial de combates");
            System.out.println("3. Salir");
            System.out.print("Opcion: ");
            try {
                switch (readOption()) {
                    case 1:
                        runBattle();
                        break;
                    case 2:
                        showHistory();
                        break;
                    case 3:
                        exit = true;
                        break;
                    default:
                        throw new InvalidDataException("opcion", "debe ser un numero entre 1 y 3");
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

    private void runBattle() {
        Player player = chooseOrCreatePlayer();
        Machine machine = chooseFrom("MAQUINA", controller.listMachines());
        Pokemon pokemon = choosePokemon(controller.listPokemon());

        controller.startBattle(player, pokemon, machine);
        Combatant c1 = controller.getFirstCombatant();
        Combatant c2 = controller.getSecondCombatant();
        System.out.println(player.getName() + " (" + c1.getName() + ") vs "
                + machine.getName() + " (" + c2.getName() + ")");
        System.out.print("Presione ENTER para comenzar...");
        scanner.nextLine();
        clearScreen();

        while (!controller.isFinished()) {
            showBattleData(c1, c2);
            Move move1 = askMove(c1);
            Move move2 = controller.chooseMachineMove();
            clearScreen();

            showBattleData(c1, c2);
            RoundResult result = controller.playRound(move1, move2);
            printMessages(result.getMessages());

            if (result.isBattleOver()) {
                clearScreen();
                showBattleData(c1, c2);
                System.out.println(result.getFainted().getName() + " se ha debilitado!");
                int combatId = controller.registerResult(result);
                System.out.println("Combate #" + combatId + " guardado en el historial.");
                break;
            }
            System.out.print("Presione ENTER para continuar...");
            scanner.nextLine();
            clearScreen();
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

    // Muestra los jugadores de la BD y, como ultima opcion, crear uno nuevo
    private Player chooseOrCreatePlayer() {
        List<Player> players = controller.listPlayers();
        System.out.println("Elija JUGADOR:");
        for (int i = 0; i < players.size(); i++) {
            System.out.println((i + 1) + ". " + players.get(i).getDescription());
        }
        int createOption = players.size() + 1;
        System.out.println(createOption + ". Crear un entrenador nuevo");
        System.out.print("Opcion: ");
        int option = readOption();
        if (option == createOption) {
            System.out.print("Nombre del nuevo entrenador: ");
            Player created = controller.createPlayer(scanner.nextLine());
            System.out.println("Entrenador " + created.getName() + " creado.");
            return created;
        }
        if (option < 1 || option > players.size()) {
            throw new InvalidDataException("jugador", "debe ser un numero entre 1 y " + createOption);
        }
        return players.get(option - 1);
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

    private Pokemon choosePokemon(List<Pokemon> options) {
        System.out.println("Elija su POKKEN:");
        for (int i = 0; i < options.size(); i++) {
            Pokemon p = options.get(i);
            System.out.println((i + 1) + ". " + p.getName() + " (" + p.getPrimaryType() + ")");
        }
        System.out.print("Opcion: ");
        int option = readOption();
        if (option < 1 || option > options.size()) {
            throw new InvalidDataException("pokken", "debe ser un numero entre 1 y " + options.size());
        }
        return options.get(option - 1);
    }

    // Si el Pokemon esta cargando Vuelo/Excavar no elige: debe completar ese ataque
    private Move askMove(Combatant combatant) {
        Move forced = controller.getForcedMove(combatant);
        if (forced != null) {
            System.out.println(combatant.getName() + " continua con " + forced.getName() + ".");
            System.out.print("Presione ENTER para continuar...");
            scanner.nextLine();
            return forced;
        }
        System.out.println("TURNO \t" + combatant.getName() + " [" + combatant.getHitPoints().getCurrent()
                + " PS]\nATAQUES:\n");
        showMoveMenu(combatant);
        while (true) {
            try {
                return controller.chooseMove(combatant, readOption());
            } catch (InvalidDataException e) {
                System.out.println("Los datos ingresados no son validos: " + e.getMessage());
            }
        }
    }

    private int readOption() {
        String text = scanner.nextLine().trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidDataException("ataque", "debe ser un numero");
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

    private void showBattleData(Combatant c1, Combatant c2) {
        showCombatantRow(c1);
        System.out.println("\n--------------------------------");
        showCombatantRow(c2);
        System.out.println("\n--------------------------------");
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
