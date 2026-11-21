package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

import java.util.ArrayList;
import java.util.List;

/** Holds the participants and fixed arena data for one free-fight session. */
public class FightingGameSession {

    public enum State { RUNNING, PAUSED, FINISHED }
    public enum Outcome { PLAYER_ONE_WINS, PLAYER_TWO_WINS, DRAW }

    private static final double PLAYER_SPEED = 320.0;
    private final int width;
    private final int height;
    private final int groundY;
    private final List<Fighter> fighters;
    private final FightingTeam playerOneTeam;
    private final FightingTeam playerTwoTeam;
    private final List<Platform> platforms;
    private final FightingStage stage;
    private final PhysicsSystem physicsSystem = new PhysicsSystem();
    private final FightingCombatSystem combatSystem = new FightingCombatSystem();
    private final boolean[] defeatCleanupDone = new boolean[2];
    private State state = State.RUNNING;
    private Outcome outcome;

    public FightingGameSession() {
        this(FightingStage.BOSQUE);
    }

    /** Combate de prueba en el mapa indicado. */
    FightingGameSession(FightingStage stage) {
        this("JUGADOR 1", List.of(), "JUGADOR 2", List.of(), true, stage);
    }

    public FightingGameSession(FreeFightSetup setup) {
        this(setup.getPlayerOne().getName(), setup.getTeamOne(),
                setup.getPlayerTwo().getName(), setup.getTeamTwo(), false, setup.getStage());
        if (!setup.isComplete()) {
            throw new IllegalArgumentException("La selección de Combate Libre está incompleta");
        }
    }

    private FightingGameSession(String playerOneName, List<Pokemon> teamOne,
                                String playerTwoName, List<Pokemon> teamTwo,
                                boolean usePrototypeTeams) {
        this(playerOneName, teamOne, playerTwoName, teamTwo, usePrototypeTeams, FightingStage.BOSQUE);
    }

    private FightingGameSession(String playerOneName, List<Pokemon> teamOne,
                                String playerTwoName, List<Pokemon> teamTwo,
                                boolean usePrototypeTeams, FightingStage stage) {
        this.width = 1280;
        this.height = 720;
        this.groundY = 677;
        this.stage = stage == null ? FightingStage.BOSQUE : stage;
        this.platforms = platformsFor(this.stage, groundY, width);
        List<Pokemon> firstTeam = usePrototypeTeams ? List.of() : teamOne;
        List<Pokemon> secondTeam = usePrototypeTeams ? List.of() : teamTwo;
        this.playerOneTeam = usePrototypeTeams
                ? FightingTeam.prototype(playerOneName,
                    new String[]{"PIKACHU", "BULBASAUR", "EEVEE"}, 230, groundY, false)
                : new FightingTeam(playerOneName, firstTeam, 230, groundY, false);
        this.playerTwoTeam = usePrototypeTeams
                ? FightingTeam.prototype(playerTwoName,
                    new String[]{"CHARIZARD", "SQUIRTLE", "CHARMANDER"}, 1050, groundY, true)
                : new FightingTeam(playerTwoName, secondTeam, 1050, groundY, true);
        this.fighters = new ArrayList<>(List.of(playerOneTeam.getActive(), playerTwoTeam.getActive()));
        this.bossIndex = -1;
        this.hardBoss = false;
        this.bossAi = null;
    }

    // El bosque tiene las plataformas de Angel; el templo y el volcan solo el piso
    private static List<Platform> platformsFor(FightingStage stage, int groundY, int width) {
        return stage == FightingStage.BOSQUE
                ? List.of(
                        new Platform(100, 570, 180, 24),
                        new Platform(320, 475, 160, 24),
                        new Platform(510, 375, 155, 24),
                        new Platform(700, 425, 145, 24),
                        new Platform(870, 475, 160, 24),
                        new Platform(1060, 570, 170, 24),
                        new Platform(0, groundY, width, 24))
                : List.of(new Platform(0, groundY, width, 24));
    }

    // ------------------------------------------------------------ jefe secreto

    /** Nombre que aparece como "entrenador" del jefe. */
    public static final String BOSS_NAME = "MEWTWO";
    private final int bossIndex;
    private final BossAI bossAi;
    private final boolean hardBoss;
    private boolean bossEnragedEvent;
    private boolean bossBlinkEvent;
    private double bossBlinkFromX;

    private FightingGameSession(FightingTeam one, FightingTeam two, FightingStage stage, int bossIndex,
                                boolean hardBoss) {
        this.width = 1280;
        this.height = 720;
        this.groundY = 677;
        this.stage = stage == null ? FightingStage.TEMPLO : stage;
        this.platforms = platformsFor(this.stage, groundY, width);
        this.playerOneTeam = one;
        this.playerTwoTeam = two;
        this.fighters = new ArrayList<>(List.of(one.getActive(), two.getActive()));
        this.bossIndex = bossIndex;
        this.hardBoss = hardBoss;
        this.bossAi = new BossAI(bossIndex, hardBoss);
    }

