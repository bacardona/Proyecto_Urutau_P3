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
