package com.pokemon.battle.fighting;

import com.pokemon.battle.MainGUI;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.ui.GameWindow;
import com.pokemon.battle.ui.SpriteLoader;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/** Regression checks for independent status actions/effects and stable sprite-sized guards. */
public final class FreeFightStatusShieldCheck {
    private static final double DT = 1.0 / 60;
    private static int checks;
    private static final String[] BASE = {"BULBASAUR", "CHARMANDER", "SQUIRTLE", "EEVEE"};
    private static final String[] EVOLVED = {"IVYSAUR", "CHARMELEON", "WARTORTLE", "FLAREON"};

    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    @SuppressWarnings("unchecked")
    private static double cooldown(FightingGameSession session, String move) throws Exception {
        var clocks = (List<Map<String, Double>>) field(session.getCombatSystem(), "moveCooldowns");
        return clocks.get(0).getOrDefault(move, 0.0);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-8, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static FightingGameSession session(String species) throws Exception {
        var ctor = FightingGameSession.class.getDeclaredConstructor(
                String.class, List.class, String.class, List.class, boolean.class);
        ctor.setAccessible(true);
        var p = new Pokemon(1, species, 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
        var q = new Pokemon(2, "SQUIRTLE", 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
        return ctor.newInstance("P1", List.of(p, p, p), "P2", List.of(q, q, q), false);
    }

    private static FightingInput request(int slot) {
        var input = new FightingInput();
        switch (slot) {
            case 0 -> input.setPlayerOneAttackOne(true);
            case 1 -> input.setPlayerOneAttackTwo(true);
            case 2 -> input.setPlayerOneAttackThree(true);
            case 3 -> input.setPlayerOneAttackFour(true);
            default -> { }
        }
        return input;
    }

    private static void ticks(FightingGameSession session, int count) {
        for (int i = 0; i < count; i++) session.update(new FightingInput(), DT);
    }

    private static void evolve(Fighter fighter) {
        fighter.addEvolutionEnergy(100);
        check(fighter.evolve(), "Cannot prepare evolution");
        fighter.updateStatusEffects(1.1);
    }

    private record StatusCase(String base, int slot, String move, String effect, double duration,
                              double cooldown, boolean self) { }
    private static final List<StatusCase> STATUS = List.of(
            new StatusCase("BULBASAUR", 3, "GROWTH", "attackBoostSeconds", 6, 6.08, true),
            new StatusCase("BULBASAUR", 2, "SLEEP_POWDER", "sleepSeconds", 1.25, 3.32, false),
            new StatusCase("EEVEE", 2, "GROWL", "attackReductionSeconds", 3.5, 4.37, false),
            new StatusCase("CHARMANDER", 2, "GROWL", "attackReductionSeconds", 3.5, 4.37, false),
            new StatusCase("EEVEE", 3, "TAIL_WHIP", "defenseReductionSeconds", 3, 1.02, false),
            new StatusCase("SQUIRTLE", 2, "TAIL_WHIP", "defenseReductionSeconds", 3, 1.02, false));

    private static Fighter affected(FightingGameSession session, StatusCase status) {
        return session.getFighters().get(status.self ? 0 : 1);
    }

    private static double effect(FightingGameSession session, StatusCase status) throws Exception {
        return (double) field(affected(session, status), status.effect);
    }

    private static void contact(FightingGameSession session, boolean left) {
        var a = session.getFighters().get(0);
        var b = session.getFighters().get(1);
        a.setX(600); a.setY(677); a.setVelocityY(0); a.setOnGround(true); a.setFacingLeft(left);
        b.setX(600 + (left ? -1 : 1) * (a.getBodyHalfWidth() + b.getBodyHalfWidth() + 1));
        b.setY(677); b.setVelocityY(0); b.setOnGround(true); b.setFacingLeft(!left);
    }

    private static FightingGameSession cast(StatusCase status, boolean evolved, boolean left) throws Exception {
        var session = session(status.base);
        if (evolved) evolve(session.getFighters().get(0));
        contact(session, left);
        session.update(request(status.slot), DT);
        near(cooldown(session, status.move), status.cooldown, "Original cast cooldown");
        var old = session.getCombatSystem().getAttackVisual(session.getFighters().get(0), session.getFighters());
        check(old != null && old.definition().moveId().equals(status.move), "Cast did not start");
        for (int i = 0; i < 60 && effect(session, status) == 0; i++) ticks(session, 1);
        near(effect(session, status), status.duration, "Original effect duration: " + status.move);
        // The startup/active phases remain real action locks.
        if (!status.move.equals("TAIL_WHIP") && !status.move.equals("SLEEP_POWDER")) {
            session.update(request(0), DT);
            var visual = session.getCombatSystem().getAttackVisual(session.getFighters().get(0), session.getFighters());
            check(visual != null && visual.definition().moveId().equals(status.move), "Startup/active lock lost");
        }
        int frames = 0;
        while (session.getCombatSystem().getAttackVisual(session.getFighters().get(0), session.getFighters()) != null
                && frames++ < 80) ticks(session, 1);
        check(frames < 80, status.move + " locks the caster for its cooldown");
        check(effect(session, status) > 0, "Effect ended before action: " + status.move);
        return session;
    }

    private static void statusChecks() throws Exception {
        for (var status : STATUS) for (boolean evolved : new boolean[]{false, true})
            for (boolean left : new boolean[]{false, true}) {
                var session = cast(status, evolved, left);
                var a = session.getFighters().get(0); var b = session.getFighters().get(1);
                contact(session, left);
                double multiplier = a.getOutgoingDamageMultiplier() * b.getIncomingDamageMultiplier();
                int health = b.getHealth();
                session.update(request(0), DT);
                var visual = session.getCombatSystem().getAttackVisual(a, session.getFighters());
                check(visual != null && visual.definition().moveId().equals("TACKLE"),
                        a.getPokemonId() + " cannot attack during " + status.move);
                check(effect(session, status) > 0, "Effect lost on new attack");
                for (int i = 0; i < 18 && b.getHealth() == health; i++) ticks(session, 1);
                check(health - b.getHealth() == Math.round(8 * multiplier), "Wrong modified Tackle damage");
                if (status.self) near(a.getOutgoingDamageMultiplier(), 1.35 * (evolved ? 1.12 : 1), "Growth multiplier");
                if (status.move.equals("GROWL")) {
                    near(b.getOutgoingDamageMultiplier(), 0.72, "Growl multiplier");
                    ticks(session, 35); contact(session, left);
                    int before = a.getHealth();
                    var response = new FightingInput(); response.setPlayerTwoAttackOne(true);
                    session.update(response, DT);
                    var reply = session.getCombatSystem().getAttackVisual(b, session.getFighters());
                    check(reply != null && reply.definition().moveId().equals("TACKLE"), "Debuff locks rival");
                    ticks(session, 14);
                    check(a.getHealth() == before - 6, "Reduced rival did not deal 6 damage");
                }
                if (status.move.equals("SLEEP_POWDER")) {
                    near(effect(session, status), 0, "Existing wake-on-damage rule changed");
                    session = cast(status, evolved, left); // Expiry without a waking hit.
                }
                double remaining = effect(session, status);
                check(remaining > 0, "Effect vanished prematurely");
                session.pause(); session.update(new FightingInput(), 2);
                near(effect(session, status), remaining, "Pause advances effect"); session.resume();
                ticks(session, (int) Math.floor(remaining / DT) - 1);
                check(effect(session, status) > 0, "Effect expired early");
                ticks(session, 3);
                near(effect(session, status), 0, "Effect failed to expire");
                near(affected(session, status).getOutgoingDamageMultiplier(),
                        status.self && evolved ? 1.12 : 1, "Outgoing damage not restored");
                near(affected(session, status).getIncomingDamageMultiplier(), 1, "Defense not restored");
                System.out.println("OK " + a.getPokemonId() + " " + status.move + " -> Tackle, effect/damage/expiry, left=" + left);
            }
    }

    private static void otherActions() throws Exception {
        // Every other slot, locomotion and defense must work during Growth/Growl.
        for (var status : STATUS) if (status.move.equals("GROWTH") || status.move.equals("GROWL"))
            for (boolean evolved : new boolean[]{false, true}) {
                for (int slot = 0; slot < 4; slot++) if (slot != status.slot) {
                    var session = cast(status, evolved, false);
                    var a = session.getFighters().get(0);
                    if (status.base.equals("CHARMANDER") && slot == 3) session.getFighters().get(1).setX(850);
                    session.update(request(slot), DT);
                    var visual = session.getCombatSystem().getAttackVisual(a, session.getFighters());
                    check(visual != null && !visual.definition().moveId().equals(status.move), "Other slot blocked");
                    check(effect(session, status) > 0, "Other attack cleared effect");
                }
                var session = cast(status, evolved, false);
                var a = session.getFighters().get(0);
                var input = new FightingInput(); input.setPlayerOneLeft(true);
                double x = a.getX(); session.update(input, DT);
                check(a.getX() < x, "Movement locked by effect");
                input.clear(); input.setPlayerOneBlock(true); session.update(input, DT);
                check(a.isBlocking(), "Defense locked by effect");
                input.clear(); input.setPlayerOneJump(true); session.update(input, DT);
                check(!a.isOnGround(), "Jump locked by effect");
                check(effect(session, status) > 0, "Locomotion clears effect");

                // A second cast still respects the ORIGINAL cooldown, even with a free action state.
                session = cast(status, evolved, false);
                double clock = cooldown(session, status.move);
                check(clock > 0, "Cooldown incorrectly shortened");
                session.update(request(status.slot), DT);
                check(session.getCombatSystem().getAttackVisual(session.getFighters().get(0), session.getFighters()) == null,
                        "Same move bypassed cooldown");
                near(cooldown(session, status.move), clock - DT, "Cooldown reset by rejected input");
            }
    }

    private static void bufferedFollowup() throws Exception {
        for (var status : STATUS) if (status.move.equals("GROWTH") || status.move.equals("GROWL")) {
            var session = session(status.base);
            session.update(request(status.slot), DT);
            var a = session.getFighters().get(0);
            boolean buffered = false;
            for (int i = 0; i < 80; i++) {
                var visual = session.getCombatSystem().getAttackVisual(a, session.getFighters());
                if (visual != null && visual.phase().equals("RECOVERY") && visual.progress() > 0.75) {
                    session.update(request(0), DT); buffered = true; break;
                }
                ticks(session, 1);
            }
            check(buffered, "Cannot reach brief status recovery");
            for (int i = 0; i < 8; i++) ticks(session, 1);
            var visual = session.getCombatSystem().getAttackVisual(a, session.getFighters());
            check(visual != null && visual.definition().moveId().equals("TACKLE"), "Buffered follow-up lost");
            check(effect(session, status) > 0, "Buffered attack lost effect");
        }
    }

    private static void shieldChecks() throws Exception {
        var renderer = new FightingRenderer(SpriteLoader.shared());
        Method drawSprite = FightingRenderer.class.getDeclaredMethod("drawSpriteAnimation", Graphics2D.class,
                Fighter.class, FightingSpriteProfile.class, FightingCombatSystem.AttackVisual.class, double.class);
        Method drawShield = FightingRenderer.class.getDeclaredMethod("drawBlockShield", Graphics2D.class,
                Fighter.class, FightingCombatSystem.class, boolean.class);
        drawSprite.setAccessible(true); drawShield.setAccessible(true);
        var preview = new BufferedImage(1280, 700, BufferedImage.TYPE_INT_RGB);
        var pg = preview.createGraphics(); pg.setColor(new Color(0x293746)); pg.fillRect(0, 0, 1280, 700);
        for (int species = 0; species < 8; species++) for (boolean left : new boolean[]{false, true}) {
            var f = new Fighter(BASE[species % 4], 320, 450, left);
            if (species >= 4) evolve(f);
            var profile = FightingSpriteProfile.forFighter(f);
            var envelope = profile.shieldEnvelope(left);
            check(envelope == profile.shieldEnvelope(left), "Shield envelope not cached");
            near(envelope.radiusX(), profile.shieldEnvelope(!left).radiusX(), "Facing changes shield width");
            near(envelope.radiusY(), profile.shieldEnvelope(!left).radiusY(), "Facing changes shield height");
            double half = f.getBodyHalfWidth(), height = f.getRenderHeight();
            var combat = new FightingCombatSystem(); f.setBlocking(true);
            f.updateMotionAnimation(Fighter.MotionAnimation.BLOCKING, 0);
            f.updateShieldVisual(false, 0.3);
            var body = new BufferedImage(640, 600, BufferedImage.TYPE_INT_ARGB);
            var shield = new BufferedImage(640, 600, BufferedImage.TYPE_INT_ARGB);
            for (int sample = 0; sample < 36; sample++) {
                // Cover both the smallest point of the pulse and guard's full native sequence.
                f.updateMotionAnimation(Fighter.MotionAnimation.BLOCKING, 0.06);
                f.updateShieldVisual(false, 0.06);
                var bg = body.createGraphics(); bg.setComposite(AlphaComposite.Clear); bg.fillRect(0, 0, 640, 600);
                bg.setComposite(AlphaComposite.SrcOver); drawSprite.invoke(renderer, bg, f, profile, null, f.getX()); bg.dispose();
                var sg = shield.createGraphics(); sg.setComposite(AlphaComposite.Clear); sg.fillRect(0, 0, 640, 600);
                sg.setComposite(AlphaComposite.SrcOver); drawShield.invoke(renderer, sg, f, combat, false); sg.dispose();
                int pixels = 0;
                for (int y = 200; y < 520; y++) for (int x = 140; x < 500; x++)
                    if ((body.getRGB(x, y) >>> 24) != 0) {
                        pixels++;
                        check((shield.getRGB(x, y) >>> 24) != 0, profile.species + " sprite outside bubble");
                        for (int[] d : new int[][]{{-3, 0}, {3, 0}, {0, -3}, {0, 3}})
                            check((shield.getRGB(x + d[0], y + d[1]) >>> 24) != 0, profile.species + " no shield margin");
                    }
                check(pixels > 100, "Sprite not rendered");
                check(envelope == profile.shieldEnvelope(left), "Frame resizes shield envelope");
            }
            near(f.getBodyHalfWidth(), half, "Shield modified collision width");
            near(f.getRenderHeight(), height, "Shield modified body height");
            near(f.getX(), 320, "Shield moved fighter"); near(f.getY(), 450, "Shield moved feet");
            if (!left) {
                f.setX((species % 4) * 320 + 160); f.setY(species < 4 ? 310 : 630);
                drawShield.invoke(renderer, pg, f, combat, false); drawSprite.invoke(renderer, pg, f, profile, null, f.getX());
                drawShield.invoke(renderer, pg, f, combat, true);
                pg.setColor(Color.WHITE); pg.drawString(profile.species, (int) f.getX() - 45, species < 4 ? 145 : 465);
                System.out.printf("OK shield %s: oval %.0fx%.0f, offset %.0f,%.0f%n", profile.species,
                        2 * envelope.radiusX(), 2 * envelope.radiusY(), envelope.centerX(), envelope.centerY());
            }
            f.setBlocking(false); f.updateShieldVisual(false, 0.05);
            check(f.getShieldOpacity() > 0 && f.getShieldOpacity() < 1, "Shield lacks release fade");
            f.updateShieldVisual(false, 0.2); check(f.getShieldOpacity() == 0, "Shield does not disappear");
        }
        pg.dispose(); ImageIO.write(preview, "png", new File("target/free-fight-status-shields.png"));
        for (String base : BASE) {
            var f = new Fighter(base, 500, 677, false);
            var initial = FightingSpriteProfile.forFighter(f).shieldEnvelope(false);
            evolve(f);
            var grown = FightingSpriteProfile.forFighter(f).shieldEnvelope(false);
            check(initial != grown, "Evolution uses base envelope");
            f.returnToBaseAfterUltimate();
            check(FightingSpriteProfile.forFighter(f).shieldEnvelope(false) == initial, "Return retains evolution envelope");
        }
    }

    private static FightingGameSession probe(String species, int slot) throws Exception {
        var session = session(species);
        session.update(request(slot), DT);
        ticks(session, 65);
        session.update(request(0), DT);
        return session;
    }

    private static void key(JComponent panel, String stroke) {
        var id = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(stroke));
        check(id != null, "Missing binding " + stroke);
        panel.getActionMap().get(id).actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "check"));
    }

