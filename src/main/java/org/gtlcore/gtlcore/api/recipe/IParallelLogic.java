package org.gtlcore.gtlcore.api.recipe;

import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.machine.trait.MEPatternRecipeHandlePart;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.utils.IngredientEquality;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;
import java.util.function.Predicate;

public interface IParallelLogic {

    static GTRecipe getRecipeOutputChance(IRecipeCapabilityHolder holder, GTRecipe recipe) {
        Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Content>> recipeContents = new Reference2ObjectOpenHashMap<>();
        for (var entry : recipe.outputs.entrySet()) {
            var cap = entry.getKey();
            List<Content> chancedContents = new ObjectArrayList<>();
            var contentList = recipeContents.computeIfAbsent(cap, c -> new ObjectArrayList<>());
            for (var cont : entry.getValue()) {
                if (cont.chance >= cont.maxChance) contentList.add(cont);
                else chancedContents.add(cont);
            }
            if (!chancedContents.isEmpty()) {
                var function = recipe.getType().getChanceFunction();
                var logic = recipe.getChanceLogicForCapability(cap, IO.OUT, true);
                int recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
                int holderTier = holder.getChanceTier();
                var cache = ((IRecipeLogicMachine) holder).getRecipeLogic().getChanceCaches().get(cap);
                chancedContents = logic.roll(chancedContents, function, recipeTier, holderTier, cache, recipe.parallels, cap);
                if (chancedContents != null) {
                    for (var cont : chancedContents) {
                        contentList.add(new Content(cont.content, 10000, 10000, 0, null, null));
                    }
                }
            }
            if (contentList.isEmpty()) recipeContents.remove(cap);
        }
        var copy = new GTRecipe(recipe.recipeType, recipe.id, recipe.inputs, recipeContents, recipe.tickInputs, recipe.tickOutputs,
                recipe.inputChanceLogics, recipe.outputChanceLogics, recipe.tickInputChanceLogics, recipe.tickOutputChanceLogics,
                recipe.conditions, recipe.ingredientActions, recipe.data, recipe.duration, recipe.isFuel);
        copy.parallels = recipe.parallels;
        copy.ocTier = recipe.ocTier;
        return copy;
    }

