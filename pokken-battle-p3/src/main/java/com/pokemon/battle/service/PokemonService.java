package com.pokemon.battle.service;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.persistence.PokemonReader;
import com.pokemon.battle.validation.PokemonValidator;

import java.util.List;

/**
 * Use cases related to the Pokemon catalog.
 */
public class PokemonService implements PokemonCatalog {

    private final PokemonReader reader;

    /**
     * @param reader read access to the stored Pokemon (injected)
     */
    public PokemonService(PokemonReader reader) {
        this.reader = reader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pokemon> listAll() {
        return reader.loadAll();
    }

    /**
     * Finds a Pokemon by name, ignoring case.
     *
     * @param name name to search
     * @return the matching Pokemon
     * @throws com.pokemon.battle.exceptions.InvalidDataException      when the name is blank
     * @throws com.pokemon.battle.exceptions.BusinessRuleException     when there is no data loaded
     * @throws com.pokemon.battle.exceptions.ResourceNotFoundException when it does not exist
     */
    @Override
    public Pokemon findByName(String name) {
        PokemonValidator.validateName(name);
        List<Pokemon> all = reader.loadAll();
        // Un catalogo vacio casi siempre significa que falta el archivo de datos
        if (all.isEmpty()) {
            throw new BusinessRuleException(
                    "No hay Pokemon cargados. Verifique que exista el archivo data/pokemon.csv "
                            + "y ejecute el juego desde la carpeta raiz del proyecto");
        }
        Pokemon found = null;
        for (Pokemon pokemon : all) {
            if (pokemon.getName().equalsIgnoreCase(name.trim())) {
                found = pokemon;
                break;
            }
        }
        PokemonValidator.validatePokemonExists(found, name);
        return found;
    }
}
