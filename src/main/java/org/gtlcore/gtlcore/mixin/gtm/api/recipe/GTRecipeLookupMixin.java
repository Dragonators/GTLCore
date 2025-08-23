package org.gtlcore.gtlcore.mixin.gtm.api.recipe;

import org.gtlcore.gtlcore.api.machine.multiblock.CoilWorkableElectricMultipleRecipesMachine;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.machine.trait.MERecipeHandlePart;
import org.gtlcore.gtlcore.api.machine.trait.RecipeHandlePart;
import org.gtlcore.gtlcore.api.recipe.IAdditionalRecipeIterator;
import org.gtlcore.gtlcore.api.recipe.IRecipeIterator;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.WorkableElectricMultipleRecipesMachine;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.steam.SteamWorkableMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.*;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.primitive.PrimitiveWorkableMachine;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;

@Mixin(GTRecipeLookup.class)
public abstract class GTRecipeLookupMixin {

    @Shadow(remap = false)
    @Final
    private GTRecipeType recipeType;

    @Unique
    private IRecipeCapabilityHolder gtlcore$machine;

    @Shadow(remap = false)
    private static final WeakHashMap<AbstractMapIngredient, WeakReference<AbstractMapIngredient>> ingredientRoot = new WeakHashMap();

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    protected @Nullable List<List<AbstractMapIngredient>> prepareRecipeFind(@NotNull IRecipeCapabilityHolder holder) {
        this.gtlcore$machine = holder;
        if (holder instanceof PrimitiveWorkableMachine || holder instanceof SteamWorkableMachine ||
                holder instanceof WorkableTieredMachine || holder instanceof ResearchStationMachine) {
            int totalSize = 0;
            for (var entries : holder.getCapabilitiesProxy().row(IO.IN).entrySet()) {
                int size = 0;
                if ((entries.getKey()).isRecipeSearchFilter()) {
                    for (var entry : entries.getValue())
                        if (entry.getSize() != -1) size += entry.getSize();
                    if (size == Integer.MAX_VALUE) return null;
                    totalSize += size;
                }
            }
            if (totalSize == 0) return null;
            List<List<AbstractMapIngredient>> list = new ObjectArrayList<>(totalSize);
            list.addAll(fromHolder(holder));
            if (list.isEmpty()) {
                RecipeResult.of((IRecipeLogicMachine) holder, RecipeResult.FAIL_NO_INPUT);
                return null;
            }
            return list;
        } else if (holder instanceof IRecipeCapabilityMachine machine) {
            if (machine.getRecipeHandleParts().isEmpty() && machine.getMERecipeHandleParts().isEmpty()) return null;
            List<List<AbstractMapIngredient>> list = new ObjectArrayList<>(machine.getRecipeHandleParts().size() + machine.getMERecipeHandleParts().size());
            list.addAll(this.gtlcore$fromHolder(machine));
            if (list.isEmpty()) {
                return null;
            }
            return list;
        }
        return List.of();
    }