    /**
     * Pelea contra Mewtwo: el retador conserva su lado (y sus teclas) y su equipo entra con la vida llena.
     * @param challengerOnRight true si el que gano era el jugador 2
     */
    public static FightingGameSession bossFight(String challengerName, List<Pokemon> challengerTeam,
                                                boolean challengerOnRight, FightingStage stage) {
        return bossFight(challengerName, challengerTeam, challengerOnRight, stage, false);
    }

    /** @param hard revancha "Mewtwo Despertado" (se ofrece al vencerlo en normal) */
    public static FightingGameSession bossFight(String challengerName, List<Pokemon> challengerTeam,
                                                boolean challengerOnRight, FightingStage stage, boolean hard) {
        FightingTeam challenger = new FightingTeam(challengerName, challengerTeam,
                challengerOnRight ? 1050 : 230, 677, challengerOnRight);
        return withBoss(challenger, challengerOnRight, stage, hard);
    }

    /** Lo mismo con Pokemon de prueba (sin base de datos), para las pruebas y el video. */
    static FightingGameSession bossFightPrototype(String challengerName, String[] species,
                                                  boolean challengerOnRight, FightingStage stage) {
        return bossFightPrototype(challengerName, species, challengerOnRight, stage, false);
    }

    static FightingGameSession bossFightPrototype(String challengerName, String[] species,
                                                  boolean challengerOnRight, FightingStage stage, boolean hard) {
        FightingTeam challenger = FightingTeam.prototype(challengerName, species,
                challengerOnRight ? 1050 : 230, 677, challengerOnRight);
        return withBoss(challenger, challengerOnRight, stage, hard);
    }

    private static FightingGameSession withBoss(FightingTeam challenger, boolean challengerOnRight,
                                                FightingStage stage, boolean hard) {
        Fighter mewtwo = new Fighter("MEWTWO", challengerOnRight ? 230 : 1050, 677, !challengerOnRight);
        FightingTeam boss = FightingTeam.boss(BOSS_NAME, mewtwo);
        if (hard) mewtwo.markAsHardBoss();
        return challengerOnRight
                ? new FightingGameSession(boss, challenger, stage, 0, hard)
                : new FightingGameSession(challenger, boss, stage, 1, hard);
    }

    public boolean isBossFight() { return bossIndex >= 0; }
    /** Revancha dificil: barrera, ataques nuevos y Mega Mewtwo Y. */
    public boolean isHardBoss() { return hardBoss; }
    /** 0 o 1 segun el lado de Mewtwo; -1 si no es pelea de jefe. */
    public int getBossIndex() { return bossIndex; }
    public Fighter getBoss() { return bossIndex < 0 ? null : fighters.get(bossIndex); }
    /** True si el retador le gano a Mewtwo. */
    public boolean challengerWon() {
        if (bossIndex < 0 || outcome == null) return false;
        return bossIndex == 1 ? outcome == Outcome.PLAYER_ONE_WINS : outcome == Outcome.PLAYER_TWO_WINS;
    }

    /** Una sola vez: Mewtwo acaba de entrar en su segunda fase. */
    public boolean consumeBossEnraged() {
        boolean was = bossEnragedEvent;
        bossEnragedEvent = false;
        return was;
    }

    /** Una sola vez: Mewtwo se acaba de teletransportar (devuelve de donde salio, o NaN). */
    public double consumeBossBlink() {
        if (!bossBlinkEvent) return Double.NaN;
        bossBlinkEvent = false;
        return bossBlinkFromX;
    }

    /** Teletransporte de Mewtwo: desaparece y aparece en x mirando al rival. */
    boolean bossBlink(double x) {
        Fighter boss = getBoss();
        if (boss == null || boss.isDefeated() || boss.isHitStunned() || boss.isAsleep()) return false;
        double half = PhysicsSystem.halfWidthFor(boss);
        bossBlinkFromX = boss.getX();
        double targetX = Math.max(half, Math.min(width - half, x));
        Fighter rival = fighters.get(1 - bossIndex);
        // Si el rival esta en una plataforma, aparece arriba junto a el (en la misma plataforma)
        double targetY = groundY;
        if (isRivalAboveBoss()) {
            Platform platform = platformAt(rival.getX(), rival.getY());
            if (platform != null) {
                double minX = platform.getX() + half * 0.6, maxX = platform.getX() + platform.getWidth() - half * 0.6;
                targetX = minX > maxX ? platform.getX() + platform.getWidth() / 2.0
                        : Math.max(minX, Math.min(maxX, targetX));
                targetY = platform.getTop();
            }
        }
        boss.setX(targetX);
        boss.setY(targetY);
        boss.setVelocityY(0);
        boss.setOnGround(true);
        boss.setFacingLeft(rival.getX() < boss.getX());
        bossBlinkEvent = true;
        return true;
    }

