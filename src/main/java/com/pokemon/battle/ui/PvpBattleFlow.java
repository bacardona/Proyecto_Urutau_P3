package com.pokemon.battle.ui;

import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.model.Battle;
import com.pokemon.battle.model.BattleSide;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Team;
import com.pokemon.battle.model.TurnAction;
import com.pokemon.battle.service.BattleController;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dirige una batalla 3 contra 3 entre dos jugadores en el mismo teclado.
 * Cada turno pide la accion de cada lado por separado (con una pantalla de
 * relevo para que no vean la jugada del otro) y luego juega la ronda.
 */
public class PvpBattleFlow {

    private static final String SCREEN = "pvpBattle";
    private static final int PAUSE_MS = 3000;

    private final GameWindow window;
    private final BattleController controller;
    private final Runnable onExit;

    private BattleScreenForm form;
    private BattleSide side1;
    private BattleSide side2;

    public PvpBattleFlow(GameWindow window, BattleController controller, Runnable onExit) {
        this.window = window;
        this.controller = controller;
        this.onExit = onExit;
    }

    public void start(Player player1, List<Pokemon> team1, Player player2, List<Pokemon> team2) {
        Battle battle = controller.startBattleVsPlayer(player1, team1, player2, team2);
        side1 = battle.getFirst();
        side2 = battle.getSecond();

        form = new BattleScreenForm(active(side1), active(side2));
        window.addScreen(SCREEN, form.getRootPanel());
        window.showScreen(SCREEN);

        form.setInputEnabled(false);
        form.showMessages(List.of(side1.getTrainer().getName() + " vs " + side2.getTrainer().getName()));
        pause(this::beginTurn);
    }

    // ---------- Turno: primero el lado 1, luego el lado 2, luego se juega ----------

    private void beginTurn() {
        if (controller.isFinished()) {
            finish();
            return;
        }
        askAction(side1, action1 -> askAction(side2, action2 -> resolve(action1, action2)));
    }

    private void askAction(BattleSide side, Consumer<TurnAction> done) {
        setPerspective(side);
        TurnAction forced = controller.getForcedAction(side);
        if (forced != null) {
            notice(active(side).getName() + " continua con " + forced.getMove().getName() + ".",
                    () -> done.accept(forced));
            return;
        }
        handoff(side, () -> showMainMenu(side, done));
    }

    // Pantalla de relevo: el otro jugador no debe mirar
    private void handoff(BattleSide side, Runnable next) {
        form.showChoices("Turno de " + side.getTrainer().getName() + " (el otro no mira)",
                new String[]{"LISTO"},
                index -> guarded(next),
                () -> {
                });
    }

    private void showMainMenu(BattleSide side, Consumer<TurnAction> done) {
        List<String> labels = new ArrayList<>();
        labels.add("ATACAR");
        if (side.getTeam().hasBench()) {
            labels.add("CAMBIAR");
        }
        labels.add("RENDIRSE");

        form.showChoices("¿Que hara " + active(side).getName() + "?",
                labels.toArray(new String[0]),
                index -> guarded(() -> {
                    String label = labels.get(index);
                    if (label.equals("ATACAR")) {
                        showMoves(side, done);
                    } else if (label.equals("CAMBIAR")) {
                        showSwitchMenu(side, done);
                    } else {
                        confirmSurrender(side, done);
                    }
                }),
                () -> {
                });
    }

    private void showMoves(BattleSide side, Consumer<TurnAction> done) {
        form.showMoves(
                index -> guarded(() -> done.accept(controller.chooseAttack(side, index + 1))),
                () -> showMainMenu(side, done));
    }

    private void showSwitchMenu(BattleSide side, Consumer<TurnAction> done) {
        String[] labels = teamLabels(side, true);
        int backIndex = labels.length - 1;
        form.showChoices("Elige pokemon", labels,
                index -> guarded(() -> {
                    if (index == backIndex) {
                        showMainMenu(side, done);
                        return;
                    }
                    TurnAction action;
                    try {
                        action = controller.chooseSwitch(side, index + 1);
                    } catch (InvalidDataException e) {
                        notice("Ese pokemon no puede entrar", () -> showSwitchMenu(side, done));
                        return;
                    }
                    done.accept(action);
                }),
                () -> showMainMenu(side, done));
    }

    private void confirmSurrender(BattleSide side, Consumer<TurnAction> done) {
        form.showChoices("¿Seguro que te rindes?", new String[]{"SI, RENDIRSE", "NO"},
                index -> guarded(() -> {
                    if (index == 0) {
                        controller.surrender(side);
                        finish();
                    } else {
                        showMainMenu(side, done);
                    }
                }),
                () -> showMainMenu(side, done));
    }