    @Unique
    protected @NotNull List<List<AbstractMapIngredient>> gtlcore$fromHolder(@NotNull IRecipeCapabilityMachine r) {
        List<RecipeHandlePart> recipeHandleParts = r.getCapabilities().getOrDefault(IO.IN, new ObjectArrayList<>());
        List<MERecipeHandlePart> meRecipeHandleParts = r.getMERecipeHandleParts();
        if (recipeHandleParts.isEmpty() && meRecipeHandleParts.isEmpty()) return Collections.emptyList();
        List<List<AbstractMapIngredient>> list = new ObjectArrayList<>(recipeHandleParts.size() + meRecipeHandleParts.size());

        // region ME Pattern, search for uncached slots
        if (!meRecipeHandleParts.isEmpty()) {
            for (var part : meRecipeHandleParts) {
                Int2ObjectArrayMap<Reference2ObjectArrayMap<RecipeCapability<?>, List<Object>>> finalMap = new Int2ObjectArrayMap<>();

                // slot -> (RecipeCapability -> Contents)
                part.getMeHandlerMap().reference2ObjectEntrySet().fastForEach(entry -> {
                    var cap = entry.getKey();
                    var meHandler = entry.getValue();

                    Int2ObjectMaps.fastForEach(meHandler.getActiveAndUnCachedSlotsLimitContentsMap(), e -> {
                        int key = e.getIntKey();
                        var values = e.getValue();

                        var capToList = finalMap.computeIfAbsent(key, k -> new Reference2ObjectArrayMap<>(2));

                        var existing = capToList.get(cap);
                        if (existing == null) {
                            capToList.put(cap, new ArrayList<>(values));
                        } else {
                            existing.addAll(values);
                        }
                    });
                });

                finalMap.int2ObjectEntrySet().fastForEach(e -> {
                    List<AbstractMapIngredient> ingredient = new ObjectArrayList<>();
                    var capToContent = e.getValue();

                    capToContent.reference2ObjectEntrySet().fastForEach(n -> {
                        var cap = n.getKey();
                        var capContent = n.getValue();
                        for (var content : cap.compressIngredients(capContent)) {
                            ingredient.addAll(cap.convertToMapIngredient(content));
                        }
                    });

                    if (!ingredient.isEmpty()) {
                        ingredient.sort(Comparator.comparing(i -> !i.isSpecialIngredient()));
                        list.add(ingredient);
                    }
                });
            }
        }
        // endregion

        if (r.isDistinct()) {
            for (var part : recipeHandleParts) {
                List<AbstractMapIngredient> ingredients = new ObjectArrayList<>();
                for (var it = part.getHandlerMap().reference2ObjectEntrySet().fastIterator(); it.hasNext();) {
                    var next = it.next();
                    var cap = next.getKey();
                    for (var handler : next.getValue()) {
                        for (var content : cap.compressIngredients(handler.getContents())) {
                            ingredients.addAll(cap.convertToMapIngredient(content).stream()
                                    .sorted(Comparator.comparing(i -> !i.isSpecialIngredient())).toList());
                        }
                    }
                }
                list.add(ingredients);
            }
        } else {
            var fluidHandlers = r.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP);
            List<AbstractMapIngredient> fluidIngredients = new ObjectArrayList<>();
            for (var fluidPart : fluidHandlers) {
                for (var content : FluidRecipeCapability.CAP.compressIngredients(fluidPart.getContents())) {
                    fluidIngredients.addAll(FluidRecipeCapability.CAP.convertToMapIngredient(content));
                }
            }
            for (var itemPart : recipeHandleParts) {
                List<AbstractMapIngredient> itemIngredients = new ObjectArrayList<>();
                for (var itemHandler : itemPart.getCapability(ItemRecipeCapability.CAP)) {
                    for (var content : ItemRecipeCapability.CAP.compressIngredients(itemHandler.getContents())) {
                        itemIngredients.addAll(ItemRecipeCapability.CAP.convertToMapIngredient(content));
                    }
                }
                itemIngredients.addAll(fluidIngredients);
                list.add(itemIngredients);
            }
        }
        return list;
    }

    /**
     * @author Adonis
     * @reason .
     */
    @Overwrite(remap = false)
    public @Nullable GTRecipe recurseIngredientTreeFindRecipe(@NotNull List<List<AbstractMapIngredient>> ingredients,
                                                              @NotNull Branch branchMap, @NotNull Predicate<GTRecipe> canHandle, int index, int count, long skip) {
        if (count == ingredients.size()) {
            return null;
        } else if (this.gtlcore$machine instanceof PrimitiveWorkableMachine || this.gtlcore$machine instanceof SteamWorkableMachine ||
                this.gtlcore$machine instanceof WorkableTieredMachine || this.gtlcore$machine instanceof ResearchStationMachine) {
                    for (AbstractMapIngredient obj : ingredients.get(index)) {
                        var targetMap = determineRootNodes(obj, branchMap);
                        var result = targetMap.get(obj);
                        if (result != null) {
                            GTRecipe r = result.map(potentialRecipe -> canHandle.test(potentialRecipe) ? potentialRecipe : null,
                                    potentialBranch -> diveIngredientTreeFindRecipe(ingredients, potentialBranch, canHandle, index, count, skip));
                            if (r != null) {
                                return r;
                            }
                        }
                    }
                } else
            if (this.gtlcore$machine instanceof IRecipeCapabilityMachine) {
                var ingredient = new ObjectArrayList<>(ingredients.get(index));
                return IRecipeIterator.diveIngredientTreeFindRecipe(ingredient, branchMap, canHandle);
            }
        return null;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    private @Nullable GTRecipe diveIngredientTreeFindRecipe(@NotNull List<List<AbstractMapIngredient>> ingredients, @NotNull Branch map, @NotNull Predicate<GTRecipe> canHandle, int currentIndex, int count, long skip) {
        int i = (currentIndex + 1) % ingredients.size();
        while (i != currentIndex) {
            if (((skip & (1L << i)) == 0)) {
                GTRecipe found = recurseIngredientTreeFindRecipe(ingredients, map, canHandle, i, count + 1, skip | (1L << i));
                if (found != null) {
                    return found;
                }
            }
            i = (i + 1) % ingredients.size();
        }
        return null;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    protected @NotNull List<List<AbstractMapIngredient>> fromRecipe(@NotNull GTRecipe r) {
        List<List<AbstractMapIngredient>> list = new ObjectArrayList<>(r.inputs.size());
        r.inputs.forEach((cap, contents) -> {
            if (cap.isRecipeSearchFilter() && !contents.isEmpty()) {
                var ingredients = new ArrayList<>();
                for (var content : contents) ingredients.add(content.getContent());
                for (var ingredient : cap.compressIngredients(ingredients))
                    retrieveCachedIngredient(list, cap.convertToMapIngredient(ingredient), ingredientRoot);
            }
        });
        return list;
    }

    @Shadow(remap = false)
    protected abstract @NotNull List<List<AbstractMapIngredient>> fromHolder(@NotNull IRecipeCapabilityHolder r);

    @Shadow(remap = false)
    protected static void retrieveCachedIngredient(@NotNull List<List<AbstractMapIngredient>> list, @NotNull List<AbstractMapIngredient> ingredients, @NotNull WeakHashMap<AbstractMapIngredient, WeakReference<AbstractMapIngredient>> cache) {}

    @Shadow(remap = false)
    protected static @NotNull Map<AbstractMapIngredient, Either<GTRecipe, Branch>> determineRootNodes(@NotNull AbstractMapIngredient ingredient, @NotNull Branch branchMap) {
        throw new RuntimeException();
    }

    @Shadow(remap = false)
    private GTRecipe recurseIngredientTreeFindRecipe(@NotNull List<List<AbstractMapIngredient>> ingredients,
                                                     @NotNull Branch branchRoot,
                                                     @NotNull Predicate<GTRecipe> canHandle) {
        throw new RuntimeException();
    }

    @Shadow(remap = false)
    @Final
    private Branch lookup;

    /**
     * @author Dragons
     * @reason 跨配方并行机器的lock功能修复
     */
    @Overwrite(remap = false)
    @Nullable
    public GTRecipe find(@NotNull IRecipeCapabilityHolder holder, @NotNull Predicate<GTRecipe> canHandle) {
        // find Cached Recipe in MEHandlers First
        if (holder instanceof IRecipeCapabilityMachine rlm) {
            var parts = rlm.getMERecipeHandleParts();
            for (var part : parts) {
                var cachedMERecipes = part.getMachine().getCachedGTRecipe();
                if (!cachedMERecipes.isEmpty()) {
                    return cachedMERecipes.get(0);
                }
            }
        }
        List<List<AbstractMapIngredient>> list = prepareRecipeFind(holder);
        // couldn't build any inputs to use for search, so no recipe could be found
        if (list == null) return null;
        return recurseIngredientTreeFindRecipe(list, this.lookup, canHandle);
    }

    /**
     * @author Dragons
     * @reason 为RecipeIterator添加ME配方缓存支持
     */
    @Overwrite(remap = false)
    public @NotNull RecipeIterator getRecipeIterator(@NotNull IRecipeCapabilityHolder holder, @NotNull Predicate<GTRecipe> canHandle) {
        List<List<AbstractMapIngredient>> list = this.prepareRecipeFind(holder);
        RecipeIterator iterator = RecipeIteratorAccessor.newRecipeIterator(this.recipeType, list, canHandle);

        // 检查是否有ME配方缓存需要合并
        if (holder instanceof IRecipeCapabilityMachine rlm) {
            var parts = rlm.getMERecipeHandleParts();
            if (!parts.isEmpty()) {
                List<GTRecipe> meRecipes = new ObjectArrayList<>();
                for (var part : parts) {
                    meRecipes.addAll(part.getMachine().getCachedGTRecipe());
                }
                meRecipes = meRecipes.stream().filter(r -> r.recipeType == recipeType).toList();
                if (!meRecipes.isEmpty()) {
                    ((IAdditionalRecipeIterator) iterator).setAdditionalRecipes(meRecipes);
                }
            }

            if (holder instanceof WorkableElectricMultipleRecipesMachine || holder instanceof CoilWorkableElectricMultipleRecipesMachine) {
                ((IAdditionalRecipeIterator) iterator).setUseDiveIngredientTreeFind(true);
            }
            if (holder instanceof IRecipeLogicMachine irl) {
                if (!iterator.hasNext()) RecipeResult.of(irl, RecipeResult.FAIL_NO_INPUT);
            }
        }

        return iterator;
    }
}
