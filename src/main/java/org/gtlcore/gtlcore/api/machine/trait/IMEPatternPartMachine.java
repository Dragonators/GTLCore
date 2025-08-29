package org.gtlcore.gtlcore.api.machine.trait;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface IMEPatternPartMachine extends IMEIOPartMachine {

    // every element should not be Null
    @NotNull
    List<@NotNull GTRecipe> getCachedGTRecipe();

    void setSlotCacheRecipe(int index, GTRecipe recipe);

    void restoreMachineCache(Map<GTRecipe, IRecipeHandlePart> map, MERecipeHandlePart mePart);

    boolean hasCacheInSlot(int slot);
}
