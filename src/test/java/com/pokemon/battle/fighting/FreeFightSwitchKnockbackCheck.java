package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.util.List;

/** Checks the hit knockback and switching Pokemon against the wall. Run with main. */
public class FreeFightSwitchKnockbackCheck {

    public static void main(String[] args) {
        // Retroceso: un golpe fisico empuja mas que un proyectil, y el golpe fuerte mas que el normal
        check(FightingCombatSystem.knockbackFor(AttackDefinition.Kind.DASH, 8)
                > FightingCombatSystem.knockbackFor(AttackDefinition.Kind.PROJECTILE, 8), "Dash pushes more");
        check(FightingCombatSystem.knockbackFor(AttackDefinition.Kind.HITBOX, 13)
                > FightingCombatSystem.knockbackFor(AttackDefinition.Kind.HITBOX, 8), "Strong hit pushes more");
        check(FightingCombatSystem.knockbackFor(AttackDefinition.Kind.GRUNT, 0) == 0, "Growl does not push");

        FightingGameSession session = new FightingGameSession(setup());
        FightingInput input = new FightingInput();
        Fighter target = session.getFighters().get(1);
        double before = target.getX();
        target.applyKnockback(-1, 400);
        for (int i = 0; i < 60; i++) session.update(input, 1 / 60.0);
        check(target.getX() < before - 20, "Knockback must move the target back");
        check(target.getKnockbackVelocity() == 0, "Knockback must stop by itself");

        // Cambio: lejos de la pared no se puede
        Fighter first = session.getFighters().get(0);
        first.setX(640);
        check(!session.trySwitchActiveFighter(0), "Switch away from the wall must fail");
        // Contra la pared si, y cada uno conserva su vida
        first.receiveDamage(30);
        first.setX(PhysicsSystem.halfWidthFor(first));
        check(session.trySwitchActiveFighter(0), "Switch at the wall must work");
        Fighter second = session.getFighters().get(0);
        check(second != first && second.getHealth() == 200 && first.getHealth() == 170,
                "Each Pokemon keeps its own health");
        // Enfriamiento: no se puede volver a cambiar enseguida
        second.setX(PhysicsSystem.halfWidthFor(second));
        check(!session.trySwitchActiveFighter(0), "Switch must have a cooldown");
        for (int i = 0; i < 260; i++) {
            second.setX(PhysicsSystem.halfWidthFor(second));
            session.update(input, 1 / 60.0);
        }
        check(session.trySwitchActiveFighter(0), "Switch works again after the cooldown");
        check(session.getFighters().get(0) == first, "The first Pokemon comes back with its health");
        // Entrada: el que entra sale saltando hacia el centro y al caer empuja al rival cercano
        Fighter back = session.getFighters().get(0);
        check(!back.isOnGround() && back.getVelocityY() < 0, "Incoming Pokemon enters with a jump");
        Fighter rival = session.getFighters().get(1);
        rival.setX(back.getX() + 90);
        boolean landed = false;
        double rivalBefore = rival.getX();
        for (int i = 0; i < 120 && !landed; i++) {
            session.update(input, 1 / 60.0);
            landed = session.consumeEntranceLanding(0);
        }
        check(landed, "Entrance landing must be reported once");
        check(!session.consumeEntranceLanding(0), "Entrance landing is reported only once");
        for (int i = 0; i < 30; i++) session.update(input, 1 / 60.0);
        check(rival.getX() > rivalBefore, "Landing pushes the nearby rival");

        // Cambio en el aire pegado a la pared
        FightingGameSession air = new FightingGameSession(setup());
        Fighter jumper = air.getFighters().get(0);
        jumper.setX(PhysicsSystem.halfWidthFor(jumper));
        jumper.setOnGround(false);
        jumper.setY(air.getGroundY() - 120);
        check(air.trySwitchActiveFighter(0), "Switch in the air at the wall must work");
        check(air.getFighters().get(0).getY() < air.getGroundY() - 100, "Incoming keeps the height of the jump");
        // Aunque sigan apretando hacia la pared, el que entra mira al centro toda la entrada
        FightingInput holdLeft = new FightingInput();
        holdLeft.setPlayerOneLeft(true);
        Fighter entering = air.getFighters().get(0);
        for (int i = 0; i < 90 && !entering.isOnGround(); i++) {
            air.update(holdLeft, 1 / 60.0);
            check(!entering.isFacingLeft(), "Incoming must face the center while entering");
        }
        check(entering.isOnGround() && !entering.isFacingLeft(), "Incoming lands facing the center");

        // Combos: del tercer golpe en adelante pegan mas
        check(FightingCombatSystem.comboDamageMultiplier(2) == 1.0
                && FightingCombatSystem.comboDamageMultiplier(3) > 1.0
                && FightingCombatSystem.comboDamageMultiplier(5) > FightingCombatSystem.comboDamageMultiplier(3),
                "Combo damage bonus");

        // Parry: apretar el escudo seguido (spam) no da parry
        Fighter guard = new FightingGameSession(setup()).getFighters().get(1);
        guard.setBlocking(true);
        check(guard.isInParryWindow(), "Fresh block opens the parry window");
        guard.updateStatusEffects(0.2);
        check(!guard.isInParryWindow(), "Parry window closes");
        guard.setBlocking(false);
        guard.setBlocking(true);
        check(!guard.isInParryWindow(), "Spamming the block gives no parry");
        guard.setBlocking(false);
        guard.updateStatusEffects(0.5);
        guard.setBlocking(true);
        check(guard.isInParryWindow(), "Parry works again after waiting");
        // K.O. del segundo despues de un cambio: debe volver el primero (antes se quedaba trabado)
        FightingGameSession ko = new FightingGameSession(setup());
        Fighter starter = ko.getFighters().get(0);
        starter.setX(PhysicsSystem.halfWidthFor(starter));
        check(ko.trySwitchActiveFighter(0), "Switch before the KO");
        Fighter switched = ko.getFighters().get(0);
        check(ko.debugDefeatActiveFighter(0), "KO the switched Pokemon");
        for (int i = 0; i < 240 && ko.getFighters().get(0) != starter; i++) ko.update(new FightingInput(), 1 / 60.0);
        check(switched.isDefeated() && ko.getFighters().get(0) == starter && !starter.isDefeated(),
                "After a KO the first Pokemon must come back");
        check(ko.getState() == FightingGameSession.State.RUNNING, "The match goes on while a Pokemon is alive");
                // Onda Trueno (RB de Pikachu): tiene espera para repetirla, pero no deja congelado al que la usa
        FreeFightSetup pikaSetup = new FreeFightSetup();
        pikaSetup.setPlayerOne(new Player(1, "A", 0, 0), List.of(p("PIKACHU", Type.ELECTRIC), p("CHARMANDER", Type.FIRE)));
        pikaSetup.setPlayerTwo(new Player(2, "B", 0, 0), List.of(p("SQUIRTLE", Type.WATER), p("EEVEE", Type.NORMAL)));
        FightingGameSession pika = new FightingGameSession(pikaSetup);
        FightingInput wave = new FightingInput();
        wave.setPlayerOneAttackFour(true);
        pika.update(wave, 1 / 60.0);
        wave.setPlayerOneAttackFour(false);
        Fighter pikachu = pika.getFighters().get(0);
        check(pika.getCombatSystem().getAttackVisual(pikachu, pika.getFighters()) != null, "Thunder Wave starts");
        for (int i = 0; i < 72; i++) pika.update(wave, 1 / 60.0);   // 1.2 s
        check(pika.getCombatSystem().getAttackVisual(pikachu, pika.getFighters()) == null,
                "Thunder Wave must not freeze Pikachu for seconds");
        // Vulpix: su primer ataque es Ataque Rapido y avanza como el de Eevee
        FreeFightSetup vulpixSetup = new FreeFightSetup();
        vulpixSetup.setPlayerOne(new Player(1, "A", 0, 0), List.of(p("VULPIX", Type.FIRE), p("CHARMANDER", Type.FIRE)));
        vulpixSetup.setPlayerTwo(new Player(2, "B", 0, 0), List.of(p("SQUIRTLE", Type.WATER), p("EEVEE", Type.NORMAL)));
        FightingGameSession fox = new FightingGameSession(vulpixSetup);
        Fighter vulpix = fox.getFighters().get(0);
        double foxStart = vulpix.getX();
        FightingInput quick = new FightingInput();
        quick.setPlayerOneAttackOne(true);
        fox.update(quick, 1 / 60.0);
        quick.setPlayerOneAttackOne(false);
        var foxVisual = fox.getCombatSystem().getAttackVisual(vulpix, fox.getFighters());
        check(foxVisual != null && "QUICK_ATTACK".equals(foxVisual.definition().moveId()),
                "Vulpix attack one must be Quick Attack");
        for (int i = 0; i < 30; i++) fox.update(quick, 1 / 60.0);
        check(Math.abs(vulpix.getX() - foxStart) > 80, "Vulpix Quick Attack must dash forward: "
                + Math.abs(vulpix.getX() - foxStart));
        // Placaje da un paso corto hacia adelante, aunque el rival este lejos
        FightingGameSession step = new FightingGameSession(setup());
        Fighter charm = step.getFighters().get(0);
        Fighter far = step.getFighters().get(1);
        charm.setFacingLeft(false);
        charm.setX(300);
        far.setX(1000);
        FightingInput tackle = new FightingInput();
        tackle.setPlayerOneAttackOne(true);
        step.update(tackle, 1 / 60.0);
        tackle.setPlayerOneAttackOne(false);
        for (int i = 0; i < 40; i++) step.update(tackle, 1 / 60.0);
        double stepped = charm.getX() - 300;
        check(stepped > 30 && stepped <= 45.5, "Placaje must take a short step forward: " + stepped);

        // De cerca, el paso se detiene al tocar al rival: pega y no lo atraviesa
        FightingGameSession close = new FightingGameSession(setup());
        Fighter c1 = close.getFighters().get(0);
        Fighter c2 = close.getFighters().get(1);
        c1.setFacingLeft(false);
        c1.setX(500);
        c2.setX(500 + PhysicsSystem.halfWidthFor(c1) + PhysicsSystem.halfWidthFor(c2) + 50);
        int c2Before = c2.getHealth();
        tackle.setPlayerOneAttackOne(true);
        close.update(tackle, 1 / 60.0);
        tackle.setPlayerOneAttackOne(false);
        for (int i = 0; i < 40; i++) close.update(tackle, 1 / 60.0);
        check(c2.getHealth() < c2Before, "The short step lets Placaje reach a rival just out of range");
        check(c1.getX() < c2.getX(), "Placaje must not pass through the rival");

        // Golpe Cuerpo (Machop) sale hacia adelante aunque el rival este detras
        FreeFightSetup machopSetup = new FreeFightSetup();
        machopSetup.setPlayerOne(new Player(1, "A", 0, 0), List.of(p("MACHOP", Type.FIGHTING), p("CHARMANDER", Type.FIRE)));
        machopSetup.setPlayerTwo(new Player(2, "B", 0, 0), List.of(p("SQUIRTLE", Type.WATER), p("EEVEE", Type.NORMAL)));
        FightingGameSession slam = new FightingGameSession(machopSetup);
        Fighter machop = slam.getFighters().get(0);
        Fighter behind = slam.getFighters().get(1);
        machop.setX(600);
        behind.setX(300);
        machop.setFacingLeft(false);
        FightingInput body = new FightingInput();
        body.setPlayerOneAttackFour(true);
        slam.update(body, 1 / 60.0);
        body.setPlayerOneAttackFour(false);
        for (int i = 0; i < 40; i++) slam.update(body, 1 / 60.0);
        check(machop.getX() - 600 > 50, "Body Slam must dash forward even with the rival behind: " + (machop.getX() - 600));
        System.out.println("PASS switch/knockback");
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
