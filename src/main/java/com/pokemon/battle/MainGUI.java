package com.pokemon.battle;

import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.ui.GameWindow;
import com.pokemon.battle.ui.IntroScreen;
import com.pokemon.battle.ui.MenuScreen;
import com.pokemon.battle.ui.PveBattleFlow;
import com.pokemon.battle.ui.PvpBattleFlow;
import com.pokemon.battle.ui.TeamBuilderScreen;
import com.pokemon.battle.ui.TrainerNameScreen;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

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
        Runnable backToMenu = () -> window.showScreen("menu");

        PveBattleFlow pveFlow = new PveBattleFlow(window, controller, backToMenu);
        PvpBattleFlow pvpFlow = new PvpBattleFlow(window, controller, backToMenu);

        // Lo que se va eligiendo en los flujos
        Player[] pvePlayer = new Player[1];
        AtomicReference<Player> pvpPlayer1 = new AtomicReference<>();
        AtomicReference<Player> pvpPlayer2 = new AtomicReference<>();
        AtomicReference<List<Pokemon>> pvpTeam1 = new AtomicReference<>();

        // 1. Intro
        IntroScreen intro = new IntroScreen(PROJECT_TITLE, MEMBERS, backToMenu);

        // 2. Menú principal
        MenuScreen mainMenu = new MenuScreen(PROJECT_TITLE,
                new String[]{"COMBATE", "POKEDEX", "HISTORIAL", "SALIR"},
                new Runnable[]{
                        () -> window.showScreen("combatMenu"),
                        () -> window.showScreen("pokedex"),
                        () -> window.showScreen("history"),
                        () -> System.exit(0)});

        // 3. PvE: equipo (empieza la batalla) y nombre
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

        // 4. PvP: se crean en orden inverso (equipo 2, nombre 2, equipo 1, nombre 1)
        TeamBuilderScreen pvpTeam2Screen = new TeamBuilderScreen(
                team2 -> {
                    try {
                        pvpFlow.start(pvpPlayer1.get(), pvpTeam1.get(), pvpPlayer2.get(), team2);
                    } catch (RuntimeException e) {
                        JOptionPane.showMessageDialog(window,
                                "No se pudo iniciar el combate: " + e.getMessage());
                    }
                },
                () -> window.showScreen("pvpName2"));

        TrainerNameScreen pvpName2 = new TrainerNameScreen(controller, "ENTRENADOR 2",
                player -> {
                    try {
                        pvpPlayer2.set(player);
                        pvpTeam2Screen.prepare(player.getName(), controller.listPokemon());
                        window.showScreen("pvpTeam2");
                    } catch (RuntimeException e) {
                        JOptionPane.showMessageDialog(window,
                                "No se pudo cargar los Pokemon: " + e.getMessage());
                    }
                },
                () -> window.showScreen("pvpTeam1"));

        TeamBuilderScreen pvpTeam1Screen = new TeamBuilderScreen(
                team1 -> {
                    pvpTeam1.set(team1);
                    pvpName2.prepare(pvpPlayer1.get()); // el 2 no puede repetir al 1
                    window.showScreen("pvpName2");
                },
                () -> window.showScreen("pvpName1"));

        TrainerNameScreen pvpName1 = new TrainerNameScreen(controller, "ENTRENADOR 1",
                player -> {
                    try {
                        pvpPlayer1.set(player);
                        pvpTeam1Screen.prepare(player.getName(), controller.listPokemon());
                        window.showScreen("pvpTeam1");
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
                        () -> {
                            pvpName1.prepare(null);
                            window.showScreen("pvpName1");
                        },
                        backToMenu});

        window.addScreen("intro", intro);
        window.addScreen("menu", mainMenu);
        window.addScreen("combatMenu", combatMenu);
        window.addScreen("pveName", pveName);
        window.addScreen("pveTeam", pveTeamScreen);
        window.addScreen("pvpName1", pvpName1);
        window.addScreen("pvpTeam1", pvpTeam1Screen);
        window.addScreen("pvpName2", pvpName2);
        window.addScreen("pvpTeam2", pvpTeam2Screen);
        window.addScreen("pokedex", window.placeholder("POKEDEX (pendiente)"));
        window.addScreen("history", window.placeholder("HISTORIAL (pendiente)"));

        window.showScreen("intro");
        window.setVisible(true);
        intro.start();
    }
}