    private void updateBossPhase(double dt) {
        Fighter boss = getBoss();
        if (boss == null || boss.isDefeated()) return;
        updateBossTechniques(boss, dt);
        // Espera a que termine una ultimate en curso para no cortarla con una escena
        if (!combatSystem.getUltimateVisuals().isEmpty()) return;
        if (!boss.isEnraged() && boss.getHealth() <= boss.getMaxHealth() / 2) {
            boss.enrage();
            boss.addEvolutionEnergy(60);   // al enfurecerse ya tiene media barra de Psicoataque
            bossEnragedEvent = true;
            return;
        }
        if (!hardBoss) return;
        if (!boss.isMega() && !boss.isMegaPending()
                && boss.getHealth() <= boss.getMaxHealth() * MEGA_AT) {
            boss.startMega();
            megaSceneLeft = MEGA_SCENE_SECONDS;
            futureSights.clear();
            pendingVolley.clear();
            waves.clear();
            waveCharge = -1;
            combatSystem.clearProjectiles();
            bossEvents.add(new BossEvent(BossEvent.Kind.MEGA, boss.getX(), boss.getY()));
        }
    }

    // ------------------------------------------------------------ modo dificil
    static final double MEGA_AT = 0.20;
    /** Lo que dura la escena de Mega (el director la congela; esto es por si no hay director). */
    static final double MEGA_SCENE_SECONDS = 6.4;
    static final double FUTURE_SIGHT_DELAY = 1.2;
    static final double FUTURE_SIGHT_RADIUS = 62;
    static final double TELEKINESIS_PULL = 760;

    /** Avisos para los efectos (FightingFx los consume). */
    public record BossEvent(Kind kind, double x, double y) {
        public enum Kind { MEGA, BARRIER, TELEKINESIS, FUTURE_SIGHT_MARK, FUTURE_SIGHT_FALL, FUTURE_SIGHT_STRIKE,
            VOLLEY, WAVE_CHARGE, WAVE, TRIPLE_BLINK }
    }

    /** Marca de Premonicion en el suelo: progress va de 0 a 1 hasta que cae el golpe. */
    public record FutureSightMark(double x, double progress, double y) { }

    /** Onda mental que avanza por el suelo (direction -1 o 1). */
    public record PsychicWave(double x, double direction) { }

    // Fase Mega: al transformarse recupera vida hasta el 60 %, y tiene la Onda mental
    static final double MEGA_HEAL_TO = 0.50;
    static final double FUTURE_SIGHT_FALL_SECONDS = 0.15;
    static final double WAVE_CHARGE_SECONDS = 0.45;
    static final double WAVE_SPEED = 720;
    private final List<double[]> waves = new ArrayList<>();          // {x, direccion}
    private double waveCharge = -1;
    private boolean waveHit;

    public List<PsychicWave> getPsychicWaves() {
        List<PsychicWave> list = new ArrayList<>();
        for (double[] wave : waves) list.add(new PsychicWave(wave[0], wave[1]));
        return list;
    }

    /** Onda mental: Mewtwo carga medio segundo y salen dos ondas por el suelo, una a cada lado. */
    boolean bossPsychicWave() {
        Fighter boss = getBoss();
        if (boss == null || !boss.isMega() || !waves.isEmpty() || waveCharge >= 0 || bossUltimateActive()) {
            return false;
        }
        waveCharge = WAVE_CHARGE_SECONDS;
        bossEvents.add(new BossEvent(BossEvent.Kind.WAVE_CHARGE, boss.getX(), boss.getY()));
        return true;
    }

    /** Aviso para el efecto del Teletransporte triple. */
    void announceTripleBlink() {
        Fighter boss = getBoss();
        if (boss != null) bossEvents.add(new BossEvent(BossEvent.Kind.TRIPLE_BLINK, boss.getX(), boss.getY()));
    }

    /** Durante la escena de Mega, la barra de Mewtwo se va llenando (la llama el director). */
    void bossMegaHeal(int amount) {
        Fighter boss = getBoss();
        if (boss != null) boss.heal(amount);
    }

    int bossMegaHealTarget() {
        Fighter boss = getBoss();
        return boss == null ? 0 : (int) Math.round(boss.getMaxHealth() * MEGA_HEAL_TO);
    }

    private final List<BossEvent> bossEvents = new ArrayList<>();
    private final List<double[]> futureSights = new ArrayList<>();   // {x, tiempo}
    private final List<double[]> pendingVolley = new ArrayList<>();  // {espera, altura}
    private double megaSceneLeft;

    public List<BossEvent> consumeBossEvents() {
        if (bossEvents.isEmpty()) return List.of();
        List<BossEvent> copy = List.copyOf(bossEvents);
        bossEvents.clear();
        return copy;
    }

    public List<FutureSightMark> getFutureSightMarks() {
        List<FutureSightMark> marks = new ArrayList<>();
        for (double[] mark : futureSights) marks.add(new FutureSightMark(mark[0], mark[1] / FUTURE_SIGHT_DELAY, mark[2]));
        return marks;
    }

    /** La escena de Mega llego a la explosion: desde aqui pelea como Mega Mewtwo Y. */
    void completeBossMega() {
        Fighter boss = getBoss();
        if (boss != null && boss.isMegaPending()) {
            boss.completeMega();
            boss.addEvolutionEnergy(100);
            megaSceneLeft = 0;
        }
    }

