package com.pokemon.battle.ui;

import com.pokemon.battle.gamepad.GameAction;
import com.pokemon.battle.gamepad.GamepadInput;
import com.pokemon.battle.gamepad.InputProfile;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GameWindow extends JFrame {

    // Tiempo minimo de la pantalla de carga, para que no parpadee si todo es rapido
    private static final long MIN_LOADING_MS = 700;

    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);
    private final Map<String, JComponent> screens = new HashMap<>();
    private final LoadingScreen loading = new LoadingScreen();


    public GameWindow() {
        setTitle("Pokken Battle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        container.setPreferredSize(new Dimension(960, 640));
        add(container);
        addScreen("loading", loading);
        gamepad.setListener(this::onGamepadAction);
        gamepad.start();   // si no hay mando o falla, el juego sigue con teclado
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { gamepad.stop(); }
        });
        pack();
        setLocationRelativeTo(null);
    }
    public void setGameSize(int width, int height) {

        container.setPreferredSize(
                new Dimension(width, height)
        );

        pack();

        // Centrar la ventana nuevamente
        setLocationRelativeTo(null);
    }


    // Si ya existia una pantalla con ese nombre, la reemplaza
    public void addScreen(String name, JComponent screen) {
        JComponent old = screens.put(name, screen);
        if (old != null) {
            container.remove(old);
        }
        container.add(screen, name);
        container.revalidate();
    }

    public void showScreen(String name) {
        cards.show(container, name);
        currentScreen = name;
        gamepad.setProfile(PROFILES.getOrDefault(name, InputProfile.MENU));
    }

    private void onGamepadAction(GameAction action) {
        JComponent screen = screens.get(currentScreen);
        if (!(screen instanceof RetroScreen retro)) return;

        String key = switch (action) {
            case UP -> "UP";
            case DOWN -> "DOWN";
            case LEFT -> "LEFT";
            case RIGHT -> "RIGHT";
            case CONFIRM, ANY_BUTTON -> "ENTER";
            case BACK -> "ESCAPE";
            default -> null;   // REMOVE_LAST, PAGE_PREV, PAGE_NEXT: pendientes
        };
        if (key != null) retro.pressKey(key);
    }

    /**
     * Muestra la pantalla de carga mientras "work" se ejecuta en segundo plano
     * (lecturas a la base de datos). Al terminar llama a onDone en el hilo de la
     * ventana; si falla, llama a onError.
     */
    public <T> void runWithLoading(String message, Supplier<T> work,
                                   Consumer<T> onDone, Consumer<Throwable> onError) {
        loading.setMessage(message);
        showScreen("loading");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                long start = System.currentTimeMillis();
                T result = work.get();
                long left = MIN_LOADING_MS - (System.currentTimeMillis() - start);
                if (left > 0) {
                    Thread.sleep(left);
                }
                return result;
            }

            @Override
            protected void done() {
                T result;
                try {
                    result = get();
                } catch (ExecutionException e) {
                    onError.accept(e.getCause() != null ? e.getCause() : e);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onError.accept(e);
                    return;
                }
                try {
                    onDone.accept(result);
                } catch (RuntimeException e) {
                    onError.accept(e);
                }
            }
        }.execute();
    }

    private final GamepadInput gamepad = new GamepadInput();
    private String currentScreen;

    // Perfil del mando por pantalla (las que no estén aquí usan MENU)
    private static final Map<String, InputProfile> PROFILES = Map.ofEntries(
            Map.entry("intro", InputProfile.CUALQUIER_BOTON),
            Map.entry("loading", InputProfile.CUALQUIER_BOTON),
            Map.entry("menu", InputProfile.MENU),
            Map.entry("combatMenu", InputProfile.MENU),
            Map.entry("pokedex", InputProfile.LISTA),
            Map.entry("history", InputProfile.LISTA),
            Map.entry("historyView", InputProfile.LISTA),
            Map.entry("pveHub", InputProfile.LISTA),
            Map.entry("pvpHub1", InputProfile.LISTA),
            Map.entry("pvpHub2", InputProfile.LISTA),
            Map.entry("pveTeam", InputProfile.EQUIPO),
            Map.entry("pvpTeam1", InputProfile.EQUIPO),
            Map.entry("pvpTeam2", InputProfile.EQUIPO));


    /** Muestra un error con el estilo retro; ESC o Enter vuelven a backScreen. */
    public void showError(String message, String backScreen) {
        loading.showError(message, () -> showScreen(backScreen));
        showScreen("loading");
    }
}