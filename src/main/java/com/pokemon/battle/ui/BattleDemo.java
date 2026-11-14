package com.pokemon.battle.ui;

import com.pokemon.battle.model.*;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class BattleDemo {

    private static final RandomProvider RANDOM = new RandomProvider() {
        @Override
        public int nextInt(int min, int max) {
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }

        @Override
        public double nextDouble(double min, double max) {
            return ThreadLocalRandom.current().nextDouble(min, max);
        }
    };

    // Tabla de tipos de prueba: todo hace dano normal
    private static final TypeEffectiveness FLAT = (attackType, target) -> 1.0;

    private BattleDemo() {
    }

    public static JPanel create() {
        Pokemon squirtle = new Pokemon(7, "Squirtle", 44, 48, 65, 50, 64, 43,
                Type.WATER, Type.NONE, List.of(
                new DamageMove("1", "Placaje", Type.NORMAL, MoveCategory.PHYSICAL, 40, 100, 35, 0),
                new DamageMove("2", "Pistola Agua", Type.WATER, MoveCategory.SPECIAL, 40, 100, 25, 0),
                new StatChangeMove("3", "Refugio", Type.WATER, 100, 40, 0, StatType.DEFENSE, 1, true)));

        Pokemon bulbasaur = new Pokemon(1, "Bulbasaur", 45, 49, 49, 65, 65, 45,
                Type.GRASS, Type.POISON, List.of(
                new DamageMove("4", "Placaje", Type.NORMAL, MoveCategory.PHYSICAL, 40, 100, 35, 0)));

        Combatant player = new Combatant(squirtle);
        Combatant rival = new Combatant(bulbasaur);
        // El rival empieza dormido, como en tu imagen
        StatusCondition.SLEEP.applyTo(rival, new BattleContext(RANDOM, FLAT, new BattleLog()));

        BattleScreenForm form = new BattleScreenForm(player, rival);
        form.setOnMoveChosen(index -> {
            BattleLog log = new BattleLog();
            BattleContext ctx = new BattleContext(RANDOM, FLAT, log);
            form.setInputEnabled(false);

            player.getPokemon().getMove(index).execute(player, rival, ctx);
            if (!rival.isFainted() && !rival.getStatus().preventsAction(rival, ctx)) {
                rival.getPokemon().getMove(0).execute(rival, player, ctx);
            }
            if (rival.isFainted()) {
                ctx.log(rival.getName() + " se ha debilitado.");
            }
            if (player.isFainted()) {
                ctx.log(player.getName() + " se ha debilitado.");
            }

            form.refresh();
            form.showMessages(log.getMessages());

            if (!rival.isFainted() && !player.isFainted()) {
                Timer back = new Timer(2500, e -> {
                    form.showPrompt();
                    form.setInputEnabled(true);
                });
                back.setRepeats(false);
                back.start();
            }
        });
        return form.getRootPanel();
    }
}