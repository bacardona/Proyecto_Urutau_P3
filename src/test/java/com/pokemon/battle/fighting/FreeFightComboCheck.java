package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.util.List;

/** Checks the double hit of attack 1, the tail swipe and the dive from the air. Run with main. */
public class FreeFightComboCheck {

    public static void main(String[] args) {
        // Un toque: un solo golpe
        int single = closeHit("CHARMANDER", Type.FIRE, new int[]{30});
        // Dos toques seguidos: dos golpes y mas dano
        int twice = closeHit("CHARMANDER", Type.FIRE, new int[]{30, 40});
        check(single > 0, "One tap must hit");
        check(twice > single + 6, "Two taps must hit twice: " + twice + " vs " + single);

        // El segundo golpe es otro ataque (TACKLE_2) y no hay tercero
        FightingGameSession s = session("MACHOP", Type.FIGHTING, 520, 600);
        Fighter machop = s.getFighters().get(0);
        FightingInput in = new FightingInput();
        String seen = null;
        for (int i = 0; i < 90; i++) {
            in.setPlayerOneAttackOne(i == 5 || i == 15 || i == 25);
            s.update(in, 1 / 60.0);
            in.setPlayerOneAttackOne(false);
            var visual = s.getCombatSystem().getAttackVisual(machop, s.getFighters());
            if (visual != null && i > 15 && i < 30) seen = visual.definition().moveId();
        }
        check("TACKLE_2".equals(seen), "Second tap must start the second hit, saw " + seen);

        // Los golpes fuertes de otros botones no se encadenan (A Bocajarro de Machop)
        int heavy = closeHitSlot("MACHOP", Type.FIGHTING, 2, new int[]{30, 40});
        int heavyOnce = closeHitSlot("MACHOP", Type.FIGHTING, 2, new int[]{30});
        check(heavy == heavyOnce, "Heavy hits stay single: " + heavy + " vs " + heavyOnce);

        // Pikachu: el coletazo sale al final del Ataque Rapido y es flojo
        int pikaOnce = closeHit("PIKACHU", Type.ELECTRIC, new int[]{30});
        int pikaTwice = closeHit("PIKACHU", Type.ELECTRIC, new int[]{30, 36});
        check(pikaTwice > pikaOnce, "Pikachu tail swipe must hit: " + pikaTwice + " vs " + pikaOnce);
        check(pikaTwice - pikaOnce <= 7, "Pikachu tail swipe must be weak: " + (pikaTwice - pikaOnce));
        int vulpixTwice = closeHit("VULPIX", Type.FIRE, new int[]{30, 36});
        int vulpixOnce = closeHit("VULPIX", Type.FIRE, new int[]{30});
        check(vulpixTwice > vulpixOnce, "Vulpix tail swipe must hit");

        // Picada: golpe cuerpo a cuerpo en el aire baja en diagonal y pega al rival de abajo
        FightingGameSession air = session("CHARMANDER", Type.FIRE, 380, 640);
        Fighter charm = air.getFighters().get(0);
        Fighter squirtle = air.getFighters().get(1);
        FightingInput jump = new FightingInput();
        int before = squirtle.getHealth();
        boolean dived = false;
        for (int i = 0; i < 120; i++) {
            jump.setPlayerOneJump(i >= 0 && i < 25);
            jump.setPlayerOneRight(i >= 0 && i < 30);
            jump.setPlayerOneAttackOne(i == 24);
            air.update(jump, 1 / 60.0);
            jump.setPlayerOneAttackOne(false);
            dived |= charm.isDiving();
        }
        check(dived, "Attacking in the air must dive");
        check(squirtle.getHealth() < before, "The dive must hit the rival below");
        check(!charm.isDiving(), "The dive must end");

        // Los sprites dibujados del otro brazo cargan
        check(new SpriteCollabAnimationSet("MACHOP", "Sprites_nuevos").get("Punch2") != null, "Machop Punch2");
        check(new SpriteCollabAnimationSet("CHARMANDER", "Sprites_nuevos").get("Punch2") != null, "Charmander Punch2");
        check(new SpriteCollabAnimationSet("CHARMANDER", "Evolucion").get("Punch2") != null, "Charmeleon Punch2");
        System.out.println("PASS combo");
    }

    private static int closeHit(String species, Type type, int[] taps) {
        return closeHitSlot(species, type, 1, taps);
    }

    /** Dano hecho a un Squirtle quieto al tocar el ataque del boton dado en esos cuadros. */
    private static int closeHitSlot(String species, Type type, int slot, int[] taps) {
        FightingGameSession s = session(species, type, 520, 600);
        Fighter rival = s.getFighters().get(1);
        FightingInput in = new FightingInput();
        int before = rival.getHealth();
        for (int i = 0; i < 150; i++) {
            boolean press = false;
            for (int t : taps) press |= i == t;
            if (slot == 1) in.setPlayerOneAttackOne(press);
            else in.setPlayerOneAttackTwo(press);
            s.update(in, 1 / 60.0);
            in.setPlayerOneAttackOne(false);
            in.setPlayerOneAttackTwo(false);
        }
        return before - rival.getHealth();
    }

    private static FightingGameSession session(String species, Type type, double x, double rivalX) {
        FreeFightSetup setup = new FreeFightSetup();
        setup.setPlayerOne(new Player(1, "A", 0, 0), List.of(p(species, type), p("EEVEE", Type.NORMAL)));
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
