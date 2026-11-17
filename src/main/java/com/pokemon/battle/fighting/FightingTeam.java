package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

import java.util.List;
import java.util.Objects;

/** Ordered three-member free-fight team; member zero starts as the active fighter. */
public final class FightingTeam {

    /** Pokemon por equipo en Combate Libre (2 para que la pelea no sea tan larga). */
    public static final int SIZE = 2;
    static final int MIN_SIZE = 2;
    static final int MAX_SIZE = 3;

    private final String trainerName;
    private final List<Fighter> members;
    private int activeIndex;

    public FightingTeam(String trainerName, List<Pokemon> pokemon, double startX,
                        double groundY, boolean facingLeft) {
        if (pokemon == null || pokemon.size() < MIN_SIZE || pokemon.size() > MAX_SIZE) {
            throw new IllegalArgumentException("El equipo libre debe tener 2 o 3 Pokemon");
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

    /** Equipo del jefe secreto: un solo Pokemon. */
    static FightingTeam boss(String trainerName, Fighter boss) {
        boss.markAsBoss();
        return new FightingTeam(trainerName, List.of(boss));
    }

    private FightingTeam(String trainerName, List<Fighter> solo) {
        this.trainerName = Objects.requireNonNull(trainerName);
        this.members = List.copyOf(solo);
    }

    private FightingTeam(String trainerName, List<Fighter> members, boolean prototype) {
        if (members.size() < MIN_SIZE || members.size() > MAX_SIZE) {
            throw new IllegalArgumentException("El equipo debe tener 2 o 3 miembros");
        }
        this.trainerName = trainerName;
        this.members = List.copyOf(members);
    }

    public String getTrainerName() { return trainerName; }
    public List<Fighter> getMembers() { return members; }
    public Fighter getActive() { return members.get(activeIndex); }
    public int getActiveIndex() { return activeIndex; }
    public boolean hasAvailableFighter() {
        return members.stream().anyMatch(member -> !member.isDefeated());
    }

    /** Puts the other healthy member in front. False when there is nobody to switch with. */
    public boolean switchActive() {
        for (int i = 0; i < members.size(); i++) {
            if (i != activeIndex && !members.get(i).isDefeated()) {
                activeIndex = i;
                return true;
            }
        }
        return false;
    }

    public boolean advanceAfterDefeat() {
        if (!getActive().isDefeated()) return false;
        // Busca desde el siguiente y da la vuelta: tras un cambio en la pared
        // el que cae puede ser el ultimo de la lista y el vivo estar antes
        for (int step = 1; step < members.size(); step++) {
            int i = (activeIndex + step) % members.size();
            if (!members.get(i).isDefeated()) {
                activeIndex = i;
                return true;
            }
        }
        return false;
    }
}
