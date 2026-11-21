package com.pokemon.battle.fighting;

import com.pokemon.battle.gamepad.PadSnapshot;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.util.List;

/** Checks that the gamepad buttons drive free fight like the keyboard. Run with main. */
public class FreeFightGamepadCheck {

    private static final double DT = 1 / 60.0;

    public static void main(String[] args) throws Exception {
        GamepadFightControls pads = new GamepadFightControls();
        FightingInput input = new FightingInput();

        // Palanca a la derecha: camina; al soltar se detiene
        pads.apply(0, pad().right(true).build(), input);
        check(input.getPlayerOneDirection() == 1, "Stick right must walk right");
        pads.apply(0, pad().build(), input);
        check(input.getPlayerOneDirection() == 0, "Releasing the stick must stop");

        // El mando 2 maneja al jugador 2
        pads.apply(1, pad().left(true).build(), input);
        check(input.getPlayerTwoDirection() == -1 && input.getPlayerOneDirection() == 0,
                "Pad 2 must move player 2 only");
        pads.apply(1, pad().build(), input);

        // Si el mando no cambia, no pisa el teclado (tecla D apretada y mando quieto)
        input.setPlayerOneRight(true);
        pads.apply(0, pad().build(), input);
        check(input.getPlayerOneDirection() == 1, "An idle pad must not cancel the keyboard");
        input.setPlayerOneRight(false);

        // LB o abajo = escudo
        pads.apply(0, pad().lb(true).build(), input);
        check(input.isPlayerOneBlockingRequested(), "LB must block");
        pads.apply(0, pad().build(), input);
        check(!input.isPlayerOneBlockingRequested(), "Releasing LB must drop the shield");
        pads.apply(0, pad().down(true).build(), input);
        check(input.isPlayerOneBlockingRequested(), "Down must block");
        pads.apply(0, pad().build(), input);

        // Desconectar el mando con botones apretados: se sueltan solos
        pads.apply(0, pad().right(true).lb(true).build(), input);
        pads.apply(0, PadSnapshot.NONE, input);
        check(input.getPlayerOneDirection() == 0 && !input.isPlayerOneBlockingRequested(),
                "Unplugging must release everything");

        // En una pelea real: X pega, A salta, SELECT cambia en la pared
        FightingGameSession session = new FightingGameSession(setup());
        GamepadFightControls live = new GamepadFightControls();
        FightingInput liveInput = new FightingInput();
        Fighter a = session.getFighters().get(0);
        Fighter b = session.getFighters().get(1);
        a.setX(600);
        b.setX(600 + a.getBodyHalfWidth() + b.getBodyHalfWidth() + 12);
        live.apply(0, pad().x(true).build(), liveInput);
        for (int i = 0; i < 30 && b.getHealth() == b.getMaxHealth(); i++) session.update(liveInput, DT);
        check(b.getHealth() < b.getMaxHealth(), "X must attack");
        live.apply(0, pad().build(), liveInput);
        for (int i = 0; i < 60; i++) session.update(liveInput, DT);

        // Solo A salta: palanca o cruceta hacia arriba no
        live.apply(0, new PadSnapshot(true, false, false, true, false,
                false, false, false, false, false, false, false, false, false, false), liveInput);
        session.update(liveInput, DT);
        check(a.isOnGround(), "Up on the stick or D-pad must not jump");
        live.apply(0, pad().build(), liveInput);
        session.update(liveInput, DT);
        live.apply(0, pad().a(true).build(), liveInput);
        session.update(liveInput, DT);
        check(!a.isOnGround(), "A must jump");
        live.apply(0, pad().build(), liveInput);
        for (int i = 0; i < 90; i++) session.update(liveInput, DT);

        a.setX(PhysicsSystem.halfWidthFor(a));
        live.apply(0, pad().back(true).build(), liveInput);
        session.update(liveInput, DT);
        check(session.getFighters().get(0) != a, "SELECT at the wall must switch");

        // justPressed: solo el primer instante cuenta (para START y los menus)
        GamepadFightControls menu = new GamepadFightControls();
        PadSnapshot start = pad().start(true).build();
        check(menu.justPressed(0, start, PadSnapshot::start), "START first press");
        menu.remember(0, start);
        check(!menu.justPressed(0, start, PadSnapshot::start), "Holding START is not a new press");
        if (!java.awt.GraphicsEnvironment.isHeadless()) panelChecks();
        System.out.println("PASS gamepad free fight");
    }