    /** Sin director (pruebas): termina la transformacion y cura de una vez. */
    private void completeBossMegaWithoutScene() {
        Fighter boss = getBoss();
        if (boss == null) return;
        completeBossMega();
        int missing = bossMegaHealTarget() - boss.getHealth();
        if (missing > 0) boss.heal(missing);
    }

    /** Triple Bola Sombra: baja, al medio y alta, una detras de otra. */
    boolean bossTripleShadowBall() {
        Fighter boss = getBoss();
        if (boss == null || !pendingVolley.isEmpty() || bossUltimateActive()) return false;
        pendingVolley.add(new double[]{0.0, 28});
        pendingVolley.add(new double[]{0.32, 62});
        pendingVolley.add(new double[]{0.64, 150});
        bossEvents.add(new BossEvent(BossEvent.Kind.VOLLEY, boss.getX(), boss.getY()));
        return true;
    }

    /** Telequinesis: jala al rival hacia Mewtwo y lo deja un instante sin poder moverse. */
    boolean bossTelekinesis() {
        Fighter boss = getBoss();
        if (boss == null || bossUltimateActive()) return false;
        Fighter foe = fighters.get(1 - bossIndex);
        if (foe.isDefeated()) return false;
        double toward = boss.getX() >= foe.getX() ? 1 : -1;
        foe.applyKnockback(toward, TELEKINESIS_PULL);
        foe.applyBlockStun(0.45);
        bossEvents.add(new BossEvent(BossEvent.Kind.TELEKINESIS, foe.getX(), foe.getY()));
        return true;
    }

    /** Premonicion: marca donde esta el rival; en 1.2 s cae un golpe ahi. */
    boolean bossFutureSight() {
        Fighter boss = getBoss();
        // Solo una Premonicion en pantalla, y nunca durante su Psicoataque
        if (boss == null || !futureSights.isEmpty() || bossUltimateActive()) return false;
        Fighter foe = fighters.get(1 - bossIndex);
        // La marca queda donde esta parado el rival (suelo o plataforma)
        double markY = foe.isOnGround() ? foe.getY() : groundY;
        futureSights.add(new double[]{foe.getX(), 0, markY});
        bossEvents.add(new BossEvent(BossEvent.Kind.FUTURE_SIGHT_MARK, foe.getX(), markY));
        return true;
    }

    /** Barrera: unos segundos devuelve los proyectiles. */
    boolean bossBarrier(double seconds) {
        Fighter boss = getBoss();
        if (boss == null || boss.hasBarrier() || boss.isBlocking()) return false;
        boss.raiseBarrier(seconds);
        bossEvents.add(new BossEvent(BossEvent.Kind.BARRIER, boss.getX(), boss.getY()));
        return true;
    }

    private void updatePsychicWaves(Fighter boss, Fighter foe, double dt) {
        if (waveCharge >= 0) {
            waveCharge -= dt;
            if (waveCharge < 0) {
                waves.add(new double[]{boss.getX(), -1});
                waves.add(new double[]{boss.getX(), 1});
                waveHit = false;
                bossEvents.add(new BossEvent(BossEvent.Kind.WAVE, boss.getX(), groundY));
            }
        }
        for (java.util.Iterator<double[]> it = waves.iterator(); it.hasNext(); ) {
            double[] wave = it.next();
            wave[0] += wave[1] * WAVE_SPEED * dt;
            // Solo pega a quien esta en el suelo (saltandola se esquiva), una vez por ataque
            boolean grounded = foe.isOnGround() || foe.getY() > groundY - 30;
            if (!waveHit && !foe.isDefeated() && grounded && Math.abs(foe.getX() - wave[0]) < 40) {
                waveHit = true;
                combatSystem.bossMindWaveHit(bossIndex, boss, foe);
            }
            if (wave[0] < -60 || wave[0] > width + 60) it.remove();
        }
    }

    private boolean bossUltimateActive() {
        return !"IDLE".equals(combatSystem.getUltimatePhaseName(bossIndex));
    }

    /** Mas alto que esto (en pixeles) el rival cuenta como "arriba en una plataforma". */
    static final double ELEVATED_GAP = 40;
    static final double MAX_AIM_RADIANS = 0.75;

    /** True si el rival esta claramente mas arriba que Mewtwo (en una plataforma). */
    public boolean isRivalAboveBoss() {
        Fighter boss = getBoss();
        if (boss == null) return false;
        Fighter foe = fighters.get(1 - bossIndex);
        return foe.isOnGround() && foe.getY() < boss.getY() - ELEVATED_GAP;
    }

    /** Hacia donde apunta Mewtwo: recto, o inclinado hacia el rival si esta arriba. */
    private void updateBossAim(Fighter boss, Fighter foe) {
        if (!isRivalAboveBoss()) {
            boss.setAimUpAngle(0);
            return;
        }
        double dx = Math.max(60, Math.abs(foe.getX() - boss.getX()));
        double dy = (boss.getY() - boss.getRenderHeight() * 0.62) - (foe.getY() - foe.getRenderHeight() * 0.5);
        boss.setAimUpAngle(Math.max(0, Math.min(MAX_AIM_RADIANS, Math.atan2(dy, dx))));
    }

