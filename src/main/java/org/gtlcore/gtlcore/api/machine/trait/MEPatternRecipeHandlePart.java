package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.capability.IMERecipeHandler;
import org.gtlcore.gtlcore.api.machine.trait.MEPart.IMEPatternPartMachine;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public class MEPatternRecipeHandlePart extends MEIORecipeHandlePart {

    @Getter
    private final BiMap<GTRecipe, Integer> slotMap = HashBiMap.create();

    @Getter
    private final IMEPatternPartMachine patternMachine;

    public MEPatternRecipeHandlePart(IMEPatternPartMachine machine) {
        super(machine);
        this.patternMachine = machine;
    }

    public static MEPatternRecipeHandlePart of(IMEPatternPartMachine machine) {
        MEPatternRecipeHandlePart rhl = new MEPatternRecipeHandlePart(machine);
        rhl.addMEHandlers(machine.getMERecipeHandlerTraits());
        return rhl;
    }

    @NotNull
    public <T extends Predicate<S>, S> Object2LongMap<S> getMEContent(RecipeCapability<T> cap) {
        return getMEContent(cap, this.getMECapability(cap).getActiveSlots());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends Predicate<S>, S> Object2LongMap<S> getMEContent(RecipeCapability<T> cap, List<Integer> slots) {
        return ((IMERecipeHandlerTrait<T, S>) (this.getMECapability(cap))).getCustomSlotsStackMap(slots);
    }

    public void restoreMachineCache(Map<GTRecipe, IRecipeHandlePart> map) {
        if (this.patternMachine != null) {
            this.patternMachine.restoreSlotMap(this.slotMap, slot -> slotMap.inverse().remove(slot));
            for (var key : slotMap.keySet()) {
                map.put(key, this);
            }
        }
    }

    public int meHandleRecipe(GTRecipe recipe,
                              Reference2ObjectMap<RecipeCapability<?>, List<Object>> contents,
                              boolean simulate) {
        if (meHandlerMap.isEmpty() || contents.isEmpty()) {
            return -1;
        }

        // Array for 1-2 handler
        IMERecipeHandler<?, ?>[] handlers = new IMERecipeHandler[contents.size()];
        List[] contentArrays = new List[contents.size()];
        int handlerCount = 0;

        // 单次遍历收集handlers和contents
        for (var it = Reference2ObjectMaps.fastIterator(contents); it.hasNext();) {
            var entry = it.next();
            var handler = getMECapability(entry.getKey());
            handlers[handlerCount] = handler;
            contentArrays[handlerCount] = entry.getValue();
            handlerCount++;
        }

        // 求取所有meHandler的activeSlots交集
        var intersectionSlots = new IntOpenHashSet();
        for (int i = 0; i < handlerCount; i++) {
            var activeSlots = handlers[i].getActiveSlots();

            if (activeSlots.isEmpty()) return -1;

            if (intersectionSlots.isEmpty()) {
                intersectionSlots.addAll(activeSlots);
            } else {
                intersectionSlots.retainAll(activeSlots);
            }
            if (intersectionSlots.isEmpty()) {
                return -1;
            }
        }

        // 初始化所有meHandler的handle内容
        for (int i = 0; i < handlerCount; i++) {
            handlers[i].initMEHandleContents(recipe, contentArrays[i], simulate);
        }

        // 遍历交集中的每个slot
        for (int slot : intersectionSlots) {
            boolean allSuccess = true;

            // 对所有cap对应的meHandler调用meHandleRecipe方法
            for (int i = 0; i < handlerCount; i++) {
                if (!handlers[i].meHandleRecipe(recipe, simulate, slot)) {
                    allSuccess = false;
                    break;
                }
            }

            if (allSuccess) {
                if (!this.patternMachine.hasCacheInSlot(slot)) this.patternMachine.setSlotCacheRecipe(slot, recipe);
                return slot;
            }
        }

        return -1;
    }

    public boolean meHandleCacheRecipe(GTRecipe recipe,
                                       Reference2ObjectMap<RecipeCapability<?>, List<Object>> contents,
                                       boolean simulate) {
        int trySlot = this.slotMap.getOrDefault(recipe, -1);
        if (!getMeHandlerMap().isEmpty() && trySlot >= 0) {
            for (var it = Reference2ObjectMaps.fastIterator(contents); it.hasNext();) {
                var entry = it.next();
                var cap = entry.getKey();
                var content = entry.getValue();
                var meHandler = getMECapability(cap);
                meHandler.initMEHandleContents(recipe, content, simulate);
                if (!meHandler.meHandleRecipe(recipe, simulate, trySlot)) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public <T extends Predicate<S>, S> Object2LongMap<S> getContent(RecipeCapability<T> cap) {
        return getMEContent(cap);
    }
}
