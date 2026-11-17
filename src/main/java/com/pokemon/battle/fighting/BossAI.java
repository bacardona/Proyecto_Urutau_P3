package com.pokemon.battle.fighting;

import java.util.List;
import java.util.Random;

/**
 * Cerebro de Mewtwo, el jefe secreto. Cada pocos instantes "piensa" que hacer y aprieta
 * los mismos botones que un jugador (FightingInput), asi usa las reglas normales del combate.
 *
 * Fase 1 (mas de la mitad de vida): pelea de lejos con Psiquico y Bola Sombra; si lo
 * acorralan se teletransporta. Fase 2 (furioso): mas rapido, se teletransporta detras del
 * rival para atacar de cerca y carga solo su Psicoataque.
 */
final class BossAI {

    // Distancias (en pixeles) que Mewtwo prefiere
    static final double CLOSE_RANGE = 150;
    static final double FAR_RANGE = 560;
    // Cada cuanto decide algo nuevo: le da un tiempo de reaccion "humano"
    static final double THINK_SECONDS = 0.20;
    static final double THINK_SECONDS_ENRAGED = 0.13;
    // Energia de ultimate que gana por segundo cuando esta furioso
    static final double ENRAGED_ENERGY_PER_SECOND = 6;
    // Fase 1: espera minima entre disparos
    static final double CALM_SHOT_SECONDS = 0.80;
    // Espera minima entre Psicoataques (si lo interrumpen no lo repite enseguida)
    static final double ULTIMATE_COOLDOWN_SECONDS = 14;

    private final int me;
    private final Random random = new Random(150);
    private double think = 0.6;
    private double shotCooldown = 1.0;
    private double blinkCooldown = 3.0;
    private double meleeCooldown;
    private double ultimateCooldown;
    private double blockSeconds;
    // Cuanto lleva cubriendose seguido: si es mucho, contraataca en vez de seguir aguantando
    private double guardedSeconds;
    private double walkSeconds;
    private double walkDirection;
    private int attackToPress = -1;
    private boolean jumpToPress;
    private boolean ultimateToPress;

    BossAI(int bossIndex) {
        this.me = bossIndex;
    }

    /** Llamado al comienzo de cada paso de la simulacion. */
    void drive(FightingGameSession session, FightingInput input, double dt) {
        List<Fighter> fighters = session.getFighters();
        Fighter boss = fighters.get(me);
        Fighter foe = fighters.get(1 - me);
        ignoreHumanKeys(input);
        shotCooldown -= dt;
        blinkCooldown -= dt;
        meleeCooldown -= dt;
        ultimateCooldown -= dt;
        if (blockSeconds > 0) guardedSeconds += dt;
        else guardedSeconds = Math.max(0, guardedSeconds - dt * 0.5);
        blockSeconds = Math.max(0, blockSeconds - dt);
        walkSeconds = Math.max(0, walkSeconds - dt);
        if (boss.isDefeated() || foe.isDefeated()) {
            press(input, 0, false, -1, false, false);
            return;
        }
        if (boss.isEnraged()) boss.addEvolutionEnergy(ENRAGED_ENERGY_PER_SECOND * dt);

        think -= dt;
        if (think <= 0) {
            think = boss.isEnraged() ? THINK_SECONDS_ENRAGED : THINK_SECONDS;
            decide(session, boss, foe);
        }

        if (attackToPress >= 0 || ultimateToPress) {
            // Antes de atacar se voltea hacia el rival (pudo estar caminando hacia atras)
            walkSeconds = 0;
            boss.setFacingLeft(foe.getX() < boss.getX());
        }
        double direction = walkSeconds > 0 && blockSeconds == 0 ? walkDirection : 0;
        press(input, direction, blockSeconds > 0, attackToPress, jumpToPress, ultimateToPress);
        attackToPress = -1;
        jumpToPress = false;
        ultimateToPress = false;
    }

