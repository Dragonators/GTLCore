package org.gtlcore.gtlcore.mixin.gtm.api.capability;

import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.*;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.valueprovider.*;
import com.gregtechceu.gtceu.integration.GTRecipeWidget;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.item.crafting.Ingredient;

import com.google.common.primitives.Ints;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;
import java.util.function.BiConsumer;

import static com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability.CAP;

@Mixin(ItemRecipeCapability.class)
public class ItemRecipeCapabilityMixin extends RecipeCapability<Ingredient> {

    protected ItemRecipeCapabilityMixin(String name, int color, boolean doRenderSlot, int sortIndex, IContentSerializer<Ingredient> serializer) {
        super(name, color, doRenderSlot, sortIndex, serializer);
    }

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
            return LongIngredient.create(sizedIngredient.getInner(), modifier.apply((long) sizedIngredient.getAmount()).longValue());
        } else if (content instanceof IntProviderIngredient intProviderIngredient) {
            return new IntProviderIngredient(intProviderIngredient.getInner(), new FlooredInt(new AddedFloat(new MultipliedFloat(new CastedFloat(intProviderIngredient.getCountProvider()), ConstantFloat.of((float) modifier.getMultiplier())), ConstantFloat.of((float) modifier.getAddition()))));
        } else {
            return LongIngredient.create(content, modifier.apply(1).longValue());
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

    @ModifyArg(method = "applyWidgetInfo",
               at = @At(value = "INVOKE",
                        target = "Lcom/lowdragmc/lowdraglib/gui/widget/SlotWidget;setOnAddedTooltips(Ljava/util/function/BiConsumer;)Lcom/lowdragmc/lowdraglib/gui/widget/SlotWidget;"),
               remap = false)
    public BiConsumer<SlotWidget, List<Component>> applyWidgetInfo(BiConsumer onAddedTooltips,
                                                                   @Local(name = "content") Content content,
                                                                   @Local(name = "recipe") GTRecipe recipe,
                                                                   @Local(name = "index") int index,
                                                                   @Local(name = "io") IO io) {
        return (w, tooltips) -> {
            var ingredient = CAP.of(content.content);
            long amount = 1;
            if (ingredient instanceof SizedIngredient si) amount = si.getAmount();
            else if (ingredient instanceof LongIngredient li) amount = li.getActualAmount();
            tooltips.add(Component.translatable("gtceu.machine.quantum_chest.items_stored")
                    .withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal(String.valueOf(amount))));
            GTRecipeWidget.setConsumedChance(content, ChanceLogic.OR, tooltips);
            if (this.isTickSlot(index, io, recipe)) {
                tooltips.add(Component.translatable("gtceu.gui.content.per_tick"));
            }
        };
    }
}
