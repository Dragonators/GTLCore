package org.gtlcore.gtlcore.mixin.gtm.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import javax.annotation.Nullable;

@Mixin(RecipeModifierList.class)
public abstract class RecipeModifierListMixin {

    @Shadow(remap = false)
    @Final
    private RecipeModifier[] modifiers;

    /**
     * @author Dragons
     * @reason 阻止强行使用subTick并行
     */
    @Overwrite(remap = false)
    @Nullable
    public GTRecipe apply(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams params, @NotNull OCResult result) {
        GTRecipe modifiedRecipe = recipe;

        for (RecipeModifier modifier : this.modifiers) {
            if (modifiedRecipe != null) {
                modifiedRecipe = modifier.apply(machine, modifiedRecipe, params, result);
            }
        }

        if (modifiedRecipe != null && result.getDuration() != 0) {
            modifiedRecipe.duration = result.getDuration();
            if (result.getEut() > 0L) {
                modifiedRecipe.tickInputs.put(EURecipeCapability.CAP, List.of(new Content(result.getEut(), ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0, null, null)));
            } else if (result.getEut() < 0L) {
                modifiedRecipe.tickOutputs.put(EURecipeCapability.CAP, List.of(new Content(-result.getEut(), ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0, null, null)));
            }

            if (result.getParallel() > 1) {
                modifiedRecipe = ParallelLogic.applyParallel(machine, modifiedRecipe, result.getParallel(), false).getFirst();
            }

            modifiedRecipe.ocTier = result.getOcLevel();
        }

        result.reset();
        return modifiedRecipe;
    }
}
