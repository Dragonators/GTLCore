package org.gtlcore.gtlcore.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IMERecipeHandler<T> extends IRecipeHandler<T> {

    List<Object> getLimitContents(int slot);

    // Active means this slot contains pattern && pattern contains CAP
    List<Integer> getActiveSlots(RecipeCapability<?> cap);

    List<List<Object>> getActiveLimitContents();

    Int2ObjectArrayMap<List<Object>> getActiveLimitContentsMap();

    Object2LongOpenHashMap<AEKey> getActiveMEContentsMap();

    Object2LongOpenHashMap<?> getActiveSlotsContentsMap();

    Object2LongOpenHashMap<?> getCustomSlotsStackMap(List<Integer> slots);

    default List<Integer> meHandleRecipe(IO io, GTRecipe recipe, List<?> left, @Nullable String slotName, boolean simulate, List<Integer> trySlots) {
        List<T> contents = new ObjectArrayList<>(left.size());

        for (Object leftObj : left) {
            contents.add(this.copyContent(leftObj));
        }
        return meHandleRecipeInner(io, recipe, contents, slotName, simulate, trySlots);
    }

    List<Integer> meHandleRecipeInner(IO var1, GTRecipe var2, List<?> var3, @Nullable String var4, boolean var5, List<Integer> var6);
}
