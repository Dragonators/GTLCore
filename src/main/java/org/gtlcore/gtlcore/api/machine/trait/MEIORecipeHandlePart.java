package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.capability.IMERecipeHandler;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;

import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class MEIORecipeHandlePart implements IRecipeHandlePart {

    public static final Comparator<MEIORecipeHandlePart> COMPARATOR = Comparator.comparingInt(h -> sumPriority(h.meHandlerMap));

    protected static int sumPriority(Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandler<?, ?>> meHandlerMap) {
        int sum = 0;
        for (var it = Reference2ObjectMaps.fastIterator(meHandlerMap); it.hasNext();) {
            sum += it.next().getValue().getPriority();
        }
        return sum;
    }

    @Getter
    protected final IMEIOPartMachine ioMachine;
    @Getter
    protected final Reference2ObjectOpenHashMap<RecipeCapability<?>, IMERecipeHandler<? extends Predicate<?>, ?>> meHandlerMap = new Reference2ObjectOpenHashMap<>();

    public MEIORecipeHandlePart(IMEIOPartMachine machine) {
        this.ioMachine = machine;
    }

    public static MEIORecipeHandlePart of(IMEIOPartMachine machine) {
        MEIORecipeHandlePart rhl = new MEIORecipeHandlePart(machine);
        rhl.addMEHandlers(machine.getMERecipeHandlerTraits());
        return rhl;
    }

    public void addMEHandlers(Iterable<IMERecipeHandlerTrait<? extends Predicate<?>, ?>> handlers) {
        for (var handler : handlers) {
            meHandlerMap.putIfAbsent(handler.getCapability(), handler);
        }
    }

    public @NotNull IMERecipeHandler<? extends Predicate<?>, ?> getMECapability(RecipeCapability<?> cap) {
        return meHandlerMap.getOrDefault(cap, null);
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
        return Object2LongMaps.emptyMap();
    }
}
