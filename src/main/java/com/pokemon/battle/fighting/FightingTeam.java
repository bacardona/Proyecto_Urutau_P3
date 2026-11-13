package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

import java.util.List;
import java.util.Objects;

/** Ordered three-member free-fight team; member zero starts as the active fighter. */
public final class FightingTeam {

    private final String trainerName;
    private final List<Fighter> members;
    private int activeIndex;

    public FightingTeam(String trainerName, List<Pokemon> pokemon, double startX,
                        double groundY, boolean facingLeft) {
        if (pokemon == null || pokemon.size() != 3) {
            throw new IllegalArgumentException("El equipo libre debe tener tres Pokemon");
        }
        this.trainerName = Objects.requireNonNull(trainerName);
        this.members = pokemon.stream()
                .map(species -> new Fighter(species, startX, groundY, facingLeft))
                .toList();
    }

    static FightingTeam prototype(String trainerName, String[] species,
                                  double startX, double groundY, boolean facingLeft) {
        List<Fighter> fighters = java.util.Arrays.stream(species)
                .map(name -> new Fighter(name, startX, groundY, facingLeft))
                .toList();
        return new FightingTeam(trainerName, fighters, true);
    }

    private FightingTeam(String trainerName, List<Fighter> members, boolean prototype) {
        if (members.size() != 3) throw new IllegalArgumentException("El equipo debe tener tres miembros");
        this.trainerName = trainerName;
        this.members = List.copyOf(members);
    }

    public String getTrainerName() { return trainerName; }
    public List<Fighter> getMembers() { return members; }
    public Fighter getActive() { return members.get(activeIndex); }
    public int getActiveIndex() { return activeIndex; }
}