    // ---------- Resolver el turno ----------

    private void resolve(TurnAction action1, TurnAction action2) {
        form.setInputEnabled(false);
        setPerspective(side1);
        List<String> messages = new ArrayList<>(controller.playRound(action1, action2));
        addFainted(messages, side1);
        addFainted(messages, side2);

        setPerspective(side1);
        form.refresh();
        form.showMessages(messages);
        pause(this::afterRound);
    }

    private void afterRound() {
        if (controller.isFinished()) {
            finish();
            return;
        }
        replaceIfNeeded(side1, () -> replaceIfNeeded(side2, this::beginTurn));
    }

    private void replaceIfNeeded(BattleSide side, Runnable after) {
        if (!controller.needsReplacement(side)) {
            after.run();
            return;
        }
        setPerspective(side);
        handoff(side, () -> askReplacement(side, after));
    }

    // El jugador debe elegir quien entra (no hay opcion de volver)
    private void askReplacement(BattleSide side, Runnable after) {
        form.showChoices("Elige el siguiente pokemon", teamLabels(side, false),
                index -> guarded(() -> {
                    Combatant next;
                    try {
                        next = controller.replaceFainted(side, index + 1);
                    } catch (InvalidDataException e) {
                        notice("Ese pokemon no puede entrar", () -> askReplacement(side, after));
                        return;
                    }
                    setPerspective(side);
                    notice(side.getTrainer().getName() + " envia a " + next.getName() + "!", after);
                }),
                () -> {
                });
    }

    // ---------- Final ----------

    private void finish() {
        Battle battle = controller.getBattle();
        List<String> messages = new ArrayList<>();
        if (battle.getSurrendered() != null) {
            messages.add(battle.getSurrendered().getTrainer().getName() + " se ha rendido.");
        }
        messages.add(battle.getWinner().getTrainer().getName() + " gana el combate!");
        try {
            int combatId = controller.registerResult();
            messages.add("Combate #" + combatId + " guardado en el historial.");
        } catch (RuntimeException e) {
            messages.add("No se pudo guardar el combate.");
        }

        setPerspective(side1);
        form.refresh();
        form.setInputEnabled(false);
        form.showMessages(messages);
        pause(() -> form.showChoices("Fin del combate", new String[]{"VOLVER AL MENU"},
                index -> onExit.run(), onExit));
    }

    // ---------- Ayudas ----------

    private Combatant active(BattleSide side) {
        return side.getTeam().getActive();
    }

    // Voltea el campo para que el jugador que decide vea su pokemon abajo
    private void setPerspective(BattleSide side) {
        BattleSide other = side == side1 ? side2 : side1;
        form.setCombatants(active(side), active(other));
    }

    private void addFainted(List<String> messages, BattleSide side) {
        Combatant active = active(side);
        if (active.isFainted()) {
            messages.add(active.getName() + " de " + side.getTrainer().getName() + " se ha debilitado!");
        }
    }

    // Una etiqueta por miembro del equipo y, si se pide, "VOLVER" al final
    private String[] teamLabels(BattleSide side, boolean withBack) {
        Team team = side.getTeam();
        List<Combatant> members = team.getMembers();
        String[] labels = new String[members.size() + (withBack ? 1 : 0)];
        for (int i = 0; i < members.size(); i++) {
            Combatant m = members.get(i);
            String state;
            if (m.isFainted()) {
                state = "DEBIL";
            } else if (i == team.getActiveIndex()) {
                state = "ACTIVO";
            } else {
                state = m.getHitPoints().getCurrent() + "/" + m.getHitPoints().getMax();
            }
            labels[i] = m.getName().toUpperCase() + " " + state;
        }
        if (withBack) {
            labels[members.size()] = "VOLVER";
        }
        return labels;
    }

    // Muestra un mensaje un momento y sigue con lo siguiente
    private void notice(String message, Runnable next) {
        form.setInputEnabled(false);
        form.showMessages(List.of(message));
        pause(next);
    }

    private void pause(Runnable next) {
        Timer timer = new Timer(PAUSE_MS, e -> guarded(next));
        timer.setRepeats(false);
        timer.start();
    }

    // Si algo falla, no se cuelga la pantalla: avisa y deja volver al menu
    private void guarded(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            form.showChoices("Error: " + e.getMessage(), new String[]{"VOLVER AL MENU"},
                    index -> onExit.run(), onExit);
        }
    }
}