    static long getParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
        if (parallelAmount <= 1) return parallelAmount;
        long maxParallel = getMaxParallel(holder, recipe, parallelAmount);
        if (maxParallel == 0L) return 0L;
        return getMinParallel(holder, recipe, maxParallel);
    }

    static long getMaxParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
        for (var cap : recipe.inputs.keySet()) {
            if (cap == ItemRecipeCapability.CAP) {
                parallelAmount = Math.min(parallelAmount, getInputItemParallel(holder, recipe, parallelAmount));
                if (parallelAmount == 0L) break;
            } else if (cap == FluidRecipeCapability.CAP) {
                parallelAmount = Math.min(parallelAmount, getInputFluidParallel(holder, recipe, parallelAmount));
                if (parallelAmount == 0L) break;
            }
        }
        return parallelAmount;
    }

    static long getMinParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
        for (var entry : recipe.outputs.entrySet()) {
            if (entry.getKey() == ItemRecipeCapability.CAP && holder instanceof IRecipeLogicMachine machine && !machine.canVoidRecipeOutputs(ItemRecipeCapability.CAP)) {
                parallelAmount = Math.min(parallelAmount, getOutputItemParallel(holder, recipe, entry.getValue(), parallelAmount));
                if (parallelAmount == 0L) break;
            } else if (entry.getKey() == FluidRecipeCapability.CAP && holder instanceof IRecipeLogicMachine machine && !machine.canVoidRecipeOutputs(FluidRecipeCapability.CAP)) {
                parallelAmount = Math.min(parallelAmount, getOutputFluidParallel(holder, recipe, entry.getValue(), parallelAmount));
                if (parallelAmount == 0L) break;
            }
        }
        return parallelAmount;
    }

    static long getInputItemParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
        if (parallelAmount <= 1) return parallelAmount;
        if (!(holder instanceof IRecipeCapabilityMachine machine)) return 1;
        if (machine.getRecipeHandleParts().isEmpty() && machine.getMEPatternRecipeHandleParts().isEmpty()) return 0;

        Object2LongOpenCustomHashMap<Ingredient> countableMap = new Object2LongOpenCustomHashMap<>(IngredientEquality.IngredientHashStrategy.INSTANCE);

        for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            if (content.chance <= 0) continue;
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            long ingredientCount;
            if (ingredient instanceof LongIngredient longIngredient) {
                ingredientCount = longIngredient.getAmount();
            } else if (ingredient instanceof SizedIngredient sizedIngredient) {
                ingredientCount = sizedIngredient.getAmount();
            } else ingredientCount = 1;
            countableMap.addTo(ingredient, ingredientCount);
        }

        if (countableMap.isEmpty()) return parallelAmount;

        Object2LongOpenCustomHashMap<ItemStack> ingredientStacks = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());

        var handle = machine.getRecipeHandleMap().get(recipe);
        if (handle instanceof MEPatternRecipeHandlePart mePatternRecipeHandlePart) {
            // ME handler
            int slot = mePatternRecipeHandlePart.getSlotMap().getOrDefault(recipe, -1);
            if (slot != -1) {
                for (var entry : Object2LongMaps.fastIterable(mePatternRecipeHandlePart.getMEContent(ItemRecipeCapability.CAP, List.of(slot)))) {
                    ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
                }
            }
        } else if (handle != null) {
            for (var entry : Object2LongMaps.fastIterable(handle.getContent(ItemRecipeCapability.CAP))) {
                ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
            }
        } else {
            // ME handlers, All Active Slots
            for (MEPatternRecipeHandlePart part : machine.getMEPatternRecipeHandleParts()) {
                for (var entry : Object2LongMaps.fastIterable(part.getMEContent(ItemRecipeCapability.CAP))) {
                    ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
                }
            }
            // other handlers
            for (var it : machine.getCapabilities().getOrDefault(IO.IN, Collections.emptyList())) {
                for (var entry : Object2LongMaps.fastIterable(it.getContent(ItemRecipeCapability.CAP))) {
                    ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
                }
            }
        }
        return calculate(parallelAmount, countableMap, ingredientStacks);
    }

    static long getInputFluidParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
        if (parallelAmount <= 1) return parallelAmount;
        if (!(holder instanceof IRecipeCapabilityMachine machine)) return 1;
        if (machine.getRecipeHandleParts().isEmpty() && machine.getMEPatternRecipeHandleParts().isEmpty()) return 0;

        Object2LongOpenHashMap<FluidIngredient> fluidCountMap = new Object2LongOpenHashMap<>();

        for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
            FluidIngredient fluidInput = FluidRecipeCapability.CAP.of(content.content);
            if (content.chance > 0) {
                fluidCountMap.addTo(fluidInput, fluidInput.getAmount());
            }
        }
        if (fluidCountMap.isEmpty()) return parallelAmount;

        Object2LongOpenHashMap<FluidStack> ingredientStacks = new Object2LongOpenHashMap<>();

        var recipeHandle = machine.getRecipeHandleMap().get(recipe);
        if (recipeHandle instanceof MEPatternRecipeHandlePart mePatternRecipeHandlePart) {
            // ME handler
            int slot = mePatternRecipeHandlePart.getSlotMap().getOrDefault(recipe, -1);
            if (slot != -1) {
                for (var entry : Object2LongMaps.fastIterable(mePatternRecipeHandlePart.getMEContent(FluidRecipeCapability.CAP, List.of(slot)))) {
                    ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
                }
            }
        } else if (recipeHandle != null && machine.isDistinct()) {
            for (var entry : Object2LongMaps.fastIterable(recipeHandle.getContent(FluidRecipeCapability.CAP))) {
                ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
            }
        } else {
            for (var container : machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
                if (container instanceof CatalystFluidStackHandler) continue;
                for (var object : container.getContents()) {
                    if (object instanceof FluidStack fs) {
                        ingredientStacks.addTo(fs, fs.getAmount());
                    }
                }
            }
        }
        return calculate(parallelAmount, fluidCountMap, ingredientStacks);
    }

    private static <I extends Predicate<S>, S> long calculate(long parallelLimit,
                                                              Object2LongMap<I> countableMap,
                                                              Object2LongMap<S> ingredientStacks) {
        if (ingredientStacks.isEmpty()) return 0;

        long needed;
        long available;
        for (var it = Object2LongMaps.fastIterator(countableMap); it.hasNext(); parallelLimit = Math.min(parallelLimit, available / needed)) {
            var entry = it.next();
            needed = entry.getLongValue();
            available = 0;
            for (var input : Object2LongMaps.fastIterable(ingredientStacks)) {
                if (entry.getKey().test(input.getKey())) {
                    available = input.getLongValue();
                    break;
                }
            }
            if (available < needed) {
                parallelLimit = 0;
                break;
            }
        }
        return parallelLimit;
    }

    static long getOutputItemParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, List<Content> contents, long multiplier) {
        if (multiplier <= 1L || contents.isEmpty()) return multiplier;
        if (holder instanceof IRecipeCapabilityMachine machine) {
            if (machine.isMEOutPutDual() || machine.isMEOutPutBus()) return multiplier;
            long minMultiplier = 0L;
            long maxMultiplier = multiplier;
            long maxCount = 0L;
            List<Ingredient> ingredients = new ObjectArrayList<>(contents.size());
            for (var content : contents) {
                Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
                long ingredientCount;
                if (ingredient instanceof LongIngredient longIngredient) {
                    ingredientCount = longIngredient.getAmount();
                } else if (ingredient instanceof SizedIngredient sizedIngredient) {
                    ingredientCount = sizedIngredient.getAmount();
                } else ingredientCount = 1;
                maxCount = Math.max(maxCount, ingredientCount);
                ingredients.add(ingredient);
            }
            if (maxCount == 0L) return multiplier;
            var handlers = machine.getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP);
            if (handlers.isEmpty()) return 0L;
            while (minMultiplier != maxMultiplier) {
                List<Ingredient> copied = new ObjectArrayList<>(ingredients.size());
                for (var ing : ingredients) {
                    copied.add(ItemRecipeCapability.CAP.copyWithModifier(ing, ContentModifier.multiplier(multiplier)));
                }
                for (var handler : handlers) {
                    copied = (List<Ingredient>) handler.handleRecipe(IO.OUT, recipe, copied, null, true);
                    if (copied == null || copied.isEmpty()) break;
                    if (multiplier == 1L) return 0L;
                }
                long[] bin = adjustMultiplier(copied == null, minMultiplier, multiplier, maxMultiplier);
                minMultiplier = bin[0];
                multiplier = bin[1];
                maxMultiplier = bin[2];
            }
            return multiplier;
        }
        return 1;
    }

    static long getOutputFluidParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, List<Content> contents, long multiplier) {
        if (multiplier <= 1L || contents.isEmpty()) return multiplier;
        if (holder instanceof IRecipeCapabilityMachine machine) {
            if (machine.isMEOutPutDual() || machine.isMEOutPutHatch()) return multiplier;
            long minMultiplier = 0L;
            long maxMultiplier = multiplier;
            long maxCount = 0L;
            List<FluidIngredient> ingredients = new ObjectArrayList<>(contents.size());
            for (var content : contents) {
                FluidIngredient recipeIngredient = FluidRecipeCapability.CAP.of(content.content);
                maxCount = Math.max(maxCount, recipeIngredient.getAmount());
                ingredients.add(recipeIngredient);
            }
            if (maxCount == 0L) return multiplier;
            List<IRecipeHandler<?>> handlers = machine.getCapabilitiesFlat(IO.OUT, FluidRecipeCapability.CAP);
            if (handlers.isEmpty()) return 0L;
            while (minMultiplier != maxMultiplier) {
                List<FluidIngredient> copied = new ObjectArrayList<>(ingredients.size());
                for (FluidIngredient ing : ingredients) {
                    copied.add(FluidRecipeCapability.CAP.copyWithModifier(ing, ContentModifier.multiplier((double) multiplier)));
                }
                for (IRecipeHandler<?> handler : handlers) {
                    copied = (List<FluidIngredient>) handler.handleRecipe(IO.OUT, recipe, copied, null, true);
                    if (copied == null || copied.isEmpty()) break;
                    if (multiplier == 1L) return 0L;
                }
                long[] bin = adjustMultiplier(copied == null, minMultiplier, multiplier, maxMultiplier);
                minMultiplier = bin[0];
                multiplier = bin[1];
                maxMultiplier = bin[2];
            }
            return multiplier;

        }
        return 1;
    }

    private static long[] adjustMultiplier(boolean mergedAll, long minMultiplier, long multiplier, long maxMultiplier) {
        if (mergedAll) {
            minMultiplier = multiplier;
            long remainder = (maxMultiplier - multiplier) % 2L;
            multiplier = multiplier + remainder + (maxMultiplier - multiplier) / 2L;
        } else {
            maxMultiplier = multiplier;
            multiplier = (multiplier + minMultiplier) / 2L;
        }
        if (maxMultiplier - minMultiplier <= 1L) {
            maxMultiplier = minMultiplier;
            multiplier = minMultiplier;
        }
        return new long[] { minMultiplier, multiplier, maxMultiplier };
    }
}
