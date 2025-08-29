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
            List<Content> chancedContents = new ObjectArrayList<>();
            var contentList = this.recipeContent.computeIfAbsent(cap, c -> new ObjectArrayList<>());
            for (Content cont : entry.getValue()) {
                if (simulated) {
                    contentList.add(cont.content);
                } else {
                    if (cont.chance >= cont.maxChance) {
                        contentList.add(cont.content);
                    } else {
                        chancedContents.add(cont);
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

        // Cache collections to avoid repeated method calls
        var recipeHandleParts = machine.getRecipeHandleParts();
        var meRecipeHandleParts = machine.getMERecipeHandleParts();

        if (recipeHandleParts.isEmpty() && meRecipeHandleParts.isEmpty()) {
            return false;
        }

        if (capIO == IO.IN) {
            // Handle ME cache recipe first (highest priority)
            if (recipeHandlePart instanceof MERecipeHandlePart meRecipeHandlePart) {
                if (meRecipeHandlePart.meHandleCacheRecipe(recipe, recipeContent, simulated)) {
                    return true;
                }
            }

            // Handle ME recipe parts
            if (!meRecipeHandleParts.isEmpty()) {
                for (var part : meRecipeHandleParts) {
                    var slot = part.meHandleRecipe(recipe, recipeContent, simulated);
                    if (slot >= 0) {
                        if (simulated) machine.setMERecipeHandleMap(part, recipe, slot);
                        return true;
                    }
                }
            }

            // Cache input handlers to avoid repeated getCapabilities() calls
            var inHandlers = machine.getCapabilities().getOrDefault(IO.IN, Collections.emptyList());

            if (machine.isDistinct()) {
                // Handle specific recipe handler first
                if (recipeHandlePart instanceof RecipeHandlePart rht) {
                    var result = rht.handleRecipe(IO.IN, recipe, recipeContent, simulated);
                    if (result.isEmpty()) {
                        return true;
                    }
                }

                // Handle remaining input handlers
                if (!inHandlers.isEmpty()) {
                    for (var handler : inHandlers) {
                        var result = handler.handleRecipe(IO.IN, recipe, recipeContent, simulated);
                        if (result.isEmpty()) {
                            if (simulated) machine.setRecipeHandleMap(handler, recipe);
                            return true;
                        }
                    }
                }
            } else {
                return handleNonDistinctInput(machine, inHandlers);
            }
        } else {
            if (handleOutput(machine.getMERecipeOutputHandleParts())) return true;
            return handleOutput(machine);
        }

        return false;
    }

    private boolean handleNonDistinctInput(IRecipeCapabilityMachine machine, List<RecipeHandlePart> inHandlers) {
        // Handle items
        List<Object> itemContent = recipeContent.getOrDefault(ItemRecipeCapability.CAP, Collections.emptyList());
        boolean itemsHandled = itemContent.isEmpty();

        if (!itemsHandled) {
            // Create single-entry map only when needed
            Map<RecipeCapability<?>, List<Object>> itemsOnly = Map.of(ItemRecipeCapability.CAP, itemContent);

            // Try specific recipe handler first
            if (recipeHandlePart instanceof RecipeHandlePart rht) {
                var result = rht.handleRecipe(IO.IN, recipe, itemsOnly, simulated);
                if (result.isEmpty()) {
                    itemContent.clear();
                    itemsHandled = true;
                }
            }

            // Try other input handlers if items still not handled
            if (!itemsHandled && !inHandlers.isEmpty()) {
                for (var part : inHandlers) {
                    var result = part.handleRecipe(IO.IN, recipe, itemsOnly, simulated);
                    if (result.isEmpty()) {
                        if (simulated) machine.setRecipeHandleMap(part, recipe);
                        itemContent.clear();
                        itemsHandled = true;
                        break;
                    }
                }
            }
        }

        // Handle fluids
        List<?> fluidContent = recipeContent.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList());
        boolean fluidsHandled = fluidContent.isEmpty();

        if (!fluidsHandled) {
            var fluidHandlers = machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP);
            for (var fluidHandler : fluidHandlers) {
                var result = fluidHandler.handleRecipe(IO.IN, recipe, fluidContent, null, simulated);
                if (result == null) {
                    fluidsHandled = true;
                    break;
                } else {
                    fluidContent = result;
                }
            }
        }

        return itemsHandled && fluidsHandled;
    }

    private boolean handleOutput(IRecipeCapabilityMachine machine) {
        var handlers = machine.getCapabilities().getOrDefault(IO.OUT, Collections.emptyList());
        if (handlers.isEmpty()) return false;

        // Sort only if not already sorted (assuming handlers list is stable)
        // Consider caching sorted handlers in machine if this becomes a bottleneck
        handlers.sort(RecipeHandlePart.COMPARATOR.reversed());

        for (var handler : handlers) {
            recipeContent = handler.handleRecipe(IO.OUT, recipe, recipeContent, simulated);
            if (recipeContent.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean handleOutput(List<MERecipeHandlePart> meHandlers) {
        if (meHandlers.isEmpty()) return false;
        for (MERecipeHandlePart meHandler : meHandlers) {
            if (meHandler.meHandleOutput(recipeContent, simulated)) return true;
        }
        return false;
    }
}
