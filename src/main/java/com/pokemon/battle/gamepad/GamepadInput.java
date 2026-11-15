package com.pokemon.battle.gamepad;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import javax.swing.Timer;
import java.util.function.Consumer;

public class GamepadInput {

    private static final double DEADZONE = 0.6;
    private static final long REPEAT_DELAY_NS = 350_000_000L; // antes del 1er repeat
    private static final long REPEAT_RATE_NS  = 120_000_000L; // entre repeats

    private ControllerManager controllers;
    private Timer timer;
    private InputProfile profile = InputProfile.CUALQUIER_BOTON;
    private Consumer<GameAction> listener = a -> {};
    private ControllerState lastState; // para el modo LIBRE

    private GameAction heldDir = null;
    private long nextRepeat = 0;

    public boolean start() {
        try {
            controllers = new ControllerManager();
            controllers.initSDLGamepad();
        } catch (Throwable t) {
            t.printStackTrace();
            return false; // el juego sigue con teclado
        }
        timer = new Timer(16, e -> poll(System.nanoTime())); // ~60 veces por segundo, en el EDT
        timer.start();
        return true;
    }

    public void stop() {
        if (timer != null) timer.stop();
        if (controllers != null) controllers.quitSDLGamepad();
    }

    public void setProfile(InputProfile p) {
        this.profile = p;
        this.heldDir = null; // evita que una dirección sostenida "pase" a la nueva pantalla
    }

    public void setListener(Consumer<GameAction> l) { this.listener = l; }

    /** Para el modo LIBRE: estado crudo del mando en este frame (puede ser null). */
    public ControllerState raw() { return lastState; }

    private void poll(long now) {
        ControllerState s = controllers.getState(0);
        lastState = s;
        if (!s.isConnected) return;

        switch (profile) {
            case CUALQUIER_BOTON -> {
                if (anyButton(s)) listener.accept(GameAction.ANY_BUTTON);
            }
            case MENU -> {
                directions(s, now);
                if (s.aJustPressed) listener.accept(GameAction.CONFIRM);
                if (s.bJustPressed) listener.accept(GameAction.BACK);
            }
            case LISTA -> {
                directions(s, now);
                if (s.aJustPressed) listener.accept(GameAction.CONFIRM);
                if (s.bJustPressed) listener.accept(GameAction.BACK);
                if (s.lbJustPressed) listener.accept(GameAction.PAGE_PREV);
                if (s.rbJustPressed) listener.accept(GameAction.PAGE_NEXT);
            }
            case EQUIPO -> {
                directions(s, now);
                if (s.aJustPressed) listener.accept(GameAction.CONFIRM);
                if (s.bJustPressed) listener.accept(GameAction.BACK);
                if (s.xJustPressed) listener.accept(GameAction.REMOVE_LAST);
                if (s.lbJustPressed) listener.accept(GameAction.PAGE_PREV);
                if (s.rbJustPressed) listener.accept(GameAction.PAGE_NEXT);
            }
            case LIBRE -> { /* no emite acciones: la pantalla lee raw() cada frame */ }
        }
    }

    private boolean anyButton(ControllerState s) {
        return s.aJustPressed || s.bJustPressed || s.xJustPressed || s.yJustPressed
                || s.lbJustPressed || s.rbJustPressed || s.startJustPressed || s.backJustPressed
                || s.dpadUpJustPressed || s.dpadDownJustPressed
                || s.dpadLeftJustPressed || s.dpadRightJustPressed;
    }

    private void directions(ControllerState s, long now) {
        GameAction dir = null;
        if (s.dpadUp    || s.leftStickY >  DEADZONE) dir = GameAction.UP;
        else if (s.dpadDown  || s.leftStickY < -DEADZONE) dir = GameAction.DOWN;
        else if (s.dpadLeft  || s.leftStickX < -DEADZONE) dir = GameAction.LEFT;
        else if (s.dpadRight || s.leftStickX >  DEADZONE) dir = GameAction.RIGHT;

        if (dir == null) { heldDir = null; return; }
        if (dir != heldDir) {
            heldDir = dir;
            nextRepeat = now + REPEAT_DELAY_NS;
            listener.accept(dir);
        } else if (now >= nextRepeat) {
            nextRepeat = now + REPEAT_RATE_NS;
            listener.accept(dir);
        }
    }
}