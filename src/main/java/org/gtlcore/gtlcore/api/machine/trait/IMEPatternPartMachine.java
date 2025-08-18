package org.gtlcore.gtlcore.api.machine.trait;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import java.util.List;

public interface IMEPatternPartMachine {

    List<GTRecipe> getRecipe();

    default void setRecipe(int index, GTRecipe recipe) {}

    List<IMERecipeHandlerTrait<?>> getMERecipeHandlerTraits();
}