    /** Si existe, la plataforma bajo x a la altura del rival (para teletransportarse junto a el). */
    private Platform platformAt(double x, double topY) {
        for (Platform platform : platforms) {
            if (Math.abs(platform.getTop() - topY) < 6 && x >= platform.getX() && x <= platform.getX() + platform.getWidth()) {
                return platform;
            }
        }
        return null;
    }

    private void updateBossTechniques(Fighter boss, double dt) {
        if (boss.isMegaPending()) {
            // Sin director (pruebas) la transformacion termina sola
            megaSceneLeft -= dt;
            if (megaSceneLeft <= 0) completeBossMegaWithoutScene();
            return;
        }
        Fighter foe = fighters.get(1 - bossIndex);
        updateBossAim(boss, foe);
        for (java.util.Iterator<double[]> it = pendingVolley.iterator(); it.hasNext(); ) {
            double[] shot = it.next();
            shot[0] -= dt;
            if (shot[0] <= 0) {
                boss.setFacingLeft(foe.getX() < boss.getX());
                combatSystem.spawnBossShadowBall(bossIndex, boss, shot[1]);
                it.remove();
            }
        }
        for (java.util.Iterator<double[]> it = futureSights.iterator(); it.hasNext(); ) {
            double[] mark = it.next();
            double before = mark[1];
            mark[1] += dt;
            // Un instante antes del golpe empieza a caer la columna desde el cielo
            double fallAt = FUTURE_SIGHT_DELAY - FUTURE_SIGHT_FALL_SECONDS;
            if (before < fallAt && mark[1] >= fallAt) {
                bossEvents.add(new BossEvent(BossEvent.Kind.FUTURE_SIGHT_FALL, mark[0], mark[2]));
            }
            if (mark[1] >= FUTURE_SIGHT_DELAY) {
                it.remove();
                bossEvents.add(new BossEvent(BossEvent.Kind.FUTURE_SIGHT_STRIKE, mark[0], mark[2]));
                if (!foe.isDefeated() && Math.abs(foe.getX() - mark[0]) < FUTURE_SIGHT_RADIUS) {
                    combatSystem.bossFutureSightStrike(bossIndex, boss, foe);
                }
            }
        }
        updatePsychicWaves(boss, foe, dt);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getGroundY() { return groundY; }
    public FightingStage getStage() { return stage; }
    public List<Fighter> getFighters() { return List.copyOf(fighters); }
    public FightingTeam getPlayerOneTeam() { return playerOneTeam; }
    public FightingTeam getPlayerTwoTeam() { return playerTwoTeam; }
    public List<Platform> getPlatforms() { return platforms; }
    public List<FightingProjectile> getProjectiles() { return combatSystem.getProjectiles(); }
    public FightingCombatSystem getCombatSystem() { return combatSystem; }
    public State getState() { return state; }
    public Outcome getOutcome() { return outcome; }
    public String getTrainerName(int playerIndex) {
        return playerIndex == 0 ? playerOneTeam.getTrainerName() : playerTwoTeam.getTrainerName();
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            combatSystem.clearPendingInputs();
        }
    }

    public void resume() {
        if (state == State.PAUSED) state = State.RUNNING;
    }

