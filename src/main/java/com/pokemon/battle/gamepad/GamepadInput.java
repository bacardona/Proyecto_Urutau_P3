package com.pokemon.battle.gamepad;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import javax.swing.SwingUtilities;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * Lee el mando en un hilo propio (SDL se inicia y se usa SIEMPRE desde ese hilo)
 * y entrega las acciones al hilo de Swing con invokeLater.
 */
public class GamepadInput {

    // ---- Ajustes en un solo lugar
    private static final double DEADZONE = 0.6;
    private static final boolean INVERT_STICK_Y = true;  // tu palanca daba arriba/abajo al reves
    private static final boolean SWAP_AB = false;        // ponlo en true si A y B salen cruzados
    private static final long REPEAT_DELAY_NS = 350_000_000L;
    private static final long REPEAT_RATE_NS  = 120_000_000L;
    private static final long POLL_CONNECTED_MS = 16;    // ~60 Hz con mando
    private static final long POLL_IDLE_MS = 250;        // sin mando, casi no gasta nada
    // Combate libre: zona muerta mas chica para caminar, y gatillos (LT/RT) desde la mitad
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
        EnumSet<GameAction> equipo = EnumSet.copyOf(lista);
        equipo.add(GameAction.REMOVE_LAST);

        ALLOWED.put(InputProfile.CUALQUIER_BOTON, EnumSet.of(GameAction.ANY_BUTTON));
        ALLOWED.put(InputProfile.MENU, menu);
        ALLOWED.put(InputProfile.LISTA, lista);
        ALLOWED.put(InputProfile.EQUIPO, equipo);
        ALLOWED.put(InputProfile.LIBRE, EnumSet.noneOf(GameAction.class)); // la pantalla lee raw()
    }

    /**
     * Mandos cambiados de lado (opcion "MANDO: LADO DERECHO" de la pausa del combate).
     * Normal: mando 1 = jugador 1, mando 2 = jugador 2. Cambiado: al reves.
     */
    private static volatile boolean padsSwapped;

    public static boolean arePadsSwapped() { return padsSwapped; }

    public static void setPadsSwapped(boolean swapped) { padsSwapped = swapped; }

    /** Que mando (0 o 1) maneja a este jugador (0 = jugador 1, 1 = jugador 2). */
    public static int padForPlayer(int player) {
        return padsSwapped ? 1 - player : player;
    }

    /**
     * Si este mando puede mover el menu. Sin jugador (activePlayer < 0) sirven los dos.
     * En las pantallas de un jugador solo sirve su mando; si ese mando no esta conectado,
     * sirve el otro, para que con un solo mando se pueda elegir igual.
     */
    static boolean padMayNavigate(int pad, int activePlayer, boolean ownerConnected) {
        if (activePlayer < 0) return true;
        int owner = padForPlayer(activePlayer);
        return pad == owner || !ownerConnected;
    }

    // ---- Compartido entre hilos
    private volatile InputProfile profile = InputProfile.CUALQUIER_BOTON;
    /** Jugador que esta eligiendo en la pantalla actual (-1 = cualquiera de los dos). */
    private volatile int activePlayer = -1;
    private volatile Consumer<GameAction> listener = a -> {};
    private volatile ControllerState lastState;   // para el modo LIBRE
    private final PadSnapshot[] snapshots = {PadSnapshot.NONE, PadSnapshot.NONE};
    private volatile boolean running;

    // ---- Solo los toca el hilo del mando
    private Thread thread;
    private ControllerManager controllers;
    private InputProfile seenProfile;
    private final GameAction[] heldDir = new GameAction[MAX_PADS];
    private final long[] nextRepeat = new long[MAX_PADS];

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

    /** Pantallas de un solo jugador (elegir entrenador y equipo): solo responde su mando. */
    public void setActivePlayer(int player) { this.activePlayer = player; }

    public void setListener(Consumer<GameAction> l) { this.listener = l; }

    /** Para el modo LIBRE: ultimo estado crudo del mando (puede ser null). */
    public ControllerState raw() { return lastState; }

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
            long t0 = System.nanoTime();
            controllers = new ControllerManager();
            System.out.println("ControllerManager: " + (System.nanoTime() - t0) / 1_000_000 + " ms");

            long t1 = System.nanoTime();
            controllers.initSDLGamepad();
            System.out.println("initSDLGamepad: " + (System.nanoTime() - t1) / 1_000_000 + " ms");
        } catch (Throwable t) {
            t.printStackTrace();   // el juego sigue con teclado y mouse
            return;
        }
        try {
            while (running) {
                ControllerState s = controllers.getState(0);
                lastState = s;
                // Fotos de los dos mandos para el combate libre (se leen aqui, en el hilo de SDL)
                boolean any = false;
                ControllerState[] pads = new ControllerState[MAX_PADS];
                for (int i = 0; i < MAX_PADS; i++) {
                    ControllerState pad = i == 0 ? s : controllers.getState(i);
                    pads[i] = pad;
                    PadSnapshot snap = toSnapshot(pad);
                    synchronized (snapshots) {
                        snapshots[i] = snap;
                    }
                    any |= snap.connected();
                }
                if (!any) {
                    Thread.sleep(POLL_IDLE_MS);
                    continue;
                }
                // Menus: los dos mandos por igual, salvo en las pantallas de un solo jugador
                int player = activePlayer;
                int owner = player < 0 ? -1 : padForPlayer(player);
                boolean ownerConnected = owner >= 0 && pads[owner] != null && pads[owner].isConnected;
                long now = System.nanoTime();
                resetHeldOnScreenChange();
                for (int i = 0; i < MAX_PADS; i++) {
                    if (pads[i] != null && pads[i].isConnected && padMayNavigate(i, player, ownerConnected)) {
                        poll(pads[i], i, now);
                    } else {
                        heldDir[i] = null;
                    }
                }
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

    /** Cambio de pantalla: olvidar la direccion sostenida de los dos mandos. */
    private void resetHeldOnScreenChange() {
        InputProfile p = profile;
        if (p != seenProfile) {
            seenProfile = p;
            java.util.Arrays.fill(heldDir, null);
        }
    }

    private void poll(ControllerState s, int pad, long now) {
        InputProfile p = profile;
        EnumSet<GameAction> allowed = ALLOWED.get(p);
        if (allowed.isEmpty()) return;

        if (allowed.contains(GameAction.ANY_BUTTON)) {
            if (anyButton(s)) emit(GameAction.ANY_BUTTON);
            return;
        }

        directions(s, pad, now);
        if (SWAP_AB ? s.bJustPressed : s.aJustPressed) emit(GameAction.CONFIRM);
        if (SWAP_AB ? s.aJustPressed : s.bJustPressed) emit(GameAction.BACK);
        if (allowed.contains(GameAction.PAGE_PREV) && s.lbJustPressed) emit(GameAction.PAGE_PREV);
        if (allowed.contains(GameAction.PAGE_NEXT) && s.rbJustPressed) emit(GameAction.PAGE_NEXT);
        if (allowed.contains(GameAction.REMOVE_LAST) && s.xJustPressed) emit(GameAction.REMOVE_LAST);
    }

    private void directions(ControllerState s, int pad, long now) {
        double stickY = INVERT_STICK_Y ? -s.leftStickY : s.leftStickY;
        GameAction dir = null;
        if (s.dpadUp    || stickY > DEADZONE)          dir = GameAction.UP;
        else if (s.dpadDown  || stickY < -DEADZONE)    dir = GameAction.DOWN;
        else if (s.dpadLeft  || s.leftStickX < -DEADZONE) dir = GameAction.LEFT;
        else if (s.dpadRight || s.leftStickX >  DEADZONE) dir = GameAction.RIGHT;

        if (dir == null) { heldDir[pad] = null; return; }
        if (dir != heldDir[pad]) {
            heldDir[pad] = dir;
            nextRepeat[pad] = now + REPEAT_DELAY_NS;
            emit(dir);
        } else if (now >= nextRepeat[pad]) {
            nextRepeat[pad] = now + REPEAT_RATE_NS;
            emit(dir);
        }
    }

    private static PadSnapshot toSnapshot(ControllerState s) {
        if (s == null || !s.isConnected) return PadSnapshot.NONE;
        double stickY = INVERT_STICK_Y ? -s.leftStickY : s.leftStickY;
        return new PadSnapshot(true,
                s.dpadLeft || s.leftStickX < -FIGHT_DEADZONE,
                s.dpadRight || s.leftStickX > FIGHT_DEADZONE,
                s.dpadUp || stickY > FIGHT_DEADZONE,
                s.dpadDown || stickY < -FIGHT_DEADZONE,
                SWAP_AB ? s.b : s.a, SWAP_AB ? s.a : s.b, s.x, s.y,
                s.lb, s.rb, s.leftTrigger > TRIGGER_PRESSED, s.rightTrigger > TRIGGER_PRESSED,
                s.start, s.back);
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