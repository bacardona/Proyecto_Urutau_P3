package com.pokemon.battle.service;

import com.pokemon.battle.model.*;
import java.util.ArrayList;
import java.util.List;

/** Database-free regression for the catalog/events conflict resolved during the develop merge. */
public final class DevelopMergeCheck {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static final RandomProvider RANDOM = new RandomProvider() {
        public int nextInt(int min, int max) { return min; }
        public double nextDouble(double min, double max) { return min; }
    };
    private static final Move HIT = new DamageMove("TACKLE", "Placaje", Type.NORMAL,
            MoveCategory.PHYSICAL, 20, 100, 35, 0);
    private static final Move GROWTH = new StatChangeMove("GROWTH", "Desarrollo", Type.NORMAL,
            100, 20, 0, StatType.ATTACK, 1, true);
    private static final Move FLY = new TwoTurnMove("FLY", "Vuelo", Type.FLYING,
            MoveCategory.PHYSICAL, 30, 100, 15, 1, "se eleva", true);

    private static Pokemon pokemon(int id, String name) {
        return new Pokemon(id, name, 100, 60, 60, 60, 60, 60,
                Type.NORMAL, Type.NONE, List.of(HIT, GROWTH, FLY));
    }

    private static final class Fixture {
        final Player one = new Player(1, "Player one", 0, 0);
        final Player two = new Player(2, "Player two", 0, 0);
        final Machine machine = new Machine(3, "CPU", "NORMAL");
        final List<Pokemon> normal = List.of(pokemon(1, "Bulbasaur"), pokemon(4, "Charmander"),
                pokemon(7, "Squirtle"), pokemon(133, "Eevee"));
        final List<Pokemon> expanded = new ArrayList<>(normal);
        Trainer winner, loser;
        int registrations;
        final BattleController controller;

        Fixture() {
            expanded.add(pokemon(63, "Abra"));
            var catalog = new PokemonCatalog() {
                public List<Pokemon> listAll() { return normal; }
                public List<Pokemon> listAllWithFreeFight() { return List.copyOf(expanded); }
                public Pokemon findByName(String name) {
                    return expanded.stream().filter(p -> p.getName().equalsIgnoreCase(name)).findFirst().orElseThrow();
                }
            };
            var trainers = new TrainerCatalog() {
                public List<Player> listPlayers() { return List.of(one, two); }
                public List<Machine> listMachines() { return List.of(machine); }
                public Player createPlayer(String name) { return new Player(4, name, 0, 0); }
            };
            var registry = new CombatRegistry() {
                public int register(Trainer winning, Trainer losing) {
                    winner = winning; loser = losing; registrations++; return 42;
                }
                public List<CombatHistoryEntry> historyOf(Trainer trainer) { return List.of(); }
            };
            controller = new BattleController(catalog, new BattleService(RANDOM, (type, target) -> 1),
                    trainers, registry, new RandomMachineStrategy(RANDOM));
        }
        List<Pokemon> team() { return normal.subList(0, 3); }
    }

    private static void events(BattleController controller, List<String> messages, int expected) {
        check(controller.getLastRoundEvents().size() == expected, "Wrong attack-event count");
        int previous = -1;
        for (var event : controller.getLastRoundEvents()) {
            check(event.getMessageIndex() >= 0 && event.getMessageIndex() < messages.size(), "Event outside message list");
            check(event.getMessageIndex() >= previous, "Events out of order");
            check(event.getAttacker() != null && event.getDefender() != null, "Missing animation participant");
            previous = event.getMessageIndex();
        }
    }

    private static void pvp() {
        var fixture = new Fixture(); var controller = fixture.controller;
        check(controller.listPokemon().size() == 4, "Normal catalog includes free-fight-only species");
        check(controller.listPokemonWithFreeFight().stream().anyMatch(p -> p.getName().equals("Abra")),
                "Expanded free-fight catalog lost in merge");
        var battle = controller.startBattleVsPlayer(fixture.one, fixture.team(), fixture.two, fixture.team());
        check(battle.getFirst().getTeam().getActive().getPokemon().getLevel() == 50, "develop level change lost");
        var messages = controller.playRound(TurnAction.attack(HIT), TurnAction.attack(HIT));
        events(controller, messages, 2);
        check(controller.getLastRoundEvents().stream().allMatch(e -> e.getDefenderHpAfter() < e.getDefenderHpBefore()),
                "Damage not captured for animations");

        messages = controller.playRound(TurnAction.switchTo(1), TurnAction.attack(HIT));
        events(controller, messages, 1);
        check(controller.getLastRoundEvents().get(0).getMessageIndex() > 1, "Switch messages not included in event offset");
        messages = controller.playRound(TurnAction.attack(FLY), TurnAction.attack(HIT));
        events(controller, messages, 2);
        check(controller.getLastRoundEvents().get(0).getKind() == MoveEvent.Kind.CHARGE, "Fly charge missing");
        check(controller.getLastRoundEvents().get(0).isInvulnerable(), "Charging sprite should hide");
        check(controller.getLastRoundEvents().get(1).isMissed(), "Attack against flying Pokemon should miss");
        var forced = controller.getForcedAction(battle.getFirst());
        check(forced != null && forced.getMove() == FLY, "Two-turn action no longer forced");
        messages = controller.playRound(forced, TurnAction.attack(HIT));
        events(controller, messages, 2);
        check(controller.getLastRoundEvents().get(0).getKind() == MoveEvent.Kind.RELEASE, "Fly release missing");
        check(!controller.getLastRoundEvents().get(0).isInvulnerable(), "Released sprite remains hidden");

        messages = controller.playRound(TurnAction.attack(GROWTH), TurnAction.attack(GROWTH));
        events(controller, messages, 2);
        check(controller.getLastRoundEvents().stream().allMatch(MoveEvent::isSelfTarget), "Buff effect targets rival visually");
        messages = controller.playRound(TurnAction.switchTo(0), TurnAction.switchTo(1));
        events(controller, messages, 0);
        controller.surrender(battle.getSecond());
        check(controller.isFinished() && controller.registerResult() == 42, "PvP result missing");
        check(fixture.registrations == 1 && fixture.winner == fixture.one && fixture.loser == fixture.two, "Wrong result participants");
        check(fixture.one.getBattlesWon() == 1 && fixture.two.getBattlesLost() == 1, "In-memory counters not updated once");
    }

    private static void pve() {
        var fixture = new Fixture(); var controller = fixture.controller;
        var battle = controller.startBattleVsMachine(fixture.one, fixture.team(), fixture.machine);
        check(battle.getSecond().getTeam().getMembers().stream()
                .noneMatch(c -> c.getName().equals("Abra")), "CPU uses free-fight-only catalog");
        var messages = controller.playRound(TurnAction.attack(HIT), controller.chooseMachineAction());
        events(controller, messages, 2);
        controller.surrender(battle.getFirst());
        check(controller.registerResult() == 42 && fixture.winner == fixture.machine && fixture.loser == fixture.one,
                "Wrong PvE result");
        check(fixture.one.getBattlesLost() == 1 && fixture.registrations == 1, "PvE counter not updated once");
    }

    public static void main(String[] args) {
        pvp(); pve();
        System.out.println("PASS develop integration: " + checks + " checks (catalogs, level 50, PvP/PvE, events, Fly, counters)");
    }
}
