package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcPokemonRepository implements PokemonRepository {

    private final MoveRepository moveRepository;

    public JdbcPokemonRepository(MoveRepository moveRepository) {
        this.moveRepository = moveRepository;
    }

    @Override
    public List<Pokemon> loadAll() {
        List<Move> allMoves = moveRepository.loadAll();
        List<Pokemon> pokemons = new ArrayList<>();


        String sql = "SELECT id_pokken, nombre_pokken, hp, atk, def, atk_sp, def_sp, spd, tipo1, tipo2, ataque1, ataque2,ataque3,ataque4 FROM POKKEN";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

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

                String attk1 = String.valueOf(rs.getInt("ataque1"))  ;
                String attk2 = String.valueOf(rs.getInt("ataque2"));
                String attk3 = String.valueOf(rs.getInt("ataque3"));
                String attk4 = String.valueOf(rs.getInt("ataque4"));

                List<Move> pokemonMoves = new ArrayList<>(); // o buscar en una tabla intermedia pokemon_moves
                for (Move m : allMoves){
                    if (m.getId().equals(attk1)
                    || m.getId().equals(attk2)
                    || m.getId().equals(attk3)
                    || m.getId().equals(attk4)){
                        pokemonMoves.add(m);
                    }
                }


                pokemons.add(new Pokemon(
                        id, name, hp, attack, defense, spAttack, spDefense, speed,
                        primaryType, secondaryType, pokemonMoves
                ));
            }

        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar Pokémon en Oracle BD", e);
        }

        return pokemons;
    }

    @Override
    public void saveAll(List<Pokemon> pokemonList) {
        // Si tu juego requiere guardar cambios en la BD, aquí implementas un INSERT / UPDATE en lote (batch)
    }
}