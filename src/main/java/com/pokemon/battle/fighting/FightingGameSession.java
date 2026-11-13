package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

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
    private final PhysicsSystem physicsSystem = new PhysicsSystem();
    private final FightingCombatSystem combatSystem = new FightingCombatSystem();
    private State state = State.RUNNING;
    private Outcome outcome;

    public FightingGameSession() {
        this("JUGADOR 1", List.of(), "JUGADOR 2", List.of(), true);
    }

    public FightingGameSession(FreeFightSetup setup) {
        this(setup.getPlayerOne().getName(), setup.getTeamOne(),
                setup.getPlayerTwo().getName(), setup.getTeamTwo(), false);
        if (!setup.isComplete()) {
            throw new IllegalArgumentException("La selección de Combate Libre está incompleta");
        }
    }

    private FightingGameSession(String playerOneName, List<Pokemon> teamOne,
                                String playerTwoName, List<Pokemon> teamTwo,
                                boolean usePrototypeTeams) {
        this.width = 1280;
        this.height = 720;
        this.groundY = 677;
        this.platforms = List.of(
                new Platform(100, 570, 180, 24),
                new Platform(320, 475, 160, 24),
                new Platform(510, 375, 155, 24),
                new Platform(700, 425, 145, 24),
                new Platform(870, 475, 160, 24),
                new Platform(1060, 570, 170, 24),
                new Platform(0, groundY, width, 24)
        );
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
        this.fighters = List.of(playerOneTeam.getActive(), playerTwoTeam.getActive());
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getGroundY() { return groundY; }
    public List<Fighter> getFighters() { return fighters; }
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
        if (state == State.RUNNING) state = State.PAUSED;
    }

    public void resume() {
        if (state == State.PAUSED) state = State.RUNNING;
    }

    public void update(FightingInput input, double deltaSeconds) {
        if (state != State.RUNNING) return;
        updateFighter(fighters.get(0), input.getPlayerOneDirection(),
                input.consumePlayerOneJump(), deltaSeconds);
        updateFighter(fighters.get(1), input.getPlayerTwoDirection(),
                input.consumePlayerTwoJump(), deltaSeconds);
        combatSystem.update(fighters, width, input.consumeQuickAttack(),
                input.consumeThunderShock(), input.consumeEmber(), input.consumeTackle(), deltaSeconds);
        checkForWinner();
    }

    private void checkForWinner() {
        boolean playerOneDefeated = fighters.get(0).getHealth() == 0;
        boolean playerTwoDefeated = fighters.get(1).getHealth() == 0;
        if (!playerOneDefeated && !playerTwoDefeated) return;

        if (playerOneDefeated && playerTwoDefeated) outcome = Outcome.DRAW;
        else if (playerTwoDefeated) outcome = Outcome.PLAYER_ONE_WINS;
        else outcome = Outcome.PLAYER_TWO_WINS;
        state = State.FINISHED;
    }

    private void updateFighter(Fighter fighter, double direction,
                               boolean jumpRequested,
                               double deltaSeconds) {
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        fighter.moveHorizontally(direction, PLAYER_SPEED * deltaSeconds);
        double minX = halfWidth;
        double maxX = width - halfWidth;
        fighter.setX(Math.max(minX, Math.min(maxX, fighter.getX())));
        physicsSystem.update(fighter, platforms, halfWidth, jumpRequested, deltaSeconds);
    }
}
