package org.gtlcore.gtlcore.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IFilteredHandler;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IMERecipeHandler<T> extends IFilteredHandler<T> {

    // Active means this slot contains pattern && pattern contains CAP
    List<Integer> getActiveSlots(RecipeCapability<?> cap);

    // Object is
    // Ingredient -> ItemStack
    // FluidIngredient -> FluidStack
    Int2ObjectMap<List<Object>> getActiveSlotsLimitContentsMap();

    Object2LongMap<?> getCustomSlotsStackMap(List<Integer> slots);

    // try slot as distinct slot to handle full left Content
    // a meHandler must initContents before calling this method
    // if !simulate, it will consume the contents
    default boolean meHandleRecipe(IO io, GTRecipe recipe, @Nullable String slotName, boolean simulate, int trySlot) {
        return meHandleRecipeInner(io, recipe, getPreparedMEHandleContents(), slotName, simulate, trySlot);
    }

    boolean meHandleRecipeInner(IO var1, GTRecipe var2, Object2LongMap<?> var3, @Nullable String var4, boolean var5, int var6);

    default void initMEHandleContents(List<?> left) {
        if (left.isEmpty()) return;

        List<T> contents = new ObjectArrayList<>(left.size());
        for (Object leftObj : left) {
            contents.add(this.copyContent(leftObj));
        }

        prepareMEHandleContents(contents);
    }

    RecipeCapability<T> getCapability();

    default T copyContent(Object content) {
        return getCapability().copyInner((T) content);
    }

    void prepareMEHandleContents(List<T> contents);

    Object2LongMap<?> getPreparedMEHandleContents();
}