    public void update(FightingInput input, double deltaSeconds) {
        if (state != State.RUNNING) return;
        if (bossAi != null) bossAi.drive(this, input, deltaSeconds);
        for (Fighter fighter : playerOneTeam.getMembers()) {
            fighter.advanceDefeatedAnimation(deltaSeconds);
        }
        for (Fighter fighter : playerTwoTeam.getMembers()) {
            fighter.advanceDefeatedAnimation(deltaSeconds);
        }
        replaceDefeatedFighters();
        checkForWinner();
        if (state != State.RUNNING) return;
        if (hasPendingDefeatAnimation()) {
            discardTransientInputs(input);
            return;
        }
        Fighter playerOne = fighters.get(0);
        Fighter playerTwo = fighters.get(1);
        playerOne.updateStatusEffects(deltaSeconds);
        playerTwo.updateStatusEffects(deltaSeconds);
        // Esquiva: con el escudo puesto, tocar una direccion (A/D, flechas o la palanca)
        tryDodge(0, input.isPlayerOneBlockingRequested(), input.getPlayerOneDirection());
        tryDodge(1, input.isPlayerTwoBlockingRequested(), input.getPlayerTwoDirection());
        playerOne.setBlocking(input.isPlayerOneBlockingRequested() && !playerOne.isDodging()
                && combatSystem.canBlock(0, fighters));
        playerTwo.setBlocking(input.isPlayerTwoBlockingRequested() && !playerTwo.isDodging()
                && combatSystem.canBlock(1, fighters));
        boolean playerOneJump = input.consumePlayerOneJump();
        boolean playerTwoJump = input.consumePlayerTwoJump();
        boolean playerOneEvolution = input.consumePlayerOneEvolution();
        boolean playerTwoEvolution = input.consumePlayerTwoEvolution();
        boolean playerOneUltimate = input.consumePlayerOneUltimate();
        boolean playerTwoUltimate = input.consumePlayerTwoUltimate();
        combatSystem.beginUltimateRequests(fighters, playerOneUltimate, playerTwoUltimate);
        var beforeOne = PhysicsSystem.positionOf(playerOne);
        var beforeTwo = PhysicsSystem.positionOf(playerTwo);
        // Mientras esquiva no camina (la direccion solo eligio hacia donde esquivar)
        boolean walkingOne = updateFighter(playerOne, 0, playerOne.isDodging() ? 0 : input.getPlayerOneDirection(),
                playerOneJump && !playerOne.isDodging(), input.isPlayerOneJumpHeld(), deltaSeconds);
        boolean walkingTwo = updateFighter(playerTwo, 1, playerTwo.isDodging() ? 0 : input.getPlayerTwoDirection(),
                playerTwoJump && !playerTwo.isDodging(), input.isPlayerTwoJumpHeld(), deltaSeconds);
        moveDodger(playerOne, deltaSeconds);
        moveDodger(playerTwo, deltaSeconds);
        PhysicsSystem.resolveBodies(playerOne, playerTwo, beforeOne, beforeTwo, width);
        updateMotion(playerOne, walkingOne && Math.abs(playerOne.getX() - beforeOne.x()) > 0.01, deltaSeconds);
        updateMotion(playerTwo, walkingTwo && Math.abs(playerTwo.getX() - beforeTwo.x()) > 0.01, deltaSeconds);
        combatSystem.update(fighters, width, platforms,
                input.consumePlayerOneAttackRequest(), input.consumePlayerTwoAttackRequest(),
                deltaSeconds);
        for (Fighter fighter : fighters) {
            fighter.advanceKnockback(deltaSeconds, width, PhysicsSystem.halfWidthFor(fighter));
        }
        replaceDefeatedFighters();
        if (bossAi != null) updateBossPhase(deltaSeconds);
        boolean playerOneSwitch = input.consumePlayerOneSwitch();
        boolean playerTwoSwitch = input.consumePlayerTwoSwitch();
        updateEntrances();
        if (playerOneSwitch) trySwitchActiveFighter(0);
        if (playerTwoSwitch) trySwitchActiveFighter(1);
        if (switchCooldown[0] > 0) switchCooldown[0] -= deltaSeconds;
        if (switchCooldown[1] > 0) switchCooldown[1] -= deltaSeconds;
        if (playerOneEvolution) tryEvolveActiveFighter(0);
        if (playerTwoEvolution) tryEvolveActiveFighter(1);
        Fighter first = fighters.get(0);
        Fighter second = fighters.get(1);
        PhysicsSystem.resolveBodies(first, second, PhysicsSystem.positionOf(first),
                PhysicsSystem.positionOf(second), width);
        for (Fighter fighter : fighters) {
            fighter.updateShieldVisual(combatSystem.getAttackVisual(fighter, fighters) != null, deltaSeconds);
        }
        checkForWinner();
    }

    private boolean tryEvolveActiveFighter(int playerIndex) {
        Fighter fighter = fighters.get(playerIndex);
        if (!fighter.canEvolve() || !combatSystem.canEvolve(playerIndex, fighters)) return false;
        return fighter.evolve();
    }

    public boolean debugDefeatActiveFighter(int playerIndex) {
        if (state != State.RUNNING || (playerIndex != 0 && playerIndex != 1)) return false;
        Fighter active = fighters.get(playerIndex);
        if (active.isDefeated()) return false;
        active.markDefeated();
        replaceDefeatedFighters();
        checkForWinner();
        return true;
    }

    public boolean debugFillActiveEnergy(int playerIndex) {
        if (state != State.RUNNING || (playerIndex != 0 && playerIndex != 1)) return false;
        Fighter active = fighters.get(playerIndex);
        if (active.isDefeated()) return false;
        active.addEvolutionEnergy(100);
        return true;
    }

    // Cambio de Pokemon tocando la pared (Q jugador 1, U jugador 2)
    static final double SWITCH_COOLDOWN_SECONDS = 4.0;

    /** Direccion que cada jugador tenia el paso anterior (para notar cuando la toca de nuevo). */
    private final double[] previousDirection = new double[2];

    /**
     * Esquiva rapida: con el escudo puesto, tocar una direccion la lanza hacia ese lado.
     * Solo cuenta si la direccion se toca estando ya cubierto (mantenerla antes no esquiva).
     */
    private void tryDodge(int index, boolean shieldHeld, double direction) {
        double before = previousDirection[index];
        previousDirection[index] = direction;
        if (!shieldHeld || direction == 0 || direction == before) return;
        Fighter fighter = fighters.get(index);
        if (fighter.isBoss() || !fighter.canDodge()
                || combatSystem.getAttackVisual(fighter, fighters) != null) return;
        fighter.startDodge(direction > 0 ? 1 : -1);
        fighter.setBlocking(false);
    }

