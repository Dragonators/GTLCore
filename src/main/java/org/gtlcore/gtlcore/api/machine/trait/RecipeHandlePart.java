package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.capability.IMERecipeHandler;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;

import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystItemStackHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RecipeHandlePart {

    public static final RecipeHandlePart NO_DATA = new RecipeHandlePart(IO.NONE, false);

    public static final Comparator<RecipeHandlePart> COMPARATOR = (h1, h2) -> {
        int cmp = Long.compare(h1.getPriority(), h2.getPriority());
        if (cmp != 0) return cmp;
        boolean b1 = h1.getTotalContentAmount() > 0;
        boolean b2 = h2.getTotalContentAmount() > 0;
        return Boolean.compare(b1, b2);
    };

    @Getter
    private final IO handlerIO;
    @Getter
    private final Reference2ObjectOpenHashMap<RecipeCapability<?>, List<IRecipeHandler<?>>> handlerMap = new Reference2ObjectOpenHashMap<>();
    private final List<IRecipeHandler<?>> allHandlers = new ObjectArrayList<>();
    private Object2LongOpenHashMap<ItemStack> itemContent;
    private Object2LongOpenHashMap<FluidStack> fluidContent;

    public RecipeHandlePart(IO io, boolean isMEHandlePart) {
        this.handlerIO = io;
        this.isMEHandlePart = isMEHandlePart;
    }

    public static RecipeHandlePart of(IO io, Iterable<IRecipeHandler<?>> handlers) {
        RecipeHandlePart rhl = new RecipeHandlePart(io, false);
        rhl.addHandlers(handlers);
        return rhl;
    }

    public Object2LongOpenHashMap<?> getContent(RecipeCapability<?> cap) {
        if (cap == ItemRecipeCapability.CAP) {
            itemContent = (Object2LongOpenHashMap<ItemStack>) this.initializeContent(cap);
            return itemContent;
        } else {
            fluidContent = (Object2LongOpenHashMap<FluidStack>) this.initializeContent(cap);
            return fluidContent;
        }
    }

    public Object2LongOpenHashMap<?> initializeContent(RecipeCapability<?> cap) {
        if (cap == ItemRecipeCapability.CAP) {
            itemContent = new Object2LongOpenHashMap<>();
            for (var item : this.getCapability(cap)) {
                if (item instanceof CatalystItemStackHandler || item instanceof NotifiableCircuitItemStackHandler) continue;
                for (var o : item.getContents()) {
                    if (o instanceof ItemStack stack) {
                        itemContent.computeLong(stack, (k, v) -> v == null ? stack.getCount() : v + stack.getCount());
                    }
                }
            }
        } else if (cap == FluidRecipeCapability.CAP) {
            fluidContent = new Object2LongOpenHashMap<>();
            for (var fluid : this.getCapability(cap)) {
                if (fluid instanceof CatalystFluidStackHandler) continue;
                for (var o : fluid.getContents()) {
                    if (o instanceof FluidStack stack) {
                        fluidContent.computeLong(stack, (k, v) -> v == null ? stack.getAmount() : v + stack.getAmount());
                    }
                }
            }
        }
        if (cap == ItemRecipeCapability.CAP) return itemContent;
        else return fluidContent;
    }

    public void addHandlers(Iterable<IRecipeHandler<?>> handlers) {
        for (var handler : handlers) {
            getHandlerMap().computeIfAbsent(handler.getCapability(), c -> new ArrayList<>()).add(handler);
            allHandlers.add(handler);
        }
        if (handlerIO == IO.OUT) sort();
    }

    private void sort() {
        for (var list : getHandlerMap().values()) {
            list.sort(IRecipeHandler.ENTRY_COMPARATOR);
        }
    }

    public @NotNull List<IRecipeHandler<?>> getCapability(RecipeCapability<?> cap) {
        return getHandlerMap().getOrDefault(cap, Collections.emptyList());
    }

    public long getPriority() {
        long priority = 0;
        for (var handler : allHandlers) priority += handler.getPriority();
        return priority;
    }

    public double getTotalContentAmount() {
        double sum = 0;
        for (var handler : allHandlers) sum += handler.getTotalContentAmount();
        return sum;
    }

    public Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> handleRecipe(IO io, GTRecipe recipe,
                                                                                       Map<RecipeCapability<?>, List<Object>> contents,
                                                                                       boolean simulate) {
        var copy = new Reference2ObjectOpenHashMap<>(contents);
        if (!getHandlerMap().isEmpty()) {
            for (var it = copy.reference2ObjectEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                var handlerList = getCapability(entry.getKey());
                for (var handler : handlerList) {
                    var left = handler.handleRecipe(io, recipe, entry.getValue(), null, simulate);
                    if (left == null) {
                        it.remove();
                        break;
                    } else entry.setValue(new ArrayList<>(left));
                }
            }
        }
        return copy;
    }

    //////////////////////////////////////
    // ********** ME Part ***********//
    //////////////////////////////////////

    @Getter
    private final List<Integer> cacheSlot = new IntArrayList();
    @Getter
    private final boolean isMEHandlePart;
    @Getter
    private final Reference2ObjectOpenHashMap<RecipeCapability<?>, IMERecipeHandler<?>> meHandlerMap = new Reference2ObjectOpenHashMap<>();

    public static RecipeHandlePart of(Iterable<IMERecipeHandler<?>> handlers) {
        RecipeHandlePart rhl = new RecipeHandlePart(IO.IN, true);
        rhl.addMEHandlers(handlers);
        return rhl;
    }

    public void addMEHandlers(Iterable<IMERecipeHandler<?>> handlers) {
        for (var handler : handlers) {
            getMeHandlerMap().putIfAbsent(handler.getCapability(), handler);
        }
    }

    public <T> Object2LongMap<T> getMEContent(RecipeCapability<?> cap) {
        return getMEContent(cap, this.getMECapability(cap).getActiveSlots(cap));
    }

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

    public List<Integer> meHandleRecipe(IO io, GTRecipe recipe,
                                        Map<RecipeCapability<?>, List<Object>> contents,
                                        boolean simulate) {
        var copy = new Reference2ObjectOpenHashMap<>(contents);
        if (!getMeHandlerMap().isEmpty()) {
            List<Integer> leftSlots = null;
            for (var it = copy.reference2ObjectEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                var cap = entry.getKey();
                var meHandler = getMECapability(cap);
                var content = entry.getValue();
                if (leftSlots == null) {
                    leftSlots = meHandler.getActiveSlots(cap);
                }
                leftSlots = meHandler.meHandleRecipe(io, recipe, content, null, simulate, leftSlots);
                if (leftSlots.isEmpty()) {
                    return Collections.emptyList();
                }
            }
            return leftSlots != null ? leftSlots : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public boolean meHandleCacheRecipe(IO io, GTRecipe recipe,
                                       Map<RecipeCapability<?>, List<Object>> contents, List<Integer> trySlots) {
        var copy = new Reference2ObjectOpenHashMap<>(contents);
        if (!getMeHandlerMap().isEmpty() && trySlots != null && !trySlots.isEmpty()) {
            for (var it = copy.reference2ObjectEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                var meHandler = getMECapability(entry.getKey());
                var content = entry.getValue();
                var slots = meHandler.meHandleRecipe(io, recipe, content, null, false, trySlots);
                if (slots.size() != trySlots.size()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
