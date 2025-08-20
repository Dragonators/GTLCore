package org.gtlcore.gtlcore.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.function.Predicate;

/**
 * @author Dragonators
 */
public interface IRecipeHandlePart {

    IO getHandlerIO();

    <T extends Predicate<S>, S> Object2LongMap<S> getContent(RecipeCapability<T> cap);
}
