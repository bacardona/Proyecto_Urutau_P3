package com.pokemon.battle.ui;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.exceptions.ResourceNotFoundException;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.service.RoundResult;

import java.util.List;
import java.util.Scanner;

/**
 * Console view of a battle. It only reads input and prints; every rule lives
 * in the controller and the services.
 */
public class BattleConsoleUI {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final int BAR_WIDTH = 20;

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
     * Runs a whole battle, showing a different message for each kind of error.
     *
     * @param name1 name of the first player's Pokemon
     * @param name2 name of the second player's Pokemon
     */
    public void start(String name1, String name2) {
        try {
            runBattle(name1, name2);
        } catch (ResourceNotFoundException e) {
            System.out.println("No se encontro el recurso solicitado: " + e.getMessage());
        } catch (BusinessRuleException e) {
            System.out.println("No se puede completar la operacion: " + e.getMessage());
        } catch (InvalidDataException e) {
            System.out.println("Los datos ingresados no son validos: " + e.getMessage());
        } catch (PersistenceException e) {
            System.out.println("Ocurrio un problema al acceder a los datos del sistema. Contacte al administrador.");
        }
    }

    private void runBattle(String name1, String name2) {
        controller.startBattle(name1, name2);
        Combatant c1 = controller.getFirstCombatant();
        Combatant c2 = controller.getSecondCombatant();

        while (!controller.isFinished()) {
            showBattleData(c1, c2);
            Move move1 = askMove(c1);
            clearScreen();

            showBattleData(c1, c2);
            Move move2 = askMove(c2);
            clearScreen();

            showBattleData(c1, c2);
            RoundResult result = controller.playRound(move1, move2);
            printMessages(result.getMessages());

            if (result.isBattleOver()) {
                clearScreen();
                showBattleData(c1, c2);
                System.out.println(result.getFainted().getName() + " se ha debilitado!");
                break;
            }
            System.out.print("Presione ENTER para continuar...");
            scanner.nextLine();
            clearScreen();
        }
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
