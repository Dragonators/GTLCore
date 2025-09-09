package org.gtlcore.gtlcore.api.machine.trait;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.google.common.collect.BiMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public interface IMEPatternPartMachine extends IMEIOPartMachine {

    // every element should not be Null
    @NotNull
    List<@NotNull GTRecipe> getCachedGTRecipe();

    void setSlotCacheRecipe(int index, GTRecipe recipe);

    void restoreSlotMap(BiMap<GTRecipe, Integer> slotMap, Consumer<Integer> removeMapOnSlot);

    boolean hasCacheInSlot(int slot);
}
