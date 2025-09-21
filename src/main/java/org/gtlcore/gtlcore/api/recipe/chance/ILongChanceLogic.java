package org.gtlcore.gtlcore.api.recipe.chance;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@FunctionalInterface
public interface ILongChanceLogic {

    @Nullable
    @Unmodifiable
    List<@NotNull Content> roll(
                                @NotNull @Unmodifiable List<@NotNull Content> chancedEntries,
                                @NotNull ChanceBoostFunction boostFunction,
                                int baseTier, int machineTier,
                                @Nullable Object2IntMap<?> cache, long times,
                                RecipeCapability<?> cap);
}
