package org.gtlcore.gtlcore.integration.ae2;

import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.InputTemplate;
import appeng.crafting.inv.ICraftingInventory;
import appeng.crafting.pattern.AEProcessingPattern;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static appeng.crafting.execution.CraftingCpuHelper.*;

public final class Ae2CompatMH {

    private static final String ELAPSED_TRACKER = "appeng.crafting.execution.ElapsedTimeTracker";

    @FunctionalInterface
    public interface AddMax {

        void invoke(Object tracker, long amount, AEKeyType type);
    }

    private static final AddMax ADD_MAX_ITEMS_FN;

    static {
        AddMax add = null;

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class<?> tracker = Class.forName(ELAPSED_TRACKER);

            MethodHandle mhAdd = findVirtualOrUnReflect(
                    lookup,
                    tracker,
                    "addMaxItems",
                    MethodType.methodType(void.class, long.class, AEKeyType.class));
            if (mhAdd != null) {
                mhAdd = mhAdd.asType(MethodType.methodType(void.class, Object.class, long.class, AEKeyType.class));
                add = MethodHandleProxies.asInterfaceInstance(AddMax.class, mhAdd);
            }

        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }

        ADD_MAX_ITEMS_FN = add;
    }

    public static KeyCounter[] extractPatternInputs5Args(IPatternDetails details,
                                                         ICraftingInventory sourceInv,
                                                         Level level,
                                                         KeyCounter expectedOutputs,
                                                         KeyCounter expectedContainerItems) {
        return extractPatternInputs5Args(details, sourceInv, level, expectedOutputs, expectedContainerItems, 1);
    }

    public static KeyCounter[] extractPatternInputs5Args(IPatternDetails details,
                                                         ICraftingInventory sourceInv,
                                                         Level level,
                                                         KeyCounter expectedOutputs,
                                                         KeyCounter expectedContainerItems,
                                                         long multiply) {
        if (details instanceof AEProcessingPattern processingPattern) return AEUtils.extractForProcessingPattern(processingPattern, sourceInv, expectedOutputs);
        var inputs = details.getInputs();
        KeyCounter[] inputHolder = new KeyCounter[inputs.length];
        boolean found = true;

        for (int x = 0; x < inputs.length; x++) {
            var list = inputHolder[x] = new KeyCounter();
            long remainingMultiplier = Math.multiplyExact(inputs[x].getMultiplier(), multiply);
            for (var template : getValidItemTemplates(sourceInv, inputs[x], level)) {
                long extracted = extractTemplates(sourceInv, template, remainingMultiplier);
                list.add(template.key(), extracted * template.amount());

                var containerItem = inputs[x].getRemainingKey(template.key());
                if (containerItem != null) {
                    expectedContainerItems.add(containerItem, extracted);
                }

                remainingMultiplier -= extracted;
                if (remainingMultiplier == 0)
                    break;
            }

            if (remainingMultiplier > 0) {
                found = false;
                break;
            }
        }

        if (!found) {
            reinjectPatternInputs(sourceInv, inputHolder);
            return null;
        }

        for (var output : details.getOutputs()) {
            expectedOutputs.add(output.what(), Math.multiplyExact(output.amount(), multiply));
        }

        return inputHolder;
    }

    public static KeyCounter[] extractPatternInputs4Args(IPatternDetails details,
                                                         ICraftingInventory sourceInv,
                                                         Level level,
                                                         KeyCounter expectedOutputs) {
        return extractPatternInputs4Args(details, sourceInv, level, expectedOutputs, 1);
    }

    public static KeyCounter[] extractPatternInputs4Args(IPatternDetails details,
                                                         ICraftingInventory sourceInv,
                                                         Level level,
                                                         KeyCounter expectedOutputs,
                                                         long multiply) {
        if (details instanceof AEProcessingPattern processingPattern) return AEUtils.extractForProcessingPattern(processingPattern, sourceInv, expectedOutputs);
        var inputs = details.getInputs();
        KeyCounter[] inputHolder = new KeyCounter[inputs.length];
        boolean found = true;

        for (int x = 0; x < inputs.length; ++x) {
            KeyCounter list = inputHolder[x] = new KeyCounter();
            long remainingMultiplier = Math.multiplyExact(inputs[x].getMultiplier(), multiply);
            for (InputTemplate template : getValidItemTemplates(sourceInv, inputs[x], level)) {
                long extracted = extractTemplates(sourceInv, template, remainingMultiplier);
                list.add(template.key(), extracted * template.amount());

                var containerItem = inputs[x].getRemainingKey(template.key());
                if (containerItem != null) {
                    expectedOutputs.add(containerItem, extracted);
                }

                remainingMultiplier -= extracted;
                if (remainingMultiplier == 0L) {
                    break;
                }
            }

            if (remainingMultiplier > 0L) {
                found = false;
                break;
            }
        }

        if (!found) {
            reinjectPatternInputs(sourceInv, inputHolder);
            return null;
        } else {
            for (GenericStack output : details.getOutputs()) {
                expectedOutputs.add(output.what(), Math.multiplyExact(output.amount(), multiply));
            }

            return inputHolder;
        }
    }

    public static void elapsedTimeTrackerAddMaxItems(Object tracker, long amount, AEKeyType type) {
        ADD_MAX_ITEMS_FN.invoke(tracker, amount, type);
    }

    private static MethodHandle findVirtualOrUnReflect(MethodHandles.Lookup lookup,
                                                       Class<?> owner,
                                                       String name,
                                                       MethodType type) {
        try {
            MethodHandles.Lookup pl = MethodHandles.privateLookupIn(owner, lookup);
            return pl.findVirtual(owner, name, type);
        } catch (Throwable ignored) {
            try {
                var m = owner.getDeclaredMethod(name, type.parameterArray());
                m.setAccessible(true);
                return lookup.unreflect(m);
            } catch (ReflectiveOperationException e2) {
                return null;
            }
        }
    }
}
