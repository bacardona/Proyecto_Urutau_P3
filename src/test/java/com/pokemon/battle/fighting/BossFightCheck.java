package com.pokemon.battle.fighting;

/** Pruebas del jefe secreto Mewtwo (sin base de datos ni ventana). */
public final class BossFightCheck {

    private static final double DT = 1 / 60.0;

    public static void main(String[] args) {
        // Equipo de prueba del retador; Mewtwo va del lado contrario
        FightingGameSession session = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, false, FightingStage.TEMPLO);
        check(session.isBossFight() && session.getBossIndex() == 1, "Mewtwo goes on the right side");
        Fighter boss = session.getBoss();
        check("MEWTWO".equals(boss.getPokemonId()) && boss.isBoss(), "The boss is Mewtwo");
        check(session.getPlayerTwoTeam().getMembers().size() == 1, "Boss team has only Mewtwo");
        check(boss.getIncomingDamageMultiplier() == Fighter.BOSS_DAMAGE_TAKEN, "Boss takes half damage");

        // Con el retador quieto, Mewtwo tiene que atacar, dispararle y ganar
        FightingInput idle = new FightingInput();
        int shots = 0;
        double lastBlinkAt = -1;
        int blinks = 0;
        for (int i = 0; i < 60 * 180 && session.getState() == FightingGameSession.State.RUNNING; i++) {
            session.update(idle, DT);
            for (FightingProjectile p : session.getProjectiles()) {
                if (p.getOwnerIndex() == 1) shots++;
            }
            if (!Double.isNaN(session.consumeBossBlink())) blinks++;
            session.getCombatSystem().drainEvents();
        }
        check(shots > 0, "Mewtwo shoots projectiles");
        check(session.getState() == FightingGameSession.State.FINISHED, "Fight ends");
        check(!session.challengerWon(), "An idle challenger loses against Mewtwo");

        // Fase 2: a la mitad de vida se enfurece una sola vez
        FightingGameSession second = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, true, FightingStage.TEMPLO);
        check(second.getBossIndex() == 0, "If player 2 won, Mewtwo takes the left side");
        Fighter mewtwo = second.getBoss();
        mewtwo.receiveDamage(mewtwo.getMaxHealth() / 2 + 1);
        second.update(new FightingInput(), DT);
        check(mewtwo.isEnraged(), "Boss enrages at half health");
        check(second.consumeBossEnraged() && !second.consumeBossEnraged(), "Enrage is reported once");
        check(mewtwo.getMovementSpeedMultiplier() > 1.0, "Enraged boss is faster");

        // Furioso gana energia solo y termina usando su Psicoataque
        boolean ultimate = false;
        for (int i = 0; i < 60 * 30 && !ultimate; i++) {
            second.update(new FightingInput(), DT);
            ultimate = !second.getCombatSystem().getUltimateVisuals().isEmpty();
            second.getCombatSystem().drainEvents();
        }
        check(ultimate, "Enraged Mewtwo uses Psystrike");

        // Teletransporte
        FightingGameSession third = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, false, FightingStage.TEMPLO);
        double from = third.getBoss().getX();
        check(third.bossBlink(400), "Blink works");
        check(third.getBoss().getX() == 400 && third.consumeBossBlink() == from, "Blink moves Mewtwo and reports where it was");

        // Un retador que pega sin parar no puede dejar a Mewtwo atrapado en su escudo
        FightingGameSession hammer = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"CHARMANDER", "SQUIRTLE"}, false, FightingStage.TEMPLO);
        FightingInput keys = new FightingInput();
        for (int i = 0; i < 60 * 150 && hammer.getState() == FightingGameSession.State.RUNNING; i++) {
            Fighter me = hammer.getFighters().get(0), them = hammer.getFighters().get(1);
            boolean far = Math.abs(them.getX() - me.getX()) > 120;
            keys.setPlayerOneRight(far && them.getX() > me.getX());
            keys.setPlayerOneLeft(far && them.getX() < me.getX());
            keys.setPlayerOneAttackOne(!far && i % 18 == 0);
            hammer.update(keys, DT);
            hammer.getCombatSystem().drainEvents();
            hammer.consumeBossBlink();
        }
        check(hammer.getState() == FightingGameSession.State.FINISHED, "A fight against a hammering challenger ends");

        System.out.println("PASS boss fight (shots=" + shots + ", blinks=" + blinks + ")");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