    /** Mueve al que esquiva (rapido, en linea recta, sin salirse del mapa). */
    private void moveDodger(Fighter fighter, double deltaSeconds) {
        fighter.advanceDodge(deltaSeconds);
        if (!fighter.isDodging()) return;
        double half = PhysicsSystem.halfWidthFor(fighter);
        fighter.setX(Math.max(half, Math.min(width - half,
                fighter.getX() + fighter.getDodgeDirection() * Fighter.DODGE_SPEED * deltaSeconds)));
    }
    static final double WALL_TOUCH_MARGIN = 8;
    private final double[] switchCooldown = new double[2];

    /** True when the fighter is touching the left or right wall of the arena. */
    boolean isTouchingWall(Fighter fighter) {
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        return fighter.getX() <= halfWidth + WALL_TOUCH_MARGIN
                || fighter.getX() >= width - halfWidth - WALL_TOUCH_MARGIN;
    }

    /**
     * Switches the active Pokemon for the other one of the team. Only against the wall (ground or air),
     * when the player is free (not attacking, stunned, asleep or in an
     * ultimate) and not more than once every few seconds. Each Pokemon keeps its health.
     */
    boolean trySwitchActiveFighter(int playerIndex) {
        Fighter current = fighters.get(playerIndex);
        FightingTeam team = playerIndex == 0 ? playerOneTeam : playerTwoTeam;
        // Vale en el suelo o en el aire, siempre que este pegado a la pared
        if (switchCooldown[playerIndex] > 0 || current.isHitStunned()
                || current.isEvolutionAnimating() || !isTouchingWall(current)
                || !combatSystem.canEvolve(playerIndex, fighters)
                || java.util.Set.of("CHARGING", "RELEASING", "FINISHING", "RECOVERY")
                        .contains(combatSystem.getUltimatePhaseName(playerIndex))) {
            return false;
        }
        if (!team.switchActive()) return false;
        Fighter incoming = team.getActive();
        current.setBlocking(false);
        combatSystem.clearPlayerState(playerIndex);
        // Entrada: el nuevo sale de la pared con un salto hacia el centro y cae con una onda
        boolean leftWall = current.getX() < width / 2.0;
        incoming.setX(current.getX());
        incoming.setY(Math.min(groundY, current.getY()));
        incoming.setVelocityY(ENTRANCE_JUMP_VELOCITY);
        incoming.setOnGround(false);
        incoming.setFacingLeft(!leftWall);
        incoming.applyKnockback(leftWall ? 1 : -1, ENTRANCE_PUSH_SPEED);
        current.setOnGround(true);
        current.setY(groundY);
        current.setVelocityY(0);
        fighters.set(playerIndex, incoming);
        switchCooldown[playerIndex] = SWITCH_COOLDOWN_SECONDS;
        entranceAirborne[playerIndex] = true;
        return true;
    }

    static final double ENTRANCE_JUMP_VELOCITY = -560;
    static final double ENTRANCE_PUSH_SPEED = 330;
    static final double ENTRANCE_SHOVE_RANGE = 150;
    static final double ENTRANCE_SHOVE_SPEED = 360;
    private final boolean[] entranceAirborne = new boolean[2];
    private final boolean[] entranceLanded = new boolean[2];

    /** When the incoming Pokemon lands, a shockwave pushes a nearby rival away (no damage). */
    private void updateEntrances() {
        for (int i = 0; i < 2; i++) {
            if (!entranceAirborne[i]) continue;
            Fighter f = fighters.get(i);
            if (!f.isOnGround()) continue;
            entranceAirborne[i] = false;
            entranceLanded[i] = true;
            Fighter rival = fighters.get(1 - i);
            double dx = rival.getX() - f.getX();
            if (!rival.isDefeated() && rival.isOnGround() && Math.abs(dx) < ENTRANCE_SHOVE_RANGE) {
                rival.applyKnockback(dx >= 0 ? 1 : -1, ENTRANCE_SHOVE_SPEED);
            }
        }
    }

    /** True once right after the incoming Pokemon lands (for the effects). */
    public boolean consumeEntranceLanding(int playerIndex) {
        if (playerIndex < 0 || playerIndex > 1 || !entranceLanded[playerIndex]) return false;
        entranceLanded[playerIndex] = false;
        return true;
    }

    private void replaceDefeatedFighters() {
        for (int playerIndex = 0; playerIndex < fighters.size(); playerIndex++) {
            Fighter defeated = fighters.get(playerIndex);
            if (!defeated.isDefeated()) {
                defeatCleanupDone[playerIndex] = false;
                continue;
            }
            if (!defeatCleanupDone[playerIndex]) {
                combatSystem.clearPlayerState(playerIndex);
                defeatCleanupDone[playerIndex] = true;
            }
            if (shouldPlayFaint(defeated)) continue;
            FightingTeam team = playerIndex == 0 ? playerOneTeam : playerTwoTeam;
            if (team.advanceAfterDefeat()) {
                Fighter replacement = team.getActive();
                Fighter opponent = fighters.get(1 - playerIndex);
                positionReplacement(replacement, opponent, playerIndex);
                fighters.set(playerIndex, replacement);
                defeatCleanupDone[playerIndex] = false;
            }
        }
    }