    private void decide(FightingGameSession session, Fighter boss, Fighter foe) {
        FightingCombatSystem combat = session.getCombatSystem();
        boolean busy = combat.getAttackVisual(boss, session.getFighters()) != null
                || boss.isHitStunned() || boss.isAsleep() || !boss.isOnGround();
        double dx = foe.getX() - boss.getX();
        double distance = Math.abs(dx);
        double towardFoe = Math.signum(dx);
        boolean foeAttacking = combat.getAttackVisual(foe, session.getFighters()) != null;
        boolean enraged = boss.isEnraged();
        if (busy) return;

        // 1. Esquivar: si le viene un proyectil cerca, salta o se cubre
        if (incomingProjectile(session, boss)) {
            if (random.nextDouble() < (enraged ? 0.75 : 0.55)) jumpToPress = true;
            else blockSeconds = 0.35;
            return;
        }

        // 2. Ultimate (solo furioso, cuando la barra esta llena)
        if (enraged && boss.isUltimateReady() && ultimateCooldown <= 0 && !foe.isBlocking()) {
            ultimateToPress = true;
            ultimateCooldown = ULTIMATE_COOLDOWN_SECONDS;
            return;
        }

        // 3. Lo atacan de cerca: escudo o teletransporte; si ya aguanto mucho, contraataca
        if (foeAttacking && distance < CLOSE_RANGE + 40 && guardedSeconds > 1.0) {
            guardedSeconds = 0;
            blockSeconds = 0;                              // suelta el escudo para poder atacar
            if (blinkCooldown <= 0) blink(session, boss, foe, enraged);
            else attackToPress = 0;                        // Psicocorte de respuesta
            return;
        }
        if (foeAttacking && distance < CLOSE_RANGE + 40) {
            if (blinkCooldown <= 0 && random.nextDouble() < (enraged ? 0.55 : 0.4)) {
                blink(session, boss, foe, enraged);
            } else if (random.nextDouble() < (enraged ? 0.6 : 0.85)) {
                blockSeconds = 0.45;                       // tranquilo se cubre mas seguido
            }
            return;
        }

        if (enraged) decideEnraged(session, boss, foe, distance, towardFoe);
        else decideCalm(session, boss, foe, distance, towardFoe);
    }

    /** Fase 1: mantiene la distancia y dispara. */
    private void decideCalm(FightingGameSession session, Fighter boss, Fighter foe,
                            double distance, double towardFoe) {
        // Si el rival se le acerca, a veces se pone en guardia antes de que ataque
        if (distance < 260 && random.nextDouble() < 0.10) {
            blockSeconds = 0.35;
            return;
        }
        if (distance < CLOSE_RANGE) {
            if (meleeCooldown <= 0 && random.nextDouble() < 0.55) {
                attackToPress = 0;                         // Psicocorte
                meleeCooldown = 0.9;
            } else if (blinkCooldown <= 0) {
                blink(session, boss, foe, false);
            } else {
                walk(-towardFoe, 0.45);                    // se aleja
            }
            return;
        }
        if (distance > FAR_RANGE) {
            walk(towardFoe, 0.4);
            return;
        }
        if (shotCooldown <= 0) {
            attackToPress = random.nextDouble() < 0.65 ? 1 : 2;   // Psiquico o Bola Sombra
            shotCooldown = CALM_SHOT_SECONDS + random.nextDouble() * 0.35;
            return;
        }
        // Entre disparos camina un poquito para no quedarse quieto
        if (random.nextDouble() < 0.35) walk(random.nextBoolean() ? towardFoe : -towardFoe, 0.3);
    }

