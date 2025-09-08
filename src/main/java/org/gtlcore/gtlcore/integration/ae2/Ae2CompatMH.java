package org.gtlcore.gtlcore.integration.ae2;

import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ICraftingInventory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class Ae2CompatMH {

    private static final String CPU_HELPER = "appeng.crafting.execution.CraftingCpuHelper";
    private static final String ELAPSED_TRACKER = "appeng.crafting.execution.ElapsedTimeTracker";

    @FunctionalInterface
    public interface Extract5 {

        KeyCounter[] invoke(IPatternDetails details,
                            ICraftingInventory sourceInv,
                            Level level,
                            KeyCounter expectedOutputs,
                            KeyCounter expectedContainerItems);
    }

    @FunctionalInterface
    public interface Extract4 {

        KeyCounter[] invoke(IPatternDetails details,
                            ICraftingInventory sourceInv,
                            Level level,
                            KeyCounter expectedOutputs);
    }

    @FunctionalInterface
    public interface AddMax {

        void invoke(Object tracker, long amount, AEKeyType type);
    }

    private static final Extract5 EXTRACT_5_FN;
    private static final Extract4 EXTRACT_4_FN;
    private static final AddMax ADD_MAX_ITEMS_FN;

    static {
        Extract5 e5 = null;
        Extract4 e4 = null;
        AddMax add = null;

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class<?> cpu = Class.forName(CPU_HELPER);
            final Class<?> tracker = Class.forName(ELAPSED_TRACKER);

            MethodHandle mhExtract5 = findStaticOrUnReflect(
                    lookup,
                    cpu,
                    "extractPatternInputs",
                    MethodType.methodType(
                            KeyCounter[].class,
                            IPatternDetails.class, ICraftingInventory.class, Level.class,
                            KeyCounter.class, KeyCounter.class));
            if (mhExtract5 != null) {
                e5 = MethodHandleProxies.asInterfaceInstance(Extract5.class, mhExtract5);
            }

            MethodHandle mhExtract4 = findStaticOrUnReflect(
                    lookup,
                    cpu,
                    "extractPatternInputs",
                    MethodType.methodType(
                            KeyCounter[].class,
                            IPatternDetails.class, ICraftingInventory.class, Level.class,
                            KeyCounter.class));
            if (mhExtract4 != null) {
                e4 = MethodHandleProxies.asInterfaceInstance(Extract4.class, mhExtract4);
            }

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

        EXTRACT_5_FN = e5;
        EXTRACT_4_FN = e4;
        ADD_MAX_ITEMS_FN = add;

        if (EXTRACT_5_FN == null && EXTRACT_4_FN == null) {
            throw new ExceptionInInitializerError(
                    "No compatible CraftingCpuHelper.extractPatternInputs signature found");
        }
    }

    /** 可选：暴露独立的 5/4 参版本（若你在上层显式分支调用） */
    public static KeyCounter[] extractPatternInputs5Args(IPatternDetails details,
                                                         ICraftingInventory sourceInv,
                                                         Level level,
                                                         KeyCounter expectedOutputs,
                                                         KeyCounter expectedContainerItems) {
        return EXTRACT_5_FN.invoke(details, sourceInv, level, expectedOutputs, expectedContainerItems);
    }

    public static KeyCounter[] extractPatternInputs4Args(IPatternDetails details,
                                                         ICraftingInventory sourceInv,
                                                         Level level,
                                                         KeyCounter expectedOutputs) {
        return EXTRACT_4_FN.invoke(details, sourceInv, level, expectedOutputs);
    }

    public static void elapsedTimeTrackerAddMaxItems(Object tracker, long amount, AEKeyType type) {
        ADD_MAX_ITEMS_FN.invoke(tracker, amount, type);
    }

    private static MethodHandle findStaticOrUnReflect(MethodHandles.Lookup lookup,
                                                      Class<?> owner,
                                                      String name,
                                                      MethodType type) {
        try {
            return lookup.findStatic(owner, name, type);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            try {
                var m = owner.getDeclaredMethod(name, type.parameterArray());
                m.setAccessible(true);
                return lookup.unreflect(m);
            } catch (ReflectiveOperationException e2) {
                return null;
            }
        }
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
