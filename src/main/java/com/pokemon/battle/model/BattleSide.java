package com.pokemon.battle.model;

/**
 * One side of a battle: a trainer (player or machine) and its team.
 */
public class BattleSide {

    private final Trainer trainer;
    private final Team team;

    /**
     * @param trainer the trainer that owns the team
     * @param team    the team of the trainer
     */
    public BattleSide(Trainer trainer, Team team) {
        this.trainer = trainer;
        this.team = team;
    }

    /**
     * @return the trainer of this side
     */
    public Trainer getTrainer() {
        return trainer;
    }

    /**
     * @return the team of this side
     */
    public Team getTeam() {
        return team;
    }

    /**
     * @return true when this side is controlled by the machine
     */
    public boolean isMachine() {
        return trainer instanceof Machine;
    }
}