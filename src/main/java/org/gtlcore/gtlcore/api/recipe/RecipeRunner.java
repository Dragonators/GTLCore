package org.gtlcore.gtlcore.api.recipe;

import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeHandlePart;
import org.gtlcore.gtlcore.api.machine.trait.MERecipeHandlePart;
import org.gtlcore.gtlcore.api.machine.trait.RecipeHandlePart;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;

import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

/**
 * 部分代码参考自gto
 * &#064;line <a href="https://github.com/GregTech-Odyssey/GTOCore">...</a>
 */

public class RecipeRunner {

    private final GTRecipe recipe;
    private final IRecipeHandlePart recipeHandlePart;
    private final IO io;
    private final boolean isTick;
    private final IRecipeCapabilityHolder holder;
    private final Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches;
    private final boolean simulated;
    private Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> recipeContent;

    public RecipeRunner(GTRecipe recipe, IO io, boolean isTick, IRecipeCapabilityHolder holder,
                        Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches, boolean simulated) {
        IRecipeHandlePart recipeHandlePart = null;
        if (io == IO.IN && holder instanceof IRecipeCapabilityMachine machine) {
            recipeHandlePart = machine.getRecipeHandleMap().get(recipe);
        }
        this.recipeHandlePart = recipeHandlePart;
        this.recipe = recipe;
        this.io = io;
        this.isTick = isTick;
        this.holder = holder;
        this.chanceCaches = chanceCaches;
        this.recipeContent = new Reference2ObjectOpenHashMap<>();
        this.simulated = simulated;
    }

    public RecipeResult handle(Map<RecipeCapability<?>, List<Content>> entry) {
        this.fillContent(entry);
        return this.recipeContent.isEmpty() ? RecipeResult.SUCCESS :
                (handleContentsInternal(io) ? RecipeResult.SUCCESS : RecipeResult.fail(null));
    }

    private void fillContent(Map<RecipeCapability<?>, List<Content>> entries) {
        for (var entry : entries.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            if (!cap.doMatchInRecipe()) continue;
            if (entry.getValue().isEmpty()) continue;
            List<Content> chancedContents = new ArrayList<>();
            var contentList = this.recipeContent.computeIfAbsent(cap, c -> new ObjectArrayList<>());
            for (Content cont : entry.getValue()) {
                if (simulated) {
                    contentList.add(cont.content);
                } else {
                    if (cont.chance >= cont.maxChance) {
                        contentList.add(cont.content);
                    } else {
                        chancedContents.add(cont.copy(cap, ContentModifier.multiplier(1.0 / recipe.parallels)));
                    }
                }
            }
            if (!chancedContents.isEmpty()) {
                ChanceBoostFunction function = recipe.getType().getChanceFunction();
                ChanceLogic logic = recipe.getChanceLogicForCapability(cap, this.io, this.isTick);
                int recipeTier = RecipeHelper.getPreOCRecipeEuTier(recipe);
                int holderTier = holder.getChanceTier();
                var cache = this.chanceCaches.get(cap);
                chancedContents = logic.roll(chancedContents, function, recipeTier, holderTier, cache, recipe.parallels, cap);
                if (chancedContents != null) {
                    for (Content cont : chancedContents) {
                        contentList.add(cont.content);
                    }
                }
            }
            if (contentList.isEmpty()) recipeContent.remove(cap);
        }
    }

    private boolean handleContentsInternal(IO capIO) {
        if (!(holder instanceof IRecipeCapabilityMachine machine)) {
            return false;
        }
        if (machine.getRecipeHandleParts().isEmpty() && machine.getMERecipeHandleParts().isEmpty()) {
            return false;
        }

        if (capIO == IO.IN) {
            if (recipeHandlePart instanceof MERecipeHandlePart meRecipeHandlePart)
                if (meRecipeHandlePart.meHandleCacheRecipe(recipe, recipeContent, simulated))
                    return true;

            if (!machine.getMERecipeHandleParts().isEmpty()) {
                var parts = machine.getMERecipeHandleParts();
                for (var p : parts) {
                    var slot = p.meHandleRecipe(recipe, recipeContent, simulated);
                    if (slot >= 0) {
                        if (simulated) machine.setMERecipeHandleMap(p, recipe, slot);
                        return true;
                    }
                }
            }

            if (machine.isDistinct()) {
                if (recipeHandlePart instanceof RecipeHandlePart rht) {
                    var result = rht.handleRecipe(IO.IN, recipe, recipeContent, simulated);
                    if (result.isEmpty()) {
                        return true;
                    }
                }
                var inHandlers = machine.getCapabilities().getOrDefault(IO.IN, Collections.emptyList());
                if (inHandlers.isEmpty()) return false;
                for (var handler : inHandlers) {
                    var result = handler.handleRecipe(IO.IN, recipe, recipeContent, simulated);
                    if (result.isEmpty()) {
                        if (simulated) machine.setRecipeHandleMap(handler, recipe);
                        return true;
                    }
                }
            } else {
                List<Object> itemContent = recipeContent.getOrDefault(ItemRecipeCapability.CAP, Collections.emptyList());
                if (!itemContent.isEmpty()) {
                    Map<RecipeCapability<?>, List<Object>> itemsOnly = Collections.singletonMap(ItemRecipeCapability.CAP, itemContent);
                    if (this.recipeHandlePart instanceof RecipeHandlePart rht) {
                        var result = rht.handleRecipe(IO.IN, recipe, itemsOnly, simulated);
                        if (result.isEmpty()) {
                            itemContent.clear();
                        }
                    }
                    if (!itemContent.isEmpty()) {
                        for (var part : machine.getCapabilities().getOrDefault(IO.IN, Collections.emptyList())) {
                            var result = part.handleRecipe(IO.IN, recipe, itemsOnly, simulated);
                            if (result.isEmpty()) {
                                if (simulated) machine.setRecipeHandleMap(part, recipe);
                                itemContent.clear();
                                break;
                            }
                        }
                    }
                }

                List<?> fluidContent = recipeContent.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList());
                if (!fluidContent.isEmpty()) {
                    for (var fluid : machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
                        var result = fluid.handleRecipe(IO.IN, recipe, fluidContent, null, simulated);
                        if (result == null) {
                            fluidContent = Collections.emptyList();
                            break;
                        } else {
                            fluidContent = result;
                        }
                    }
                }

                return itemContent.isEmpty() && fluidContent.isEmpty();
            }
        } else {
            var handlers = machine.getCapabilities().getOrDefault(IO.OUT, Collections.emptyList());
            if (handlers.isEmpty()) return false;
            handlers.sort(RecipeHandlePart.COMPARATOR.reversed());
            for (var handler : handlers) {
                recipeContent = handler.handleRecipe(capIO, recipe, recipeContent, simulated);
                if (recipeContent.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryHandleWithPart(IRecipeHandlePart part,
                                      IRecipeCapabilityMachine machine,
                                      Map<RecipeCapability<?>, List<Object>> contents) {
        if (part == null) return false;
        if (part instanceof MERecipeHandlePart meRecipeHandlePart) {
            return meRecipeHandlePart.meHandleCacheRecipe(recipe, recipeContent, simulated);
        } else if (part instanceof RecipeHandlePart handlePart) {
            var result = handlePart.handleRecipe(IO.IN, recipe, contents, simulated);
            if (result.isEmpty()) {
                if (simulated) machine.setRecipeHandleMap(handlePart, recipe);
                return true;
            }
        }
        return false;
    }
}
