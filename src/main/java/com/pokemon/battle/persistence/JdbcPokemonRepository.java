package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcPokemonRepository implements PokemonRepository {

    private static final String CALL_LOAD_ALL_POKEMON =
            "{ call pokken__listar_todos(?) }";

    private final MoveRepository moveRepository;

    public JdbcPokemonRepository(MoveRepository moveRepository) {
        this.moveRepository = moveRepository;
    }

    @Override
    public List<Pokemon> loadAll() {
        List<Move> allMoves = moveRepository.loadAll();
        Map<String, Move> movesById = new HashMap<>();
        for (Move m : allMoves) {
            movesById.put(m.getId(), m);
        }

        List<Pokemon> pokemons = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(CALL_LOAD_ALL_POKEMON)) {

            stmt.registerOutParameter(1, Types.REF_CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    int id = rs.getInt("id_pokken");
                    String name = rs.getString("nombre_pokken");
                    int hp = rs.getInt("hp");
                    int attack = rs.getInt("atk");
                    int defense = rs.getInt("def");
                    int spAttack = rs.getInt("atk_sp");
                    int spDefense = rs.getInt("def_sp");
                    int speed = rs.getInt("spd");
                    Type primaryType = Type.valueOf(rs.getString("tipo1").toUpperCase());

                    String rawSecondary = rs.getString("tipo2");
                    Type secondaryType = (rawSecondary != null && !rawSecondary.isBlank())
                            ? Type.valueOf(rawSecondary.toUpperCase())
                            : Type.NONE;

                    List<Move> pokemonMoves = new ArrayList<>(4);
                    addMoveIfPresent(pokemonMoves, movesById, rs.getInt("ataque1"), rs.wasNull());
                    addMoveIfPresent(pokemonMoves, movesById, rs.getInt("ataque2"), rs.wasNull());
                    addMoveIfPresent(pokemonMoves, movesById, rs.getInt("ataque3"), rs.wasNull());
                    addMoveIfPresent(pokemonMoves, movesById, rs.getInt("ataque4"), rs.wasNull());

                    Pokemon pokemon = new Pokemon(
                            id, name, hp, attack, defense, spAttack, spDefense, speed,
                            primaryType, secondaryType, pokemonMoves
                    );
                    pokemon.setTotalStats(rs.getInt("total_stats"));

                    pokemons.add(pokemon);
                }
            }

        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar Pokémon en Oracle BD");
        }

        return pokemons;
    }

    private void addMoveIfPresent(List<Move> target, Map<String, Move> movesById, int moveId, boolean wasNull) {
        if (!wasNull) {
            Move move = movesById.get(String.valueOf(moveId));
            if (move != null) {
                target.add(move);
            }
        }
    }

    @Override
    public void saveAll(List<Pokemon> pokemonList) {
        // Si tu juego requiere guardar cambios en la BD, aquí implementas un INSERT / UPDATE en lote (batch)
    }
}