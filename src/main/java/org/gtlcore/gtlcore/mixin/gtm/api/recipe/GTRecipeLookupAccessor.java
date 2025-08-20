package org.gtlcore.gtlcore.mixin.gtm.api.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GTRecipeLookup.class)
public interface GTRecipeLookupAccessor {

    @Accessor(value = "recipeType", remap = false)
    GTRecipeType getRecipeType();
}
