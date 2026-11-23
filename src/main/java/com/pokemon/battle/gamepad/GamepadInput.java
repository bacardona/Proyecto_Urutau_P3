package com.pokemon.battle.gamepad;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import javax.swing.SwingUtilities;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * Lee los mandos en un hilo propio (SDL se inicia y se usa SIEMPRE desde ese hilo)
 * y entrega las acciones al hilo de Swing con invokeLater.
 */
public class GamepadInput {

    // ---- Ajustes en un solo lugar
    private static final double DEADZONE = 0.6;
    private static final boolean INVERT_STICK_Y = true;  // palanca arriba/abajo al reves
    private static final boolean SWAP_AB = false;        // true = forzar el cambio en TODOS los mandos
    private static final long REPEAT_DELAY_NS = 350_000_000L;
    private static final long REPEAT_RATE_NS  = 120_000_000L;
    private static final long POLL_CONNECTED_MS = 16;    // ~60 Hz con mando
    private static final long POLL_IDLE_MS = 250;        // sin mando, casi no gasta nada
    // Combate libre: zona muerta mas chica para caminar, y gatillos desde la mitad
    private static final double FIGHT_DEADZONE = 0.45;
    private static final double TRIGGER_PRESSED = 0.5;
    /** Mandos que se leen: uno por jugador. */
    public static final int MAX_PADS = 2;

    // ---- Que acciones vale en cada perfil
    private static final EnumMap<InputProfile, EnumSet<GameAction>> ALLOWED =
            new EnumMap<>(InputProfile.class);
    static {
        EnumSet<GameAction> menu = EnumSet.of(GameAction.UP, GameAction.DOWN,
                GameAction.LEFT, GameAction.RIGHT, GameAction.CONFIRM, GameAction.BACK);
        EnumSet<GameAction> lista = EnumSet.copyOf(menu);
        lista.add(GameAction.PAGE_PREV);
        lista.add(GameAction.PAGE_NEXT);
        lista.add(GameAction.TOGGLE_VIEW);
        EnumSet<GameAction> equipo = EnumSet.copyOf(lista);
        equipo.add(GameAction.REMOVE_LAST);
        equipo.add(GameAction.START);

        ALLOWED.put(InputProfile.CUALQUIER_BOTON, EnumSet.of(GameAction.ANY_BUTTON));
        ALLOWED.put(InputProfile.MENU, menu);
        ALLOWED.put(InputProfile.LISTA, lista);
        ALLOWED.put(InputProfile.EQUIPO, equipo);
        ALLOWED.put(InputProfile.LIBRE, EnumSet.noneOf(GameAction.class)); // la pantalla lee snapshot()
    }

    // ---- Compartido entre hilos
    private volatile InputProfile profile = InputProfile.CUALQUIER_BOTON;
    private volatile Consumer<GameAction> listener = a -> {};
    private volatile int menuPadIndex = -1;   // -1 = el primer mando conectado
    private final PadSnapshot[] snapshots = {PadSnapshot.NONE, PadSnapshot.NONE};
    private volatile boolean running;

    // ---- Solo los toca el hilo del mando
    private Thread thread;
    private ControllerManager controllers;
    private InputProfile seenProfile;
    private GameAction heldDir;
    private long nextRepeat;

    /** Arranca el hilo del mando. Vuelve al instante: no bloquea la ventana. */
    public void start() {
        if (thread != null) return;
        running = true;
        thread = new Thread(this::run, "gamepad-poll");
        thread.setDaemon(true);
        thread.start();
    }

    /** Detiene el hilo; el propio hilo cierra SDL. */
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setProfile(InputProfile p) { this.profile = p; }

    public void setListener(Consumer<GameAction> l) { this.listener = l; }

    /** Que mando maneja los menus (0 o 1); -1 = el primero conectado. */
    public void setMenuPad(int index) { this.menuPadIndex = index; }

    /**
     * Combate libre: foto del mando de un jugador (0 = mando 1, 1 = mando 2).
     * Nunca devuelve null; si no hay mando, todo sale suelto.
     */
    public PadSnapshot snapshot(int player) {
        if (player < 0 || player >= MAX_PADS) return PadSnapshot.NONE;
        synchronized (snapshots) {
            return snapshots[player];
        }
    }

    // ------------------------------------------------------------------ hilo del mando

    private void run() {
        try {
            controllers = new ControllerManager();
            controllers.initSDLGamepad();
        } catch (Throwable t) {
            t.printStackTrace();   // el juego sigue con teclado y mouse
            return;
        }
        try {
            while (running) {
                ControllerState[] states = new ControllerState[MAX_PADS];
                ControllerState menuPad = null;   // por defecto, el primer mando conectado
                boolean any = false;
                for (int i = 0; i < MAX_PADS; i++) {
                    states[i] = controllers.getState(i);
                    PadSnapshot snap = toSnapshot(states[i]);
                    synchronized (snapshots) {
                        snapshots[i] = snap;
                    }
                    any |= snap.connected();
                    if (menuPad == null && states[i].isConnected) menuPad = states[i];
                }
                if (!any) {
                    Thread.sleep(POLL_IDLE_MS);
                    continue;
                }
                int wanted = menuPadIndex;
                if (wanted >= 0 && wanted < MAX_PADS) {
                    // Pantalla de un jugador concreto: solo manda su mando
                    menuPad = states[wanted].isConnected ? states[wanted] : null;
                }
                if (menuPad != null) poll(menuPad, System.nanoTime());
                Thread.sleep(POLL_CONNECTED_MS);
            }
        } catch (InterruptedException ignored) {
            // stop() pidio cerrar
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            controllers.quitSDLGamepad();
        }
    }

