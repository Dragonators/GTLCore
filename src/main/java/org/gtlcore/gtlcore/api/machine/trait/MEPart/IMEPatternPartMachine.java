package org.gtlcore.gtlcore.api.machine.trait.MEPart;

import org.gtlcore.gtlcore.utils.DisjointSetMap;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public interface IMEPatternPartMachine extends IMETraitIOPartMachine {

    // every element should not be Null
    @NotNull
    ObjectSet<@NotNull GTRecipe> getCachedGTRecipe();

    void setSlotCacheRecipe(int index, GTRecipe recipe);

    void restoreSlotMap(DisjointSetMap<GTRecipe, Integer> recipe2SlotsMap, Consumer<Integer> removeMapOnSlot);

    boolean hasCacheInSlot(int slot);
}
