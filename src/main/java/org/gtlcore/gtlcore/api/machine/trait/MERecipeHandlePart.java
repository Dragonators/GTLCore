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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class MERecipeHandlePart implements IRecipeHandlePart {

    public static final Comparator<MERecipeHandlePart> COMPARATOR = Comparator.comparingInt(h -> sumPriority(h.meHandlerMap));

    protected static int sumPriority(Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandler<?, ?>> meHandlerMap) {
        int sum = 0;
        for (var it = Reference2ObjectMaps.fastIterator(meHandlerMap); it.hasNext();) {
            sum += it.next().getValue().getPriority();
        }
        return sum;
    }

    @Getter
    private final BiMap<GTRecipe, Integer> slotMap = HashBiMap.create();
    @Getter
    private final Reference2ObjectOpenHashMap<RecipeCapability<?>, IMERecipeHandler<? extends Predicate<?>, ?>> meHandlerMap = new Reference2ObjectOpenHashMap<>();
    @Getter
    @Nullable
    private final IMEPatternPartMachine patternMachine;
    @Getter
    private final IMEIOPartMachine ioMachine;

    public MERecipeHandlePart(IMEIOPartMachine machine) {
        this.ioMachine = machine;
        if (machine instanceof IMEPatternPartMachine patternPartMachine) {
            this.patternMachine = patternPartMachine;
        } else {
            this.patternMachine = null;
        }
    }

    public static MERecipeHandlePart of(IMEIOPartMachine machine) {
        MERecipeHandlePart rhl = new MERecipeHandlePart(machine);
        rhl.addMEHandlers(machine.getMERecipeHandlerTraits());
        return rhl;
    }

    public void addMEHandlers(Iterable<IMERecipeHandlerTrait<? extends Predicate<?>, ?>> handlers) {
        for (var handler : handlers) {
            getMeHandlerMap().putIfAbsent(handler.getCapability(), handler);
        }
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

    public @NotNull IMERecipeHandler<? extends Predicate<?>, ?> getMECapability(RecipeCapability<?> cap) {
        return getMeHandlerMap().getOrDefault(cap, null);
    }

    public void restoreMachineCache(Map<GTRecipe, IRecipeHandlePart> map) {
        this.patternMachine.restoreMachineCache(map, this);
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
            // 当取出再原地放回样板时，RecipeHandle侧的slotMap会先从该方法尝试handle原slot
            // 此时会handle成功，导致对应slot的cache永远无法更新(只在meHandleRecipe这一方法中更新)
            // 因此在此处特别添加判断，防止此类情形
            if (!patternMachine.hasCacheInSlot(trySlot)) {
                patternMachine.setSlotCacheRecipe(trySlot, recipe);
            }
            return true;
        }
        return false;
    }

    public Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> meHandleOutput(Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> contents, boolean simulate) {
        boolean hasOutput = false;
        for (var it = Reference2ObjectMaps.fastIterator(contents); it.hasNext();) {
            var entry = it.next();
            var content = entry.getValue();
            if (content.isEmpty()) {
                it.remove();
                continue;
            }
            var cap = entry.getKey();
            var meHandler = getMECapability(cap);
            var result = meHandler.meHandleRecipeOutput(content, simulate);
            if (result.size() != content.size()) {
                hasOutput = true;
                if (result.isEmpty()) it.remove();
                else entry.setValue(new ObjectArrayList<>(result));
            }
        }
        if (!simulate && hasOutput) ioMachine.notifySelfIO();
        return contents;
    }

    @Override
    public IO getHandlerIO() {
        return ioMachine.getIO();
    }

    @Override
    public <T extends Predicate<S>, S> Object2LongMap<S> getContent(RecipeCapability<T> cap) {
        return getMEContent(cap);
    }
}