    private static void awaitEdt(java.util.concurrent.Callable<Boolean> condition, String message) throws Exception {
        long deadline = System.nanoTime() + 20_000_000_000L;
        while (System.nanoTime() < deadline) {
            var result = new java.util.concurrent.FutureTask<Boolean>(condition);
            SwingUtilities.invokeAndWait(result);
            if (result.get()) return;
            Thread.sleep(40);
        }
        throw new AssertionError(message);
    }

    private static void window() throws Exception {
        final Throwable[] failure = {null};
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { failure[0] = error; error.printStackTrace(); });
        MainGUI.main(new String[0]); SwingUtilities.invokeAndWait(() -> {});
        final GameWindow[] window = {null};
        final FightingGamePanel[] panel = {null};
        final FightingGameSession[] session = {null};
        final double[] frozen = {0};
        try {
            for (var status : STATUS) if (status.move.equals("GROWTH") || status.move.equals("GROWL"))
                for (boolean evolved : new boolean[]{false, true}) {
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            if (window[0] == null) {
                                for (Window candidate : Window.getWindows())
                                    if (candidate instanceof GameWindow game && game.isShowing()) window[0] = game;
                                check(window[0] != null, "MainGUI did not open");
                                var screens = (Map<?, ?>) field(window[0], "screens");
                                key((JComponent) screens.get("intro"), "pressed ENTER");
                                panel[0] = (FightingGamePanel) screens.get("fighting");
                            }
                            window[0].showScreen("menu");
                            var setup = (FreeFightSetup) field(panel[0], "setup");
                            var a = new Pokemon(1, status.base, 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
                            var b = new Pokemon(2, "SQUIRTLE", 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
                            setup.setPlayerOne(new Player(1, "J1", 0, 0), List.of(a, a, a));
                            setup.setPlayerTwo(new Player(2, "J2", 0, 0), List.of(b, b, b));
                            window[0].setGameSize(1280, 720); window[0].showScreen("fighting");
                            session[0] = (FightingGameSession) field(panel[0], "session");
                            contact(session[0], false);
                        } catch (Throwable e) { failure[0] = e; }
                    });
                    if (failure[0] == null) FightingWindowChecks.awaitGameplay(panel[0]);
                    if (evolved) SwingUtilities.invokeAndWait(() -> {
                        session[0].debugFillActiveEnergy(0); key(panel[0], "pressed R"); key(panel[0], "released R");
                    });
                    awaitEdt(() -> failure[0] != null || !evolved
                            || session[0].getFighters().get(0).isEvolved()
                            && !session[0].getFighters().get(0).isEvolutionAnimating(), "Live evolution failed");
                    if (failure[0] != null) throw new AssertionError(failure[0]);
                    SwingUtilities.invokeAndWait(() -> {
                        String button = status.slot == 3 ? "V" : "H";
                        key(panel[0], "pressed " + button); key(panel[0], "released " + button);
                    });
                    awaitEdt(() -> effect(session[0], status) > 0 && session[0].getCombatSystem()
                            .getAttackVisual(session[0].getFighters().get(0), session[0].getFighters()) == null,
                            "Live status still locks actions: " + status.base);
                    SwingUtilities.invokeAndWait(() -> {
                        contact(session[0], false); key(panel[0], "pressed F"); key(panel[0], "released F");
                    });
                    awaitEdt(() -> session[0].getFighters().get(1).getHealth() < 200, "Live Tackle did not hit");
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            var a = session[0].getFighters().get(0); var b = session[0].getFighters().get(1);
                            int damage = (int) Math.round(8 * (evolved ? 1.12 : 1) * (status.self ? 1.35 : 1));
                            check(b.getHealth() == 200 - damage, "Live modified damage incorrect");
                            check(effect(session[0], status) > 0, "Live effect ended before Tackle");
                            key(panel[0], "pressed ESCAPE"); frozen[0] = effect(session[0], status);
                            check(session[0].getState() == FightingGameSession.State.PAUSED, "Live pause failed");
                            System.out.println("OK MainGUI " + a.getPokemonId() + " " + status.move + " -> Tackle damage=" + damage);
                        } catch (Throwable e) { failure[0] = e; }
                    });
                    Thread.sleep(180);
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            near(effect(session[0], status), frozen[0], "Live pause advances effect");
                            key(panel[0], "pressed ESCAPE");
                        } catch (Throwable e) { failure[0] = e; }
                    });
                    if (failure[0] != null) throw new AssertionError(failure[0]);
                }
        } finally {
            SwingUtilities.invokeAndWait(() -> { for (Window candidate : Window.getWindows()) candidate.dispose(); });
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--window")) {
            try { window(); System.out.println("PASS live status: " + checks + " checks"); }
            catch (Throwable e) { e.printStackTrace(); System.exit(1); }
            System.exit(0);
        }
        if (args.length > 0 && args[0].equals("--probe")) {
            var growth = probe("BULBASAUR", 3);
            var growl = probe("EEVEE", 2);
            var powder = probe("BULBASAUR", 2);
            System.out.println("Status probes complete");
            return;
        }
        statusChecks(); otherActions(); bufferedFollowup(); shieldChecks();
        System.out.println("PASS status/shields: " + checks + " checks");
    }
}