    /** Real screen with a fake pad: START pauses, the D-pad moves the menu, B resumes. */
    private static void panelChecks() throws Exception {
        PadSnapshot[] current = {PadSnapshot.NONE, PadSnapshot.NONE};
        FightingGamePanel[] panel = new FightingGamePanel[1];
        javax.swing.JFrame[] frame = new javax.swing.JFrame[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            panel[0] = new FightingGamePanel(setup(), () -> { });
            panel[0].setGamepadSource(i -> current[i]);
            frame[0] = new javax.swing.JFrame();
            frame[0].add(panel[0]);
            frame[0].setSize(1280, 760);
            frame[0].setVisible(true);
        });
        FightingWindowChecks.awaitGameplay(panel[0]);
        // Con mando conectado, el aviso de evolucion dice LT en vez de R
        current[0] = pad().build();
        FightingWindowChecks.awaitCondition(() -> ((boolean[]) field(panel[0], "padConnected"))[0],
                "Panel must notice the pad");
        Object hud = field(field(panel[0], "renderer"), "hud");
        var label = hud.getClass().getDeclaredMethod("energyLabel", Fighter.class, int.class);
        label.setAccessible(true);
        Fighter one = session(panel[0]).getFighters().get(0);
        session(panel[0]).debugFillActiveEnergy(0);
        check("EVOLUCIONA (LT)".equals(label.invoke(hud, one, 0)), "Pad label must say LT");
        check("EVOLUCIONA (P)".equals(label.invoke(hud, session(panel[0]).getFighters().get(1), 1))
                || !session(panel[0]).getFighters().get(1).isEvolutionReady(), "Keyboard player keeps P");
        current[0] = pad().start(true).build();
        FightingWindowChecks.awaitCondition(() -> session(panel[0]).getState()
                == FightingGameSession.State.PAUSED, "START must pause");
        current[0] = PadSnapshot.NONE;
        Thread.sleep(100);
        current[0] = new PadSnapshot(true, false, false, false, true,
                false, false, false, false, false, false, false, false, false, false);
        FightingWindowChecks.awaitCondition(() -> (int) field(panel[0], "selectedOption") == 1,
                "D-pad down must move the pause menu");
        current[0] = PadSnapshot.NONE;
        Thread.sleep(100);
        current[0] = new PadSnapshot(true, false, false, false, false,
                false, true, false, false, false, false, false, false, false, false);
        FightingWindowChecks.awaitCondition(() -> session(panel[0]).getState()
                == FightingGameSession.State.RUNNING, "B must close the pause menu");
        // El mando 2 camina en la pelea real
        current[0] = PadSnapshot.NONE;
        Fighter two = session(panel[0]).getFighters().get(1);
        double before = two.getX();
        current[1] = pad().left(true).build();
        FightingWindowChecks.awaitCondition(() -> two.getX() < before - 20, "Pad 2 must walk in the real panel");
        current[1] = PadSnapshot.NONE;
        // MANDO: LADO DERECHO -> el mando 1 maneja al Pokemon de la derecha
        check(panel[0].padIndexFor(0) == 0 && panel[0].padIndexFor(1) == 1, "Pads start on their own side");
        javax.swing.SwingUtilities.invokeAndWait(() -> panel[0].togglePadSide());
        check(panel[0].padIndexFor(1) == 0, "After the switch pad 1 must drive the right player");
        Thread.sleep(100);
        double rightBefore = two.getX();
        double leftBefore = one.getX();
        current[0] = pad().left(true).build();
        FightingWindowChecks.awaitCondition(() -> two.getX() < rightBefore - 20,
                "Pad 1 must walk the right Pokemon after the switch");
        check(Math.abs(one.getX() - leftBefore) < 1, "Left Pokemon stays for the keyboard");
        current[0] = PadSnapshot.NONE;
        javax.swing.SwingUtilities.invokeAndWait(() -> panel[0].togglePadSide());
        check(panel[0].padIndexFor(0) == 0, "Switching again goes back to normal");
        javax.swing.SwingUtilities.invokeAndWait(() -> frame[0].dispose());
    }

    private static FightingGameSession session(FightingGamePanel panel) throws Exception {
        return (FightingGameSession) field(panel, "session");
    }

    private static Object field(Object owner, String name) throws Exception {
        var f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(owner);
    }

    // ---- ayudante para armar fotos del mando
    private static Pad pad() { return new Pad(); }

    private static final class Pad {
        boolean left, right, up, down, a, b, x, y, lb, rb, lt, rt, start, back;
        Pad left(boolean v) { left = v; return this; }
        Pad right(boolean v) { right = v; return this; }
        Pad down(boolean v) { down = v; return this; }
        Pad a(boolean v) { a = v; return this; }
        Pad x(boolean v) { x = v; return this; }
        Pad lb(boolean v) { lb = v; return this; }
        Pad start(boolean v) { start = v; return this; }
        Pad back(boolean v) { back = v; return this; }
        PadSnapshot build() {
            return new PadSnapshot(true, left, right, up, down, a, b, x, y, lb, rb, lt, rt, start, back);
        }
    }

    private static FreeFightSetup setup() {
        FreeFightSetup setup = new FreeFightSetup();
        setup.setPlayerOne(new Player(1, "A", 0, 0), List.of(p("CHARMANDER", Type.FIRE), p("PIKACHU", Type.ELECTRIC)));
        setup.setPlayerTwo(new Player(2, "B", 0, 0), List.of(p("SQUIRTLE", Type.WATER), p("EEVEE", Type.NORMAL)));
        return setup;
    }

    private static Pokemon p(String name, Type type) {
        return new Pokemon(1, name, 50, 50, 50, 50, 50, 50, type, Type.NONE, List.of());
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
