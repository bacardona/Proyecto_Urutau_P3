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
import com.pokemon.battle.ui.TrainerHubScreen;
import com.pokemon.battle.ui.TrainerSelectPanel;

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
    // Pon aqui los nombres reales de los integrantes
    private static final String[] MEMBERS = {"Jhonatan David Galindo Gómez", "Brayan Andres Cardona Reyes", "Angel David Bermudez González", "Luis Javier Chávez Montes"};
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

        // Lo que se elige en PvP
        AtomicReference<Player> pvpPlayer1 = new AtomicReference<>();
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

        // 3. PvE: entrenador + equipo, y luego la batalla (con pantalla de carga)
        TrainerHubScreen pveHub = new TrainerHubScreen("COMBATE PVE",
                "ENTRENADOR: SELECCIONA O CREA UN ENTRENADOR", controller,
                (player, team) -> window.runWithLoading("PREPARANDO COMBATE",
                        () -> startVsMachine(controller, player, team),
                        pveFlow::start,
                        error -> fail(window, error, "combatMenu")),
                () -> window.showScreen("combatMenu"));

        // 4. PvP: entrenador 2 (se crea primero porque el 1 lo usa)
        TrainerHubScreen pvpHub2 = new TrainerHubScreen("COMBATE PVP",
                "ENTRENADOR 2: SELECCIONA O CREA UN ENTRENADOR", controller,
                (player2, team2) -> window.runWithLoading("PREPARANDO COMBATE",
                        () -> controller.startBattleVsPlayer(pvpPlayer1.get(), pvpTeam1.get(), player2, team2),
                        pvpFlow::start,
                        error -> fail(window, error, "combatMenu")),
                () -> window.showScreen("pvpHub1"));

        TrainerHubScreen pvpHub1 = new TrainerHubScreen("COMBATE PVP",
                "ENTRENADOR 1: SELECCIONA O CREA UN ENTRENADOR", controller,
                (player1, team1) -> {
                    pvpPlayer1.set(player1);
                    pvpTeam1.set(team1);
                    openHub(window, controller, pvpHub2, "pvpHub2", player1);
                },
                () -> window.showScreen("combatMenu"));

        // 5. Submenu de combate
        MenuScreen combatMenu = new MenuScreen("COMBATE",
                new String[]{"PVE", "PVP", "VOLVER"},
                new Runnable[]{
                        () -> openHub(window, controller, pveHub, "pveHub", null),
                        () -> openHub(window, controller, pvpHub1, "pvpHub1", null),
                        backToMenu},
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
        window.addScreen("pveHub", pveHub);
        window.addScreen("pvpHub1", pvpHub1);
        window.addScreen("pvpHub2", pvpHub2);
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