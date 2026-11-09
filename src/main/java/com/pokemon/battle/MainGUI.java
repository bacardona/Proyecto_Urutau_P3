package com.pokemon.battle;

import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.ui.GameWindow;
import com.pokemon.battle.ui.IntroScreen;
import com.pokemon.battle.ui.MenuScreen;
import com.pokemon.battle.ui.PveBattleFlow;
import com.pokemon.battle.ui.TeamBuilderScreen;
import com.pokemon.battle.ui.TrainerNameScreen;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Punto de entrada de la interfaz gráfica. Construye las pantallas y las
 * registra en la ventana. (Main.java sigue siendo la versión de consola.)
 */
public class MainGUI {

    private static final String PROJECT_TITLE = "POKKEN BATTLE";
    // Pon aquí los nombres reales de los integrantes
    private static final String[] MEMBERS = {"Integrante 1", "Integrante 2", "Integrante 3"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainGUI::start);
    }

    private static void start() {
        GameWindow window = new GameWindow();
        BattleController controller = AppFactory.createController();

        // El entrenador elegido en el flujo PvE
        Player[] pvePlayer = new Player[1];
        PveBattleFlow pveFlow = new PveBattleFlow(window, controller,
                () -> window.showScreen("menu"));

        // 1. Intro
        IntroScreen intro = new IntroScreen(PROJECT_TITLE, MEMBERS,
                () -> window.showScreen("menu"));

        // 2. Menú principal
        MenuScreen mainMenu = new MenuScreen(PROJECT_TITLE,
                new String[]{"COMBATE", "POKEDEX", "HISTORIAL", "SALIR"},
                new Runnable[]{
                        () -> window.showScreen("combatMenu"),
                        () -> window.showScreen("pokedex"),
                        () -> window.showScreen("history"),
                        () -> System.exit(0)});

        // 3. PvE, paso 2: armar equipo y empezar la batalla
        TeamBuilderScreen pveTeamScreen = new TeamBuilderScreen(
                team -> {
                    try {
                        List<Machine> machines = controller.listMachines();
                        if (machines.isEmpty()) {
                            JOptionPane.showMessageDialog(window, "No hay maquinas registradas.");
                            return;
                        }
                        Machine machine = machines.get(
                                ThreadLocalRandom.current().nextInt(machines.size()));
                        pveFlow.start(pvePlayer[0], team, machine);
                    } catch (RuntimeException e) {
                        JOptionPane.showMessageDialog(window,
                                "No se pudo iniciar el combate: " + e.getMessage());
                    }
                },
                () -> window.showScreen("pveName"));

        // 4. PvE, paso 1: nombre del entrenador
        TrainerNameScreen pveName = new TrainerNameScreen(controller, "ENTRENADOR",
                player -> {
                    try {
                        pvePlayer[0] = player;
                        pveTeamScreen.prepare(player.getName(), controller.listPokemon());
                        window.showScreen("pveTeam");
                    } catch (RuntimeException e) {
                        JOptionPane.showMessageDialog(window,
                                "No se pudo cargar los Pokemon: " + e.getMessage());
                    }
                },
                () -> window.showScreen("combatMenu"));

        // 5. Submenú de combate
        MenuScreen combatMenu = new MenuScreen("COMBATE",
                new String[]{"PVE", "PVP", "VOLVER"},
                new Runnable[]{
                        () -> {
                            pveName.prepare(null);
                            window.showScreen("pveName");
                        },
                        () -> window.showScreen("pvp"),
                        () -> window.showScreen("menu")});

        window.addScreen("intro", intro);
        window.addScreen("menu", mainMenu);
        window.addScreen("combatMenu", combatMenu);
        window.addScreen("pveName", pveName);
        window.addScreen("pveTeam", pveTeamScreen);
        window.addScreen("pvp", window.placeholder("PVP (pendiente)"));
        window.addScreen("pokedex", window.placeholder("POKEDEX (pendiente)"));
        window.addScreen("history", window.placeholder("HISTORIAL (pendiente)"));

        window.showScreen("intro");
        window.setVisible(true);
        intro.start();
    }
}