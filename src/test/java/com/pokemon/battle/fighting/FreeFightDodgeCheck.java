package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.util.List;

/** Checks the quick dodge: shield + direction, pass-through, just-in-time dodge and cooldown. Run with main. */
public class FreeFightDodgeCheck {

    public static void main(String[] args) {
        // Escudo + tocar derecha: esquiva unos 140 px y atraviesa al rival
        FightingGameSession s = session(300, 360);
        Fighter me = s.getFighters().get(0);
        FightingInput in = new FightingInput();
        in.setPlayerOneBlock(true);
        step(s, in, 5);
        double start = me.getX();
        in.setPlayerOneRight(true);
        step(s, in, 1);
        check(me.isDodging(), "Shield + direction must start the dodge");
        step(s, in, 20);
        double moved = me.getX() - start;
        check(moved > 120 && moved < 170, "Dodge must move about 140 px: " + moved);
        check(me.getX() > s.getFighters().get(1).getX(), "Dodge must pass through the rival");
        check(me.isBlocking(), "Still holding the shield after the dodge keeps blocking");

        // Mantener la direccion y luego poner el escudo NO esquiva
        FightingGameSession held = session(300, 700);
        Fighter walker = held.getFighters().get(0);
        FightingInput hin = new FightingInput();
        hin.setPlayerOneRight(true);
        step(held, hin, 10);
        hin.setPlayerOneBlock(true);
        step(held, hin, 10);
        check(!walker.isDodging(), "Holding the direction before the shield must not dodge");

        // Espera entre esquivas
        FightingGameSession again = session(300, 900);
        Fighter quick = again.getFighters().get(0);
        FightingInput ain = new FightingInput();
        ain.setPlayerOneBlock(true);
        step(again, ain, 3);
        ain.setPlayerOneRight(true); step(again, ain, 14);
        ain.setPlayerOneRight(false); step(again, ain, 2);
        ain.setPlayerOneLeft(true); step(again, ain, 1);
        check(!quick.isDodging(), "A second dodge needs the 0.8 s wait");

        // Justo a tiempo: el Placaje del rival falla
        check(dodgedTackle(4) == 0, "Dodging just as the Placaje hits must avoid it");
        // Muy tarde (ya pego): recibe el golpe
        check(dodgedTackle(30) > 0, "Dodging too late must not avoid the hit");

        // Una bola: esquivar hacia ella justo a tiempo la atraviesa; muy antes, pega
        check(dodgeBall(125) == 0, "Dodging into the ball just in time must pass through it");
        check(dodgeBall(330) > 0, "Dodging too early must not avoid the ball");
        System.out.println("PASS dodge");
    }

    /** Squirtle (rival) pega Placaje; Charmander, cubierto, esquiva a la derecha N cuadros despues. */
    private static int dodgedTackle(int framesAfterAttack) {
        FightingGameSession s = session(520, 590);
        Fighter me = s.getFighters().get(0);
        Fighter rival = s.getFighters().get(1);
        rival.setFacingLeft(true);
        FightingInput in = new FightingInput();
        in.setPlayerOneBlock(false);
        step(s, in, 3);
        int before = me.getHealth();
        in.setPlayerTwoAttackOne(true);
        step(s, in, 1);
        in.setPlayerTwoAttackOne(false);
        step(s, in, framesAfterAttack);
        in.setPlayerOneBlock(true);
        in.setPlayerOneRight(true);
        step(s, in, 40);
        return before - me.getHealth();
    }

    /** Squirtle dispara Pistola Agua; Charmander esquiva hacia la bola cuando esta a esa distancia. */
    private static int dodgeBall(double distance) {
        FightingGameSession s = session(300, 1000);
        Fighter me = s.getFighters().get(0);
        FightingInput in = new FightingInput();
        int before = me.getHealth();
        in.setPlayerTwoAttackTwo(true);
        step(s, in, 1);
        in.setPlayerTwoAttackTwo(false);
        boolean done = false;
        for (int i = 0; i < 240; i++) {
            if (!done) {
                for (FightingProjectile p : s.getCombatSystem().getProjectiles()) {
                    if (p.getOwnerIndex() == 1 && Math.abs(p.getX() - me.getX()) < distance) {
                        done = true;
                        in.setPlayerOneBlock(true);
                        step(s, in, 1);
                        in.setPlayerOneRight(true);
                    }
                }
            }
            step(s, in, 1);
            if (done && !me.isDodging()) { in.setPlayerOneRight(false); in.setPlayerOneBlock(false); }
        }
        return before - me.getHealth();
    }

    private static void step(FightingGameSession s, FightingInput in, int frames) {
        for (int i = 0; i < frames; i++) s.update(in, 1 / 60.0);
    }

    private static FightingGameSession session(double x, double rivalX) {
        FreeFightSetup setup = new FreeFightSetup();
        setup.setPlayerOne(new Player(1, "A", 0, 0), List.of(p("CHARMANDER", Type.FIRE), p("EEVEE", Type.NORMAL)));
        setup.setPlayerTwo(new Player(2, "B", 0, 0), List.of(p("SQUIRTLE", Type.WATER), p("BULBASAUR", Type.GRASS)));
        FightingGameSession s = new FightingGameSession(setup);
        Fighter me = s.getFighters().get(0);
        Fighter rival = s.getFighters().get(1);
        me.setX(x);
        me.setFacingLeft(rivalX < x);
        rival.setX(rivalX);
        rival.setFacingLeft(rivalX > x);
        return s;
    }

    private static Pokemon p(String name, Type type) {
        return new Pokemon(1, name, 50, 50, 50, 50, 50, 50, type, Type.NONE, List.of());
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