    private void poll(ControllerState s, long now) {
        InputProfile p = profile;
        if (p != seenProfile) {          // cambio de pantalla: olvidar direccion sostenida
            seenProfile = p;
            heldDir = null;
        }
        EnumSet<GameAction> allowed = ALLOWED.get(p);
        if (allowed.isEmpty()) return;

        if (allowed.contains(GameAction.ANY_BUTTON)) {
            if (anyButton(s)) emit(GameAction.ANY_BUTTON);
            return;
        }

        boolean nintendo = SWAP_AB || isNintendo(s);
        boolean confirm = nintendo ? s.bJustPressed : s.aJustPressed;
        boolean back = nintendo ? s.aJustPressed : s.bJustPressed;
        boolean removeLast = nintendo ? s.yJustPressed : s.xJustPressed;
        boolean toggleView = nintendo ? s.xJustPressed : s.yJustPressed;

        directions(s, now);
        if (confirm) emit(GameAction.CONFIRM);
        if (back) emit(GameAction.BACK);
        if (allowed.contains(GameAction.PAGE_PREV) && s.lbJustPressed) emit(GameAction.PAGE_PREV);
        if (allowed.contains(GameAction.PAGE_NEXT) && s.rbJustPressed) emit(GameAction.PAGE_NEXT);
        if (allowed.contains(GameAction.REMOVE_LAST) && removeLast) emit(GameAction.REMOVE_LAST);
        if (allowed.contains(GameAction.TOGGLE_VIEW) && toggleView) emit(GameAction.TOGGLE_VIEW);
        if (allowed.contains(GameAction.START) && s.startJustPressed) emit(GameAction.START);
    }

    private void directions(ControllerState s, long now) {
        double stickY = INVERT_STICK_Y ? -s.leftStickY : s.leftStickY;
        GameAction dir = null;
        if (s.dpadUp    || stickY > DEADZONE)             dir = GameAction.UP;
        else if (s.dpadDown  || stickY < -DEADZONE)       dir = GameAction.DOWN;
        else if (s.dpadLeft  || s.leftStickX < -DEADZONE) dir = GameAction.LEFT;
        else if (s.dpadRight || s.leftStickX >  DEADZONE) dir = GameAction.RIGHT;

        if (dir == null) { heldDir = null; return; }
        if (dir != heldDir) {
            heldDir = dir;
            nextRepeat = now + REPEAT_DELAY_NS;
            emit(dir);
        } else if (now >= nextRepeat) {
            nextRepeat = now + REPEAT_RATE_NS;
            emit(dir);
        }
    }

    private static PadSnapshot toSnapshot(ControllerState s) {
        if (s == null || !s.isConnected) return PadSnapshot.NONE;
        double stickY = INVERT_STICK_Y ? -s.leftStickY : s.leftStickY;
        boolean nintendo = SWAP_AB || isNintendo(s);
        boolean a = nintendo ? s.b : s.a;
        boolean b = nintendo ? s.a : s.b;
        boolean x = nintendo ? s.y : s.x;
        boolean y = nintendo ? s.x : s.y;
        return new PadSnapshot(true,
                s.dpadLeft || s.leftStickX < -FIGHT_DEADZONE,
                s.dpadRight || s.leftStickX > FIGHT_DEADZONE,
                s.dpadUp || stickY > FIGHT_DEADZONE,
                s.dpadDown || stickY < -FIGHT_DEADZONE,
                a, b, x, y,
                s.lb, s.rb, s.leftTrigger > TRIGGER_PRESSED, s.rightTrigger > TRIGGER_PRESSED,
                s.start, s.back);
    }

    /** True si el mando es de Nintendo (Pro Controller / Switch). */
    private static boolean isNintendo(ControllerState s) {
        String type = String.valueOf(s.controllerType).toUpperCase();
        return type.contains("SWITCH") || type.contains("NINTENDO");
    }

    private boolean anyButton(ControllerState s) {
        return s.aJustPressed || s.bJustPressed || s.xJustPressed || s.yJustPressed
                || s.lbJustPressed || s.rbJustPressed || s.startJustPressed || s.backJustPressed
                || s.dpadUpJustPressed || s.dpadDownJustPressed
                || s.dpadLeftJustPressed || s.dpadRightJustPressed;
    }

    /** Entrega la accion en el hilo de Swing, para que las pantallas se toquen solo desde ahi. */
    private void emit(GameAction action) {
        Consumer<GameAction> l = listener;
        SwingUtilities.invokeLater(() -> l.accept(action));
    }
}