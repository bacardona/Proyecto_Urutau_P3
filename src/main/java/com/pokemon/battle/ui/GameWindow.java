package com.pokemon.battle.ui;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.Dimension;
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

    /** Muestra un error con el estilo retro; ESC o Enter vuelven a backScreen. */
    public void showError(String message, String backScreen) {
        loading.showError(message, () -> showScreen(backScreen));
        showScreen("loading");
    }
}