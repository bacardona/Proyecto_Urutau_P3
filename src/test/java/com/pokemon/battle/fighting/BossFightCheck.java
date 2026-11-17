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

        hardMode();

        System.out.println("PASS boss fight (shots=" + shots + ", blinks=" + blinks + ")");
    }

    /** Revancha dificil: tecnicas nuevas y Mega Mewtwo Y. */
    private static void hardMode() {
        FightingGameSession hard = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, false, FightingStage.TEMPLO, true);
        Fighter mewtwo = hard.getBoss();
        Fighter foe = hard.getFighters().get(0);
        check(hard.isHardBoss(), "Hard boss session");
        check(mewtwo.getIncomingDamageMultiplier() == Fighter.HARD_BOSS_DAMAGE_TAKEN, "Hard boss takes 50%");

        // Triple Bola Sombra: tres proyectiles a tres alturas
        check(hard.bossTripleShadowBall(), "Volley starts");
        java.util.Set<Integer> heights = new java.util.HashSet<>();
        for (int i = 0; i < 50; i++) {
            hard.update(new FightingInput(), DT);
            for (FightingProjectile p : hard.getProjectiles())
                if (p.getOwnerIndex() == 1) heights.add((int) Math.round(mewtwo.getY() - p.getY()));
        }
        check(heights.size() == 3, "Three shadow balls at three heights: " + heights);

        // Barrera: un proyectil del rival vuelve hacia el
        FightingGameSession barrier = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"CHARMANDER", "SQUIRTLE"}, false, FightingStage.TEMPLO, true);
        Fighter shooter = barrier.getFighters().get(0);
        Fighter wall = barrier.getBoss();
        shooter.setX(700);
        wall.setX(1000);
        check(barrier.bossBarrier(3), "Barrier rises");
        FightingInput ember = new FightingInput();
        ember.setPlayerOneAttackTwo(true);
        boolean reflected = false;
        for (int i = 0; i < 90 && !reflected; i++) {
            barrier.update(ember, DT);
            ember.setPlayerOneAttackTwo(false);
            for (FightingProjectile p : barrier.getProjectiles()) if (p.getOwnerIndex() == 1) reflected = true;
        }
        check(reflected && wall.getHealth() == wall.getMaxHealth(), "Barrier reflects the projectile");

        // Telequinesis: jala al rival hacia Mewtwo
        FightingGameSession pull = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, false, FightingStage.TEMPLO, true);
        Fighter pulled = pull.getFighters().get(0);
        double before = pull.getBoss().getX() - pulled.getX();
        check(pull.bossTelekinesis(), "Telekinesis works");
        for (int i = 0; i < 30; i++) pull.update(new FightingInput(), DT);
        check(pull.getBoss().getX() - pulled.getX() < before - 100, "Telekinesis pulls the rival closer");

        // Premonicion: pega si te quedas, no si te mueves
        FightingGameSession sight = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, false, FightingStage.TEMPLO, true);
        Fighter still = sight.getFighters().get(0);
        still.setX(500);
        sight.getBoss().setX(1100);
        sight.bossFutureSight();
        int healthBefore = still.getHealth();
        for (int i = 0; i < 90; i++) { still.setX(500); sight.update(new FightingInput(), DT); }
        check(still.getHealth() < healthBefore, "Future sight hits a rival that stays");
        FightingGameSession dodge = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, false, FightingStage.TEMPLO, true);
        Fighter mover = dodge.getFighters().get(0);
        mover.setX(500);
        dodge.getBoss().setX(1100);
        dodge.bossFutureSight();
        int moverHealth = mover.getHealth();
        for (int i = 0; i < 90; i++) { mover.setX(i < 20 ? 500 : 300); dodge.update(new FightingInput(), DT); }
        check(mover.getHealth() == moverHealth, "Moving away dodges future sight");

        // Mega al 20 %
        FightingGameSession phases = FightingGameSession.bossFightPrototype(
                "ASH", new String[]{"PIKACHU", "BULBASAUR"}, false, FightingStage.TEMPLO, true);
        Fighter boss = phases.getBoss();
        boss.receiveDamage(boss.getMaxHealth() / 2 + 1);
        phases.update(new FightingInput(), DT);
        check(boss.isEnraged(), "Hard boss enrages");
        boss.receiveDamage(boss.getHealth() - 49);          // 49: todavia no es Mega
        phases.update(new FightingInput(), DT);
        check(boss.getHealth() == 49 && !boss.isMegaPending(), "No heal and no Mega above 20%");
        boss.receiveDamage(boss.getHealth() - 39);          // 39 (menos del 20 %)
        phases.update(new FightingInput(), DT);
        check(boss.isMegaPending() && !boss.isMega(), "Mega transformation starts at 20%");
        for (int i = 0; i < 60 * 7; i++) phases.update(new FightingInput(), DT);
        check(boss.isMega() && Fighter.MEGA_FORM_ID.equals(boss.getPokemonId()), "Becomes Mega Mewtwo Y");
        check(UltimateDefinition.forEvolvedForm(Fighter.MEGA_FORM_ID) != null, "Mega has its Psystrike");
        check(FightingSpriteProfile.forSpecies(Fighter.MEGA_FORM_ID) != null, "Mega sprites exist");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
