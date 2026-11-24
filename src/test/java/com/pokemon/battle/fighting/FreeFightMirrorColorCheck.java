package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.awt.image.BufferedImage;
import java.util.List;

/** Checks that player 2 gets alternate colors only for Pokemon that player 1 also has. Run with main. */
public class FreeFightMirrorColorCheck {

    private static final String[] ALL = {"PIKACHU", "CHARMANDER", "SQUIRTLE", "BULBASAUR", "EEVEE",
            "GASTLY", "MACHOP", "ABRA", "VULPIX", "SANDSHREW"};
    private static final String[] EVOLVED = {"RAICHU", "CHARMELEON", "WARTORTLE", "IVYSAUR", "FLAREON",
            "HAUNTER", "MACHOKE", "KADABRA", "NINETALES", "SANDSLASH"};

    public static void main(String[] args) {
        // Mismo Pokemon en los dos equipos: solo el del jugador 2 cambia de color
        FightingGameSession s = session(List.of("PIKACHU", "EEVEE"), List.of("PIKACHU", "SQUIRTLE"));
        check(!s.getPlayerOneTeam().getMembers().get(0).usesAltColors(), "Player 1 keeps normal colors");
        check(s.getPlayerTwoTeam().getMembers().get(0).usesAltColors(), "Repeated Pokemon of player 2 uses alt colors");
        check(!s.getPlayerTwoTeam().getMembers().get(1).usesAltColors(), "Unique Pokemon keeps normal colors");

        // Sin repetidos: nadie cambia
        FightingGameSession plain = session(List.of("ABRA", "EEVEE"), List.of("GASTLY", "SQUIRTLE"));
        for (Fighter f : plain.getPlayerTwoTeam().getMembers()) check(!f.usesAltColors(), "No mirror, no alt colors");

        // Cada Pokemon y su evolucion tienen colores alternos que si cambian el sprite
        for (String id : ALL) check(AltPalette.hasPalette(id), "Missing palette: " + id);
        for (String id : EVOLVED) check(AltPalette.hasPalette(id), "Missing palette: " + id);
        BufferedImage sample = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        sample.setRGB(0, 0, 0xFFF8D030);   // amarillo Pikachu
        sample.setRGB(1, 0, 0x00000000);   // transparente
        BufferedImage alt = AltPalette.recolor("PIKACHU", sample);
        check(alt.getRGB(0, 0) != sample.getRGB(0, 0), "Yellow must change for player 2");
        check(alt.getRGB(1, 0) == 0, "Transparent pixels stay transparent");
        check(AltPalette.recolor("PIKACHU", sample) == alt, "Recolored frames are cached");
        check(AltPalette.recolor("MEWTWO", sample) == sample, "Pokemon without palette stay the same");

        System.out.println("PASS mirror colors");
    }

    private static FightingGameSession session(List<String> one, List<String> two) {
        FreeFightSetup setup = new FreeFightSetup();
        setup.setPlayerOne(new Player(1, "A", 0, 0), one.stream().map(FreeFightMirrorColorCheck::p).toList());
        setup.setPlayerTwo(new Player(2, "B", 0, 0), two.stream().map(FreeFightMirrorColorCheck::p).toList());
        return new FightingGameSession(setup);
    }

    private static Pokemon p(String name) {
        return new Pokemon(1, name, 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
