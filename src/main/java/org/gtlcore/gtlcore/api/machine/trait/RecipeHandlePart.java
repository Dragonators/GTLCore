package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.machine.trait.MEStock.IMEPartMachine;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;

import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystItemStackHandler;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public class RecipeHandlePart implements IRecipeHandlePart {

    public static final RecipeHandlePart NO_DATA = new RecipeHandlePart(IO.NONE);

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

    public RecipeHandlePart(IO io) {
        this.handlerIO = io;
    }

    public static RecipeHandlePart of(IO io, Iterable<IRecipeHandler<?>> handlers) {
        RecipeHandlePart rhl = new RecipeHandlePart(io);
        rhl.addHandlers(handlers);
        return rhl;
    }

    @SuppressWarnings("unchecked")
    public <T extends Predicate<S>, S> Object2LongMap<S> getContent(RecipeCapability<T> cap) {
        return (Object2LongMap<S>) this.initializeContent(cap);
    }

    public Object2LongMap<?> initializeContent(RecipeCapability<?> cap) {
        if (cap == ItemRecipeCapability.CAP) {
            itemContent = new Object2LongOpenHashMap<>();
            for (var item : this.getCapability(cap)) {
                if (item instanceof CatalystItemStackHandler || item instanceof NotifiableCircuitItemStackHandler) continue;
                if (item instanceof IMEPartMachine aeItemHandler) {
                    final var map = aeItemHandler.getMEItemMap();
                    if (map != null) {
                        for (var it = Object2LongMaps.fastIterator(map); it.hasNext();) {
                            var entry = it.next();
                            itemContent.addTo(entry.getKey(), entry.getLongValue());
                        }
                    }
                } else {
                    for (var o : item.getContents()) {
                        if (o instanceof ItemStack stack) {
                            itemContent.addTo(stack, stack.getCount());
                        }
                    }
                }
            }
            return itemContent;
        } else if (cap == FluidRecipeCapability.CAP) {
            fluidContent = new Object2LongOpenHashMap<>();
            for (var fluid : this.getCapability(cap)) {
                if (fluid instanceof CatalystFluidStackHandler) continue;
                for (var o : fluid.getContents()) {
                    if (o instanceof FluidStack stack) {
                        fluidContent.addTo(stack, stack.getAmount());
                    }
                }
            }
            return fluidContent;
        }
        return Object2LongMaps.EMPTY_MAP;
    }

    public void addHandlers(Iterable<IRecipeHandler<?>> handlers) {
        for (var handler : handlers) {
            getHandlerMap().computeIfAbsent(handler.getCapability(), c -> new ObjectArrayList<>()).add(handler);
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
}