    /** Fase 2: agresivo, aparece detras del rival y pega de cerca. */
    private void decideEnraged(FightingGameSession session, Fighter boss, Fighter foe,
                               double distance, double towardFoe) {
        if (distance < CLOSE_RANGE) {
            if (meleeCooldown <= 0) {
                attackToPress = 0;                         // Psicocorte
                meleeCooldown = 0.55;
            } else if (random.nextDouble() < 0.3) {
                blockSeconds = 0.3;
            }
            return;
        }
        if (blinkCooldown <= 0 && random.nextDouble() < 0.45) {
            blink(session, boss, foe, true);
            meleeCooldown = 0;
            return;
        }
        if (distance < 420 && random.nextDouble() < 0.35) {
            attackToPress = 3;                             // Teletransporte (embestida)
            return;
        }
        if (shotCooldown <= 0) {
            attackToPress = random.nextDouble() < 0.5 ? 1 : 2;
            shotCooldown = 0.75 + random.nextDouble() * 0.35;
            return;
        }
        walk(towardFoe, 0.3);
    }

    /** Desaparece y aparece: furioso detras del rival, tranquilo lejos de el. */
    private void blink(FightingGameSession session, Fighter boss, Fighter foe, boolean behind) {
        double width = session.getWidth();
        double half = PhysicsSystem.halfWidthFor(boss);
        double target;
        if (behind) {
            double side = foe.isFacingLeft() ? 1 : -1;     // a la espalda del rival
            target = foe.getX() + side * (PhysicsSystem.halfWidthFor(foe) + half + 24);
            if (target < half || target > width - half) target = foe.getX() - side * 120;
        } else {
            target = foe.getX() < width / 2 ? width - 180 : 180;
        }
        if (session.bossBlink(target)) blinkCooldown = behind ? 2.4 : 3.2;
    }

    private void walk(double direction, double seconds) {
        walkDirection = direction;
        walkSeconds = seconds;
    }

    private boolean incomingProjectile(FightingGameSession session, Fighter boss) {
        for (FightingProjectile projectile : session.getProjectiles()) {
            if (projectile.getOwnerIndex() == me) continue;
            double dx = boss.getX() - projectile.getX();
            boolean coming = Math.signum(dx) == Math.signum(projectile.getVelocityX());
            if (coming && Math.abs(dx) < 260) return true;
        }
        return false;
    }

    /** Lo que el teclado del lado de Mewtwo haya apretado no cuenta. */
    private void ignoreHumanKeys(FightingInput input) {
        if (me == 0) {
            input.consumePlayerOneAttackRequest();
            input.consumePlayerOneJump();
            input.consumePlayerOneEvolution();
            input.consumePlayerOneUltimate();
            input.consumePlayerOneSwitch();
        } else {
            input.consumePlayerTwoAttackRequest();
            input.consumePlayerTwoJump();
            input.consumePlayerTwoEvolution();
            input.consumePlayerTwoUltimate();
            input.consumePlayerTwoSwitch();
        }
    }

    /** Aprieta (o suelta) los botones de Mewtwo en este paso. */
    private void press(FightingInput input, double direction, boolean block, int attack,
                       boolean jump, boolean ultimate) {
        if (me == 0) {
            input.setPlayerOneLeft(direction < 0);
            input.setPlayerOneRight(direction > 0);
            input.setPlayerOneBlock(block);
            input.setPlayerOneJump(jump);
            input.setPlayerOneUltimate(ultimate);
            input.setPlayerOneAttackOne(attack == 0);
            input.setPlayerOneAttackTwo(attack == 1);
            input.setPlayerOneAttackThree(attack == 2);
            input.setPlayerOneAttackFour(attack == 3);
        } else {
            input.setPlayerTwoLeft(direction < 0);
            input.setPlayerTwoRight(direction > 0);
            input.setPlayerTwoBlock(block);
            input.setPlayerTwoJump(jump);
            input.setPlayerTwoUltimate(ultimate);
            input.setPlayerTwoAttackOne(attack == 0);
            input.setPlayerTwoAttackTwo(attack == 1);
            input.setPlayerTwoAttackThree(attack == 2);
            input.setPlayerTwoAttackFour(attack == 3);
        }
    }
}
