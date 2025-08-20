package org.gtlcore.gtlcore.api.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public interface IAdditionalRecipeIterator {

    void setAdditionalRecipes(@NotNull List<@NotNull GTRecipe> additionalRecipes);

    void setAdditionalRecipesCanHandle(@NotNull Predicate<GTRecipe> canHandle);

    boolean hasAdditionalRecipes();

    @Nullable
    List<GTRecipe> getAdditionalRecipes();

    void setUseDiveIngredientTreeFind(boolean useDiveIngredientTreeFind);
}
