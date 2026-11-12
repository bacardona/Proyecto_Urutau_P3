package com.pokemon.battle;

import com.pokemon.battle.model.Battle;
import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.ui.GameWindow;
import com.pokemon.battle.ui.HistoryPanel;
import com.pokemon.battle.ui.IntroScreen;
import com.pokemon.battle.ui.MenuScreen;
import com.pokemon.battle.ui.PokedexPanel;
import com.pokemon.battle.ui.PveBattleFlow;
import com.pokemon.battle.ui.PvpBattleFlow;
import com.pokemon.battle.ui.TeamSelectScreen;
import com.pokemon.battle.ui.TrainerHubScreen;
import com.pokemon.battle.ui.TrainerSelectPanel;
import com.pokemon.battle.fighting.FightingGamePanel;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Punto de entrada de la interfaz grafica. Construye las pantallas y las
 * registra en la ventana. (Main.java sigue siendo la version de consola.)
 */
public class MainGUI {

    private static final String PROJECT_TITLE = "POKKEN BATTLE";

    private static final String[] MEMBERS = {
            "Jhonatan David Galindo Gómez",
            "Brayan Andres Cardona Reyes",
            "Angel David Bermudez González",
            "Luis Javier Chávez Montes"
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainGUI::start);
    }

    private static void start() {
        GameWindow window = new GameWindow();
        AppFactory.Services services = AppFactory.create();
        BattleController controller = services.controller;
        Runnable backToMenu = () -> window.showScreen("menu");

        PveBattleFlow pveFlow = new PveBattleFlow(window, controller, backToMenu);
        PvpBattleFlow pvpFlow = new PvpBattleFlow(window, controller, backToMenu);

        // Lo que se elige en los flujos
        AtomicReference<Player> pvePlayer = new AtomicReference<>();
        AtomicReference<Player> pvpPlayer1 = new AtomicReference<>();
        AtomicReference<Player> pvpPlayer2 = new AtomicReference<>();
        AtomicReference<List<Pokemon>> pvpTeam1 = new AtomicReference<>();

        // 1. Intro
        IntroScreen intro = new IntroScreen(PROJECT_TITLE, MEMBERS, backToMenu);

        // 2. Menu principal
        MenuScreen mainMenu = new MenuScreen(PROJECT_TITLE,
                new String[]{"COMBATE", "POKEDEX", "HISTORIAL", "SALIR"},
                new Runnable[]{
                        () -> window.showScreen("combatMenu"),
                        () -> window.showScreen("pokedex"),
                        () -> window.showScreen("history"),
                        () -> System.exit(0)});

        // 3. PvE: equipo (empieza la batalla) y entrenador
        TeamSelectScreen pveTeamScreen = new TeamSelectScreen(
                team -> window.runWithLoading("PREPARANDO COMBATE",
                        () -> startVsMachine(controller, pvePlayer.get(), team),
                        pveFlow::start,
                        error -> fail(window, error, "combatMenu")),
                () -> window.showScreen("pveHub"));

        TrainerHubScreen pveHub = new TrainerHubScreen("COMBATE PVE",
                "ENTRENADOR: SELECCIONA O CREA UN ENTRENADOR", controller,
                (player, catalog) -> {
                    pvePlayer.set(player);
                    pveTeamScreen.prepare(player.getName(), catalog);
                    window.showScreen("pveTeam");
                },
                () -> window.showScreen("combatMenu"));

        // 4. PvP: se crean en orden inverso (equipo 2, entrenador 2, equipo 1, entrenador 1)
        TeamSelectScreen pvpTeamScreen2 = new TeamSelectScreen(
                team2 -> window.runWithLoading("PREPARANDO COMBATE",
                        () -> controller.startBattleVsPlayer(
                                pvpPlayer1.get(), pvpTeam1.get(), pvpPlayer2.get(), team2),
                        pvpFlow::start,
                        error -> fail(window, error, "combatMenu")),
                () -> window.showScreen("pvpHub2"));

        TrainerHubScreen pvpHub2 = new TrainerHubScreen("COMBATE PVP",
                "ENTRENADOR 2: SELECCIONA O CREA UN ENTRENADOR", controller,
                (player2, catalog) -> {
                    pvpPlayer2.set(player2);
                    pvpTeamScreen2.prepare(player2.getName(), catalog);
                    window.showScreen("pvpTeam2");
                },
                () -> window.showScreen("pvpHub1"));

        TeamSelectScreen pvpTeamScreen1 = new TeamSelectScreen(
                team1 -> {
                    pvpTeam1.set(team1);
                    openHub(window, controller, pvpHub2, "pvpHub2", pvpPlayer1.get());
                },
                () -> window.showScreen("pvpHub1"));

        TrainerHubScreen pvpHub1 = new TrainerHubScreen("COMBATE PVP",
                "ENTRENADOR 1: SELECCIONA O CREA UN ENTRENADOR", controller,
                (player1, catalog) -> {
                    pvpPlayer1.set(player1);
                    pvpTeamScreen1.prepare(player1.getName(), catalog);
                    window.showScreen("pvpTeam1");
                },
                () -> window.showScreen("combatMenu"));

        // 5. Pantalla de combate libre
        FightingGamePanel fightingPanel = new FightingGamePanel(
                () -> {
                    window.setGameSize(960, 640);
                    window.showScreen("combatMenu");
                }
        );

        // Submenu de combate
        MenuScreen combatMenu = new MenuScreen("COMBATE",
                new String[]{"PVE", "PVP", "COMBATE LIBRE", "VOLVER"},
                new Runnable[]{
                        () -> openHub(window, controller, pveHub, "pveHub", null),
                        () -> openHub(window, controller, pvpHub1, "pvpHub1", null),
                        () -> {
                            window.setGameSize(1280, 720);
                            window.showScreen("fighting");
                        },
                        backToMenu
                },
                backToMenu);

        // 6. Pokedex e historial
        PokedexPanel pokedex = new PokedexPanel(services.pokemonCatalog, backToMenu);
        HistoryPanel historyPanel = new HistoryPanel(services.combatRegistry,
                () -> window.showScreen("history"));
        TrainerSelectPanel historySelect = new TrainerSelectPanel(services.trainerCatalog,
                "ELIGE UN ENTRENADOR",
                player -> {
                    historyPanel.showPlayer(player);
                    window.showScreen("historyView");
                },
                backToMenu);

        window.addScreen("intro", intro);
        window.addScreen("menu", mainMenu);
        window.addScreen("combatMenu", combatMenu);
        window.addScreen("fighting", fightingPanel);
        window.addScreen("pveHub", pveHub);
        window.addScreen("pveTeam", pveTeamScreen);
        window.addScreen("pvpHub1", pvpHub1);
        window.addScreen("pvpTeam1", pvpTeamScreen1);
        window.addScreen("pvpHub2", pvpHub2);
        window.addScreen("pvpTeam2", pvpTeamScreen2);
        window.addScreen("pokedex", pokedex);
        window.addScreen("history", historySelect);
        window.addScreen("historyView", historyPanel);

        window.showScreen("intro");
        window.setVisible(true);
        intro.start();
    }

    // Lee entrenadores y Pokemon en segundo plano y abre la pantalla de entrenador
    private static void openHub(GameWindow window, BattleController controller,
                                TrainerHubScreen hub, String screenName, Player excluded) {
        window.runWithLoading("CARGANDO ENTRENADORES",
                () -> new HubData(controller.listPlayers(), controller.listPokemon()),
                data -> {
                    hub.prepare(data.players, data.pokemon, excluded);
                    window.showScreen(screenName);
                },
                error -> fail(window, error, "combatMenu"));
    }

    // Sortea una maquina e inicia la batalla (se ejecuta en segundo plano)
    private static Battle startVsMachine(BattleController controller, Player player, List<Pokemon> team) {
        List<Machine> machines = controller.listMachines();
        if (machines.isEmpty()) {
            throw new IllegalStateException("No hay maquinas registradas");
        }
        Machine machine = machines.get(ThreadLocalRandom.current().nextInt(machines.size()));
        return controller.startBattleVsMachine(player, team, machine);
    }

    private static void fail(GameWindow window, Throwable error, String backScreen) {
        String text = error.getMessage() == null ? error.toString() : error.getMessage();
        window.showError(text, backScreen);
    }

    private static final class HubData {
        final List<Player> players;
        final List<Pokemon> pokemon;

        HubData(List<Player> players, List<Pokemon> pokemon) {
            this.players = players;
            this.pokemon = pokemon;
        }
    }
}