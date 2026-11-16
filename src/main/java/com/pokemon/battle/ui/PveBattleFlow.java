package com.pokemon.battle.ui;

import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.model.Battle;
import com.pokemon.battle.model.BattleSide;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Team;
import com.pokemon.battle.model.TurnAction;
import com.pokemon.battle.service.BattleController;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

/**
 * Dirige una batalla 3 contra 3 contra la maquina: pide las acciones en la
 * pantalla, las juega con el BattleController y muestra el resultado.
 */
public class PveBattleFlow {

    private static final String SCREEN = "pveBattle";
    private static final int PAUSE_MS = 3000;
    private static final int AFTER_TURN_MS = 1200;

    private final GameWindow window;
    private final BattleController controller;
    private final Runnable onExit;

    private BattleScreenForm form;
    private BattleSide side1; // el jugador
    private BattleSide side2; // la maquina

    public PveBattleFlow(GameWindow window, BattleController controller, Runnable onExit) {
        this.window = window;
        this.controller = controller;
        this.onExit = onExit;
    }

    public void start(Battle battle) {
        side1 = battle.getFirst();
        side2 = battle.getSecond();

        form = new BattleScreenForm(active(side1), active(side2));
        window.addScreen(SCREEN, form.getRootPanel());
        window.showScreen(SCREEN);

        form.setInputEnabled(false);
        form.showMessages(List.of(side1.getTrainer().getName() + " vs " + side2.getTrainer().getName()));
        pause(this::beginTurn);
    }

    // ---------- Turno del jugador ----------

    private void beginTurn() {
        if (controller.isFinished()) {
            finish();
            return;
        }
        sync();
        TurnAction forced = controller.getForcedAction(side1);
        if (forced != null) {
            form.setInputEnabled(false);
            form.showMessages(List.of(active(side1).getName() + " continua con "
                    + forced.getMove().getName() + "."));
            pause(() -> resolve(forced));
            return;
        }
        showMainMenu();
    }

    private void showMainMenu() {
        List<String> labels = new ArrayList<>();
        labels.add("ATACAR");
        if (side1.getTeam().hasBench()) {
            labels.add("CAMBIAR");
        }
        labels.add("RENDIRSE");

        form.showChoices("¿Que hara " + active(side1).getName() + "?",
                labels.toArray(new String[0]),
                index -> guarded(() -> {
                    String label = labels.get(index);
                    if (label.equals("ATACAR")) {
                        showMoves();
                    } else if (label.equals("CAMBIAR")) {
                        showSwitchMenu();
                    } else {
                        confirmSurrender();
                    }
                }),
                () -> {
                });
    }

    private void showMoves() {
        form.showMoves(
                index -> guarded(() -> resolve(controller.chooseAttack(side1, index + 1))),
                this::showMainMenu);
    }

    private void showSwitchMenu() {
        String[] labels = teamLabels(true);
        int backIndex = labels.length - 1;
        form.showChoices("Elige pokemon", labels,
                index -> guarded(() -> {
                    if (index == backIndex) {
                        showMainMenu();
                        return;
                    }
                    TurnAction action;
                    try {
                        action = controller.chooseSwitch(side1, index + 1);
                    } catch (InvalidDataException e) {
                        notice("Ese pokemon no puede entrar", this::showSwitchMenu);
                        return;
                    }
                    resolve(action);
                }),
                this::showMainMenu);
    }

    private void confirmSurrender() {
        form.showChoices("¿Seguro que te rindes?", new String[]{"SI, RENDIRSE", "NO"},
                index -> guarded(() -> {
                    if (index == 0) {
                        controller.surrender(side1);
                        finish();
                    } else {
                        showMainMenu();
                    }
                }),
                this::showMainMenu);
    }

    // ---------- Resolver el turno ----------

    private void resolve(TurnAction playerAction) {
        form.setInputEnabled(false);
        TurnAction machineAction = controller.chooseMachineAction();
        List<String> messages = new ArrayList<>(controller.playRound(playerAction, machineAction));
        addFainted(messages, side1);
        addFainted(messages, side2);

        sync();
        // Se anima ataque por ataque; al final queda un momento para leer
        form.playTurn(messages, controller.getLastRoundEvents(), () -> pause(AFTER_TURN_MS, this::afterRound));
    }

    private void afterRound() {
        if (controller.isFinished()) {
            finish();
            return;
        }
        if (controller.needsReplacement(side1)) {
            askReplacement();
            return;
        }
        replaceMachineIfNeeded(this::beginTurn);
    }

    // El jugador debe elegir quien entra (no hay opcion de volver)
    private void askReplacement() {
        form.showChoices("Elige el siguiente pokemon", teamLabels(false),
                index -> guarded(() -> {
                    Combatant next;
                    try {
                        next = controller.replaceFainted(side1, index + 1);
                    } catch (InvalidDataException e) {
                        notice("Ese pokemon no puede entrar", this::askReplacement);
                        return;
                    }
                    sync();
                    notice(side1.getTrainer().getName() + " envia a " + next.getName() + "!",
                            () -> replaceMachineIfNeeded(this::beginTurn));
                }),
                () -> {
                });
    }

    private void replaceMachineIfNeeded(Runnable after) {
        if (controller.needsReplacement(side2)) {
            Combatant next = controller.replaceMachineFainted();
            sync();
            notice(side2.getTrainer().getName() + " envia a " + next.getName() + "!", after);
        } else {
            after.run();
        }
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

        sync();
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

    private void sync() {
        form.setCombatants(active(side1), active(side2));
    }

    private void addFainted(List<String> messages, BattleSide side) {
        Combatant active = active(side);
        if (active.isFainted()) {
            messages.add(active.getName() + " de " + side.getTrainer().getName() + " se ha debilitado!");
        }
    }

    // Una etiqueta por miembro del equipo y, si se pide, "VOLVER" al final
    private String[] teamLabels(boolean withBack) {
        Team team = side1.getTeam();
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
        pause(PAUSE_MS, next);
    }

    private void pause(int ms, Runnable next) {
        Timer timer = new Timer(ms, e -> guarded(next));
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