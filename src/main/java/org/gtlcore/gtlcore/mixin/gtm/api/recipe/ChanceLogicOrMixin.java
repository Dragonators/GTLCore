package org.gtlcore.gtlcore.mixin.gtm.api.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

import static org.gtlcore.gtlcore.api.recipe.IAdvancedContentModifier.preciseDivision;

@Mixin(targets = "com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic$1")
public abstract class ChanceLogicOrMixin extends ChanceLogic {

    public ChanceLogicOrMixin(String id) {
        super(id);
    }

    @Unique
    private static int gTLCore$getChance(@NotNull Content entry, @NotNull ChanceBoostFunction boostFunction, int baseTier, int machineTier) {
        return boostFunction.getBoostedChance(entry, baseTier, machineTier);
    }

    @Unique
    private static boolean gTLCore$passesChance(int chance, int maxChance) {
        return chance >= maxChance;
    }

    @Unique
    private static int gTLCore$getCachedChance(Content entry, @Nullable Object2IntMap<?> cache) {
        return cache != null && cache.containsKey(entry.content) ? cache.getInt(entry.content) : GTValues.RNG.nextInt(entry.maxChance);
    }

    @Unique
    @SuppressWarnings("all")
    private static void gTLCore$updateCachedChance(Object ingredient, @Nullable Object2IntMap<?> cache, int chance) {
        if (cache != null) {
            ((Object2IntMap) cache).put(ingredient, chance);
        }
    }

    /**
     * @author Dragons
     * @reason 修复1/Parallel导致的精度问题
     */
    @Overwrite(remap = false)
    public @Nullable List<Content> roll(
                                        List<Content> chancedEntries,
                                        @NotNull ChanceBoostFunction boostFunction,
                                        int baseTier,
                                        int machineTier,
                                        @Nullable Object2IntMap<?> cache,
                                        int times,
                                        RecipeCapability<?> cap) {
        List<Content> out = new ObjectArrayList<>(chancedEntries.size());

        for (Content entry : chancedEntries) {
            int maxChance = entry.maxChance;

            int newChance = gTLCore$getChance(entry, boostFunction, baseTier, machineTier);
            int totalChance = times * newChance;
            int guaranteed = totalChance / maxChance;
            if (guaranteed > 0) out.add(entry.copy(cap, preciseDivision(guaranteed, times)));
            newChance = totalChance % maxChance;

            int cached = gTLCore$getCachedChance(entry, cache);
            int chance = newChance + cached;
            if (gTLCore$passesChance(chance, maxChance)) {
                do {
                    out.add(entry.copy(cap, preciseDivision(1, times)));
                    chance -= maxChance;
                    newChance -= maxChance;
                } while (gTLCore$passesChance(chance, maxChance));
            }

            gTLCore$updateCachedChance(entry.content, cache, newChance / 2 + cached);
        }

        return out.isEmpty() ? null : out;
    }
}
