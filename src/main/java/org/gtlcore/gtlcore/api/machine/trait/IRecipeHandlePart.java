package org.gtlcore.gtlcore.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

public interface IRecipeHandlePart {

    IO getHandlerIO();

    Object2LongMap<?> getContent(RecipeCapability<?> cap);
}
