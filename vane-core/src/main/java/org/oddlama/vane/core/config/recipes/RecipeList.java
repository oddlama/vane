package org.oddlama.vane.core.config.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.oddlama.vane.core.config.ConfigDictSerializable;

public class RecipeList implements ConfigDictSerializable {

    private List<RecipeDefinition> recipes = new ArrayList<>();

    public RecipeList() {}

    public RecipeList(List<RecipeDefinition> recipes) {
        this.recipes = recipes;
    }

    public List<RecipeDefinition> recipes() {
        return recipes;
    }

    public Map<String, Object> to_dict() {
        return recipes.stream().collect(Collectors.toMap(RecipeDefinition::name, RecipeDefinition::to_dict));
    }

    public void from_dict(final Map<String, Object> dict) {
        recipes.clear();
        for (final var e : dict.entrySet()) {
            recipes.add(RecipeDefinition.from_dict(e.getKey(), e.getValue()));
        }
    }

    public static RecipeList of(RecipeDefinition... defs) {
        final var rl = new RecipeList();
        rl.recipes = Arrays.asList(defs);
        return rl;
    }
}
