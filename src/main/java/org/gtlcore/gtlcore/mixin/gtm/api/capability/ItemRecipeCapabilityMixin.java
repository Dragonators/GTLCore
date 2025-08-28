package org.gtlcore.gtlcore.mixin.gtm.api.capability;

import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.valueprovider.AddedFloat;
import com.gregtechceu.gtceu.common.valueprovider.CastedFloat;
import com.gregtechceu.gtceu.common.valueprovider.FlooredInt;
import com.gregtechceu.gtceu.common.valueprovider.MultipliedFloat;

import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.item.crafting.Ingredient;

import com.google.common.primitives.Ints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability.CAP;

@Mixin(ItemRecipeCapability.class)
public class ItemRecipeCapabilityMixin {

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public int limitParallel(GTRecipe recipe, IRecipeCapabilityHolder holder, int multiplier) {
        return Ints.saturatedCast(IParallelLogic.getOutputItemParallel(holder, recipe, recipe.getOutputContents(CAP), multiplier));
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public int getMaxParallelRatio(IRecipeCapabilityHolder holder, GTRecipe recipe, int parallelAmount) {
        return Ints.saturatedCast(IParallelLogic.getInputItemParallel(holder, recipe, parallelAmount));
    }

    /**
     * @author Dragons
     * @reason 提供Long级别物品处理
     */
    @Overwrite(remap = false)
    public Ingredient copyWithModifier(Ingredient content, ContentModifier modifier) {
        if (content instanceof LongIngredient longIngredient) {
            return LongIngredient.create(longIngredient.getInner(), modifier.apply(longIngredient.getActualAmount()).longValue());
        } else if (content instanceof SizedIngredient sizedIngredient) {
            return LongIngredient.create(sizedIngredient.getInner(), modifier.apply(sizedIngredient.getAmount()).longValue());
        } else if (content instanceof IntProviderIngredient intProviderIngredient) {
            return new IntProviderIngredient(intProviderIngredient.getInner(), new FlooredInt(new AddedFloat(new MultipliedFloat(new CastedFloat(intProviderIngredient.getCountProvider()), ConstantFloat.of((float) modifier.getMultiplier())), ConstantFloat.of((float) modifier.getAddition()))));
        } else {
            return SizedIngredient.create(content, modifier.apply(1).intValue());
        }
    }

    /**
     * @author Dragons
     * @reason 提供Long级别物品处理
     */
    @Overwrite(remap = false)
    public Ingredient copyInner(Ingredient content) {
        return LongIngredient.copy(content);
    }
}
