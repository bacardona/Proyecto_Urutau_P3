package com.pokemon.battle.fighting;

import com.pokemon.battle.MainGUI;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.ui.GameWindow;
import com.pokemon.battle.ui.SpriteLoader;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Scenery integration: main renders a preview; --window exercises the normal Swing entry point. */
public final class FreeFightStageCheck {
    private static int checks;
    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }

    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true);
        return field.get(object);
    }

    private static Pokemon pokemon(String name) {
        return new Pokemon(1, name, 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
    }

    private static FreeFightSetup setup() {
        var setup = new FreeFightSetup();
        setup.setPlayerOne(new Player(1, "JUGADOR 1", 0, 0),
                List.of(pokemon("BULBASAUR"), pokemon("SQUIRTLE"), pokemon("EEVEE")));
        setup.setPlayerTwo(new Player(2, "JUGADOR 2", 0, 0),
                List.of(pokemon("CHARMANDER"), pokemon("EEVEE"), pokemon("SQUIRTLE")));
        return setup;
    }

    private static BufferedImage image(String path) throws Exception {
        try (var input = FreeFightStageCheck.class.getResourceAsStream(path)) {
            check(input != null, "Missing classpath image " + path);
            var image = ImageIO.read(input); check(image != null, "Unsupported image " + path);
            return image;
        }
    }

    private static String digest(String path) throws Exception {
        try (var input = FreeFightStageCheck.class.getResourceAsStream(path)) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
        }
    }

    private static void assets() throws Exception {
        var background = image("/Escenario/Fondo.jpg");
        check(background.getWidth() == 735 && background.getHeight() == 490, "Background dimensions changed");
        check(digest("/Escenario/Fondo.jpg").equals("6ea726a562e5a0019cd3e4582637009b3444002dc7c2d2ef5d7ea877371abc86"),
                "Original background was modified");
        check(digest("/Escenario/Items_de_fondo.jpg").equals("77f2e3816ee5681a05915b6881bbc39aba01ac15c1858e6c73b603989d116097"),
                "Original atlas was modified");
        for (String name : new String[]{"terreno", "arbol_redondo", "arbol_pino", "arbusto_alto",
                "arbusto_ancho", "cesped", "flores_blancas", "flor_azul", "tronco"}) {
            var asset = image("/Escenario/decoraciones/" + name + ".png");
            check(asset.getColorModel().hasAlpha(), "Opaque crop " + name);
            int clear = 0, solid = 0, white = 0;
            for (int y = 0; y < asset.getHeight(); y++) for (int x = 0; x < asset.getWidth(); x++) {
                int rgb = asset.getRGB(x, y), alpha = rgb >>> 24;
                if (alpha == 0) clear++; else solid++;
                check(alpha == 0 || alpha == 255, "Generated halo/partial alpha in " + name);
                if (x == 0 || y == 0 || x == asset.getWidth() - 1 || y == asset.getHeight() - 1)
                    check(alpha == 0, "Background on crop border " + name);
                if (alpha == 255 && ((rgb >>> 16) & 255) > 200 && ((rgb >>> 8) & 255) > 195 && (rgb & 255) > 175)
                    white++;
            }
            check(clear > 0 && solid > 100, "Empty crop or rectangular background " + name);
            if (name.equals("flores_blancas")) check(white > 350, "White petals erased");
        }
        System.out.println("OK original JPG hashes, nine classpath PNGs, transparency, solid outlines and white petals");
    }

    private static String physics(FightingGameSession session) {
        var result = new StringBuilder();
        for (var p : session.getPlatforms()) result.append(p.getX()).append('/').append(p.getTop())
                .append('/').append(p.getWidth()).append('/').append(p.getHeight()).append(';');
        for (var f : session.getFighters()) result.append(f.getX()).append('/').append(f.getY())
                .append('/').append(f.getVelocityY()).append('/').append(f.getHealth()).append('/').append(f.getPokemonId());
        return result.toString();
    }

    private static BufferedImage stage(FightingGameSession session) {
        var image = new BufferedImage(session.getWidth(), session.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics(); new FightingStageRenderer().draw(g, session); g.dispose(); return image;
    }

    private static void scene() throws Exception {
        var session = new FightingGameSession(setup());
        String original = physics(session);
        var a = stage(session); var b = stage(session);
        for (int y = 0; y < 720; y++) for (int x = 0; x < 1280; x++) {
            check((a.getRGB(x, y) >>> 24) == 255, "Empty background edge");
            check(a.getRGB(x, y) == b.getRGB(x, y), "Scenery animates between paints");
        }
        check(original.equals(physics(session)), "Scenery changes gameplay state");
        check(session.getWidth() == 1280 && session.getHeight() == 720 && session.getGroundY() == 677,
                "Logical arena changed");
        check(session.getPlatforms().size() == 7, "Platform count changed");
        var platformDraw = FightingStageRenderer.class.getDeclaredMethod("drawPlatform", Graphics2D.class,
                Platform.class, int.class, boolean.class); platformDraw.setAccessible(true);
        for (var platform : session.getPlatforms()) {
            var single = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB); var g = single.createGraphics();
            platformDraw.invoke(new FightingStageRenderer(), g, platform, platform.getHeight(), false); g.dispose();
            int center = platform.getX() + platform.getWidth() / 2;
            check(single.getRGB(center, platform.getTop()) >>> 24 != 0, "Grass below physical surface");
            check(single.getRGB(center, platform.getTop() - 1) >>> 24 == 0, "Terrain above physical surface");
            check(single.getRGB(center, platform.getTop() + platform.getHeight()) >>> 24 == 0, "Ledge height changed");
        }
        var full = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB); var g = full.createGraphics();
        new FightingRenderer(SpriteLoader.shared()).draw(g, session, 0, () -> {}); g.dispose();
        ImageIO.write(full, "png", new File("target/combate-libre-escenario.png"));
        ImageIO.write(a, "png", new File("target/combate-libre-escenario-sin-hud.png"));
        check(original.equals(physics(session)), "Full renderer changes gameplay state");
        SwingUtilities.invokeAndWait(() -> {
            var panel = new FightingGamePanel(setup(), () -> {});
            for (var size : new Dimension[]{new Dimension(960, 540), new Dimension(1600, 900), new Dimension(1100, 800)}) {
                panel.setSize(size); var view = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
                var vg = view.createGraphics(); panel.paint(vg); vg.dispose();
                check(view.getRGB(size.width / 2, size.height / 2) != Color.BLACK.getRGB(), "Empty resized view");
            }
        });
        System.out.println("OK static full coverage, seven unchanged surfaces, aligned terrain, unchanged fighters and resized panel");
    }

    private static void key(JComponent component, String stroke) {
        var id = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(stroke));
        check(id != null, "Missing key " + stroke);
        component.getActionMap().get(id).actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "check"));
    }

    private static void awaitEdt(java.util.concurrent.Callable<Boolean> condition, String message) throws Exception {
        long deadline = System.nanoTime() + 20_000_000_000L;
        while (System.nanoTime() < deadline) {
            var result = new java.util.concurrent.FutureTask<Boolean>(condition); SwingUtilities.invokeAndWait(result);
            if (result.get()) return; Thread.sleep(40);
        }
        throw new AssertionError(message);
    }

    private static void capture(JComponent component, String name) throws Exception {
        var image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); component.paint(g); g.dispose();
        ImageIO.write(image, "png", new File("target/" + name + ".png"));
    }

    private static void window() throws Exception {
        final GameWindow[] window = {null}; final FightingGamePanel[] panel = {null};
        final FightingGameSession[] session = {null}; final Throwable[] failure = {null};
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { failure[0] = error; error.printStackTrace(); });
        MainGUI.main(new String[0]); SwingUtilities.invokeAndWait(() -> {});
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    for (Window candidate : Window.getWindows()) if (candidate instanceof GameWindow game && game.isShowing()) window[0] = game;
                    check(window[0] != null, "MainGUI did not start");
                    var screens = (Map<?, ?>) field(window[0], "screens"); key((JComponent) screens.get("intro"), "pressed ENTER");
                    window[0].showScreen("menu"); panel[0] = (FightingGamePanel) screens.get("fighting");
                    var setup = (FreeFightSetup) field(panel[0], "setup"); var chosen = setup();
                    setup.setPlayerOne(chosen.getPlayerOne(), chosen.getTeamOne()); setup.setPlayerTwo(chosen.getPlayerTwo(), chosen.getTeamTwo());
                    window[0].setGameSize(1280, 720); window[0].showScreen("fighting");
                    session[0] = (FightingGameSession) field(panel[0], "session");
                    check(panel[0].isShowing(), "Real fighting panel is hidden");
                    capture(panel[0], "combate-libre-escenario-ventana");
                    key(panel[0], "pressed J"); key(panel[0], "released J");
                } catch (Throwable e) { failure[0] = e; }
            });
            if (failure[0] != null) throw new AssertionError(failure[0]);
            awaitEdt(() -> !session[0].getProjectiles().isEmpty(), "Live projectile did not spawn");
            SwingUtilities.invokeAndWait(() -> {
                try { capture(panel[0], "combate-libre-escenario-proyectil"); key(panel[0], "pressed ESCAPE"); }
                catch (Throwable e) { failure[0] = e; }
            });
            String paused = physics(session[0]); Thread.sleep(180);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    check(session[0].getState() == FightingGameSession.State.PAUSED && paused.equals(physics(session[0])), "Pause advances physics");
                    capture(panel[0], "combate-libre-escenario-pausa");
                    for (var size : new Dimension[]{new Dimension(960, 540), new Dimension(1600, 900), new Dimension(1100, 800)}) {
                        window[0].setGameSize(size.width, size.height); capture(panel[0], "combate-libre-escenario-" + size.width + "x" + size.height);
                        check(paused.equals(physics(session[0])), "Resizing changes fighters/platforms");
                    }
                    // The production window is fixed-size. Temporarily allow maximize in this check only.
                    window[0].setResizable(true); window[0].setExtendedState(JFrame.MAXIMIZED_BOTH);
                } catch (Throwable e) { failure[0] = e; }
            });
            Thread.sleep(250);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    capture(panel[0], "combate-libre-escenario-maximizado");
                    check(paused.equals(physics(session[0])), "Maximize changes logical state");
                    window[0].setExtendedState(JFrame.NORMAL); window[0].setResizable(false); window[0].setGameSize(1280, 720);
                    key(panel[0], "pressed ESCAPE"); session[0].debugFillActiveEnergy(0); key(panel[0], "pressed R"); key(panel[0], "released R");
                } catch (Throwable e) { failure[0] = e; }
            });
            if (failure[0] != null) throw new AssertionError(failure[0]);
            awaitEdt(() -> session[0].getFighters().get(0).isEvolved() && !session[0].getFighters().get(0).isEvolutionAnimating(), "Live evolution failed");
            // The Ember used for the projectile preview can legitimately interrupt an ultimate.
            // Let it finish before checking a complete charge/release/return sequence.
            awaitEdt(() -> session[0].getProjectiles().isEmpty()
                    && !session[0].getFighters().get(0).isHitStunned(), "Preview projectile did not finish");
            SwingUtilities.invokeAndWait(() -> { session[0].debugFillActiveEnergy(0); key(panel[0], "pressed T"); key(panel[0], "released T"); });
            awaitEdt(() -> !session[0].getCombatSystem().getUltimateVisuals().isEmpty(), "Live ultimate failed");
            SwingUtilities.invokeAndWait(() -> {
                try { capture(panel[0], "combate-libre-escenario-ulti"); } catch (Throwable e) { failure[0] = e; }
            });
            awaitEdt(() -> !session[0].getFighters().get(0).isEvolved(), "Ultimate did not return to base");
            if (failure[0] != null) throw new AssertionError(failure[0]);
            check(session[0].getPlatforms().size() == 7, "Live platforms changed");
            System.out.println("OK MainGUI, live combat/projectile, pause, resize/maximize, evolution, ultimate and return");
        } finally { SwingUtilities.invokeAndWait(() -> { for (Window candidate : Window.getWindows()) candidate.dispose(); }); }
    }

    public static void main(String[] args) throws Exception {
        try {
            if (args.length > 0 && args[0].equals("--window")) window(); else { assets(); scene(); }
            System.out.println("PASS stage: " + checks + " checks");
        } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
        if (args.length > 0) System.exit(0);
    }
}
