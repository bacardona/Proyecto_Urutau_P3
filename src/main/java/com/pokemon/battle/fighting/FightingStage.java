package com.pokemon.battle.fighting;

/** Maps you can pick for the free fight. */
public enum FightingStage {
    BOSQUE("BOSQUE", "El escenario original del combate libre"),
    TEMPLO("TEMPLO DE LA SELVA", "Ruinas con antorchas, luz y hojas que caen"),
    VOLCAN("CUEVA DEL VOLCAN", "Cascadas de lava, brasas y un crater encendido");

    private final String title;
    private final String description;

    FightingStage(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() { return title; }
    public String description() { return description; }
}
