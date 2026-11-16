package com.pokemon.battle.fighting;

import javax.swing.SwingUtilities;
import java.util.concurrent.FutureTask;

/** Wait for the real presentation instead of sending inputs that it deliberately discards. */
final class FightingWindowChecks {
    private FightingWindowChecks() { }

    static void awaitGameplay(FightingGamePanel panel) throws Exception {
        awaitCondition(() -> {
            var renderer = FightingGamePanel.class.getDeclaredField("renderer");
            renderer.setAccessible(true);
            return panel.isShowing() && !((FightingRenderer) renderer.get(panel)).fx().director().freezesSimulation();
        }, "Combat presentation did not finish");
    }

    static void awaitCondition(java.util.concurrent.Callable<Boolean> condition, String message) throws Exception {
        long deadline = System.nanoTime() + 20_000_000_000L;
        while (System.nanoTime() < deadline) {
            var ready = new FutureTask<>(condition);
            SwingUtilities.invokeAndWait(ready);
            if (ready.get()) return;
            Thread.sleep(40);
        }
        throw new AssertionError(message);
    }
}