    private boolean hasPendingDefeatAnimation() {
        for (Fighter fighter : fighters) {
            if (shouldPlayFaint(fighter)) return true;
        }
        return false;
    }

    private boolean shouldPlayFaint(Fighter fighter) {
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(fighter);
        return profile != null && fighter.isDefeated()
                && fighter.getDefeatedAnimationSeconds() < profile.faintDuration();
    }

    private void discardTransientInputs(FightingInput input) {
        input.consumePlayerOneJump();
        input.consumePlayerTwoJump();
        input.consumePlayerOneAttackRequest();
        input.consumePlayerTwoAttackRequest();
        input.consumePlayerOneEvolution();
        input.consumePlayerTwoEvolution();
        input.consumePlayerOneUltimate();
        input.consumePlayerTwoUltimate();
    }

    private void positionReplacement(Fighter replacement, Fighter opponent, int playerIndex) {
        double halfWidth = PhysicsSystem.halfWidthFor(replacement);
        double preferred = playerIndex == 0 ? 230 : width - 230;
        double[] candidates = playerIndex == 0
                ? new double[]{preferred, 350, 180, 450, 100}
                : new double[]{preferred, width - 350, width - 180, width - 450, width - 100};
        double opponentHalfWidth = PhysicsSystem.halfWidthFor(opponent);
        for (double candidate : candidates) {
            double x = Math.max(halfWidth, Math.min(width - halfWidth, candidate));
            if (Math.abs(x - opponent.getX()) >= halfWidth + opponentHalfWidth + 20) {
                replacement.setX(x);
                replacement.setY(groundY);
                replacement.setVelocityY(0);
                replacement.setOnGround(true);
                return;
            }
        }
        replacement.setX(playerIndex == 0 ? halfWidth : width - halfWidth);
        replacement.setY(groundY);
        replacement.setVelocityY(0);
        replacement.setOnGround(true);
    }

    private void checkForWinner() {
        boolean playerOneDefeated = !playerOneTeam.hasAvailableFighter();
        boolean playerTwoDefeated = !playerTwoTeam.hasAvailableFighter();
        if (!playerOneDefeated && !playerTwoDefeated) return;
        if (hasPendingDefeatAnimation()) return;

        if (playerOneDefeated && playerTwoDefeated) outcome = Outcome.DRAW;
        else if (playerTwoDefeated) outcome = Outcome.PLAYER_ONE_WINS;
        else outcome = Outcome.PLAYER_TWO_WINS;
        state = State.FINISHED;
        combatSystem.clearAllState();
    }

    private boolean updateFighter(Fighter fighter, int playerIndex, double direction,
                               boolean jumpRequested, boolean jumpHeld,
                               double deltaSeconds) {
        if (playerIndex >= 0 && playerIndex < 2 && entranceAirborne[playerIndex]) {
            // Saltando de entrada: no se voltea hacia la pared aunque sigan apretando hacia alla
            direction = 0;
            jumpRequested = false;
            fighter.setFacingLeft(fighter.getX() > width / 2.0);
        }
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        double forcedDelta = combatSystem.getForcedHorizontalDelta(
                fighter, fighters, deltaSeconds, width);
        boolean inputLocked = combatSystem.isPlayerInputLocked(playerIndex, fighters);
        double beforeInputX = fighter.getX();
        boolean walkingFromInput = false;
        if (!fighter.isUltimateStunned() && !fighter.isHitStunned()
                && !fighter.isBlocking() && forcedDelta != 0) {
            fighter.setX(fighter.getX() + forcedDelta);
        } else if (!inputLocked && !fighter.isUltimateStunned()) {
            fighter.moveHorizontally(direction,
                    PLAYER_SPEED * fighter.getMovementSpeedMultiplier() * deltaSeconds);
            walkingFromInput = direction != 0;
        }
        double minX = halfWidth;
        double maxX = width - halfWidth;
        fighter.setX(Math.max(minX, Math.min(maxX, fighter.getX())));
        walkingFromInput &= Math.abs(fighter.getX() - beforeInputX) > 0.01;
        physicsSystem.update(fighter, platforms, halfWidth,
                (jumpRequested || jumpHeld) && !inputLocked, jumpHeld, deltaSeconds);
        return walkingFromInput;
    }

    private void updateMotion(Fighter fighter, boolean walkingFromInput, double deltaSeconds) {
        Fighter.MotionAnimation motion = fighter.isBlocking()
                ? Fighter.MotionAnimation.BLOCKING
                : !fighter.isOnGround() ? Fighter.MotionAnimation.JUMPING
                : walkingFromInput ? Fighter.MotionAnimation.WALKING
                : Fighter.MotionAnimation.IDLE;
        fighter.updateMotionAnimation(motion, deltaSeconds);
    }
}
