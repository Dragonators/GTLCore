package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.capability.IMERecipeHandler;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MERecipeHandlePart implements IRecipeHandlePart {

    @Getter
    private final BiMap<GTRecipe, Integer> slotMap = HashBiMap.create();
    @Getter
    private final Reference2ObjectOpenHashMap<RecipeCapability<?>, IMERecipeHandler<?>> meHandlerMap = new Reference2ObjectOpenHashMap<>();
    @Getter
    private final IMEPatternPartMachine machine;

    public MERecipeHandlePart(IMEPatternPartMachine machine) {
        this.machine = machine;
    }

    public static MERecipeHandlePart of(IMEPatternPartMachine machine) {
        MERecipeHandlePart rhl = new MERecipeHandlePart(machine);
        rhl.addMEHandlers(machine.getMERecipeHandlerTraits());
        return rhl;
    }

    public void addMEHandlers(Iterable<IMERecipeHandlerTrait<?>> handlers) {
        for (var handler : handlers) {
            getMeHandlerMap().putIfAbsent(handler.getCapability(), handler);
        }
    }

    @NotNull
    public <T> Object2LongMap<T> getMEContent(RecipeCapability<?> cap) {
        return getMEContent(cap, this.getMECapability(cap).getActiveSlots(cap));
    }

    @NotNull
    public <T> Object2LongMap<T> getMEContent(RecipeCapability<?> cap, List<Integer> slots) {
        return (Object2LongMap<T>) this.getMECapability(cap).getCustomSlotsStackMap(slots);
    }

    public <T> Object2LongMap<T> getMEContentSafe(RecipeCapability<?> cap, Class<T> expectedType) {
        return getMEContentSafe(cap, this.getMECapability(cap).getActiveSlots(cap), expectedType);
    }

    public <T> Object2LongMap<T> getMEContentSafe(RecipeCapability<?> cap, List<Integer> slots, Class<T> expectedType) {
        @SuppressWarnings("unchecked")
        var map = (Object2LongMap<T>) this.getMECapability(cap).getCustomSlotsStackMap(slots);
        for (var it = Object2LongMaps.fastIterator(map); it.hasNext();) {
            if (!expectedType.isInstance(it.next().getKey())) {
                it.remove();
            }
        }
        return map;
    }

    public @NotNull IMERecipeHandler<?> getMECapability(RecipeCapability<?> cap) {
        return getMeHandlerMap().getOrDefault(cap, null);
    }

    public void setMachineCache(Map<GTRecipe, IRecipeHandlePart> map) {
        this.machine.setCache(map, this);
    }

    public int meHandleRecipe(GTRecipe recipe,
                              Reference2ObjectMap<RecipeCapability<?>, List<Object>> contents,
                              boolean simulate) {
        if (meHandlerMap.isEmpty() || contents.isEmpty()) {
            return -1;
        }

        // Array for 1-2 handler
        IMERecipeHandler<?>[] handlers = new IMERecipeHandler<?>[contents.size()];
        List<Object>[] contentArrays = new List[contents.size()];
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
            var activeSlots = handlers[i].getActiveSlots(
                    Reference2ObjectMaps.fastIterator(contents).next().getKey());

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
                if (!this.machine.hasCacheInSlot(slot)) this.machine.setRecipe(slot, recipe);
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
            // 当取出再原地放回样板时，RecipeHandle侧的slotMap会先从该方法尝试handle原slot
            // 此时会handle成功，导致对应slot的cache永远无法更新(只在meHandleRecipe这一方法中更新)
            // 因此在此处特别添加判断，防止此类情形
            if (!machine.hasCacheInSlot(trySlot)) {
                machine.setRecipe(trySlot, recipe);
            }
            return true;
        }
        return false;
    }

    @Override
    public IO getHandlerIO() {
        return IO.IN;
    }

    @Override
    public Object2LongMap<?> getContent(RecipeCapability<?> cap) {
        return getMEContent(cap);
    }
}
