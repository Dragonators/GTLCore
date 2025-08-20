package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface IMEPatternPartMachine {

    // every element should not be Null
    @NotNull
    List<@NotNull GTRecipe> getCachedGTRecipe();

    void setRecipe(int index, GTRecipe recipe);

    void setCache(Map<GTRecipe, IRecipeHandlePart> map, MERecipeHandlePart mePart);

    MEPatternBufferPartMachine.InternalSlot[] getInternalInventory();

    boolean hasCacheInSlot(int slot);

    List<IMERecipeHandlerTrait<?>> getMERecipeHandlerTraits();
}
