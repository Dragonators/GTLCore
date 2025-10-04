package org.gtlcore.gtlcore.integration.ae2;

import org.gtlcore.gtlcore.integration.extendedae.ItemTagPriority;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ICraftingInventory;
import appeng.util.prioritylist.IPartitionList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

import static appeng.crafting.execution.CraftingCpuHelper.*;

public final class Ae2CompatMH {

    private static final String ELAPSED_TRACKER = "appeng.crafting.execution.ElapsedTimeTracker";
    private static final String TAG_EXP_PARSER = "com.glodblock.github.extendedae.common.me.taglist.TagExpParser";
    private static final String TAG_PRIORITY_LIST = "com.glodblock.github.extendedae.common.me.taglist.TagPriorityList";

    @FunctionalInterface
    public interface AddMax {

        void invoke(Object tracker, long amount, AEKeyType type);
    }

    @FunctionalInterface
    public interface GetMatchingOre {

        Set<TagKey<?>> invoke(String oreExp);
    }

    @FunctionalInterface
    public interface CreateTagPriorityList {

        IPartitionList invoke(String whiteListExpression, String blackListExpression);
    }

    private static final AddMax ADD_MAX_ITEMS_FN;
    private static final GetMatchingOre GET_MATCHING_ORE_FN;
    private static final CreateTagPriorityList CREATE_TAG_PRIORITY_LIST_FN;

    static {
        AddMax add = null;
        GetMatchingOre getMatchingOre = null;
        CreateTagPriorityList createTagPriorityList = null;

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            // Initialize ElapsedTimeTracker compatibility
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

            // Try to initialize TagExpParser.getMatchingOre (old API)
            try {
                final Class<?> tagExpParser = Class.forName(TAG_EXP_PARSER);
                MethodHandle mhGetMatchingOre = findStaticOrUnReflect(
                        lookup,
                        tagExpParser,
                        "getMatchingOre",
                        MethodType.methodType(Set.class, String.class));
                if (mhGetMatchingOre != null) {
                    getMatchingOre = MethodHandleProxies.asInterfaceInstance(GetMatchingOre.class, mhGetMatchingOre);
                }
            } catch (Throwable ignored) {}

            // Try to initialize TagPriorityList constructor (new API)
            try {
                final Class<?> tagPriorityList = Class.forName(TAG_PRIORITY_LIST);
                MethodHandle mhConstructor = findConstructorOrUnReflect(
                        lookup,
                        tagPriorityList,
                        MethodType.methodType(void.class, String.class, String.class));
                if (mhConstructor != null) {
                    createTagPriorityList = MethodHandleProxies.asInterfaceInstance(CreateTagPriorityList.class, mhConstructor);
                }
            } catch (Throwable ignored) {}

        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }

        ADD_MAX_ITEMS_FN = add;
        GET_MATCHING_ORE_FN = getMatchingOre;
        CREATE_TAG_PRIORITY_LIST_FN = createTagPriorityList;
    }

    public static KeyCounter[] extractForCraftPattern4Args(IPatternDetails details,
                                                           ICraftingInventory sourceInv,
                                                           Level level,
                                                           KeyCounter expectedOutputs) {
        var inputs = details.getInputs();
        KeyCounter[] inputHolder = new KeyCounter[inputs.length];
        boolean found = true;

        for (int x = 0; x < inputs.length; x++) {
            var list = inputHolder[x] = new KeyCounter();
            long remainingMultiplier = inputs[x].getMultiplier();
            for (var template : getValidItemTemplates(sourceInv, inputs[x], level)) {
                long extracted = extractTemplates(sourceInv, template, remainingMultiplier);
                list.add(template.key(), extracted * template.amount());

                var containerItem = inputs[x].getRemainingKey(template.key());
                if (containerItem != null) {
                    expectedOutputs.add(containerItem, extracted);
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

        for (GenericStack output : details.getOutputs()) {
            expectedOutputs.add(output.what(), output.amount());
        }

        return inputHolder;
    }

    public static KeyCounter[] extractForCraftPattern5Args(IPatternDetails details,
                                                           ICraftingInventory sourceInv,
                                                           Level level,
                                                           KeyCounter expectedOutputs,
                                                           KeyCounter expectedContainerItems) {
        var inputs = details.getInputs();
        KeyCounter[] inputHolder = new KeyCounter[inputs.length];
        boolean found = true;

        for (int x = 0; x < inputs.length; x++) {
            var list = inputHolder[x] = new KeyCounter();
            long remainingMultiplier = inputs[x].getMultiplier();
            for (var template : getValidItemTemplates(sourceInv, inputs[x], level)) {
                long extracted = CraftingCpuHelper.extractTemplates(sourceInv, template, remainingMultiplier);
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

        for (GenericStack output : details.getOutputs()) {
            expectedOutputs.add(output.what(), output.amount());
        }

        return inputHolder;
    }

    public static void elapsedTimeTrackerAddMaxItems(Object tracker, long amount, AEKeyType type) {
        ADD_MAX_ITEMS_FN.invoke(tracker, amount, type);
    }

    public static IPartitionList createTagFilter(String tagWhite, String tagBlack) {
        return CREATE_TAG_PRIORITY_LIST_FN == null ? new ItemTagPriority(GET_MATCHING_ORE_FN.invoke(tagWhite), GET_MATCHING_ORE_FN.invoke(tagBlack), tagWhite + tagBlack) : CREATE_TAG_PRIORITY_LIST_FN.invoke(tagWhite, tagBlack);
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

    private static MethodHandle findStaticOrUnReflect(MethodHandles.Lookup lookup,
                                                      Class<?> owner,
                                                      String name,
                                                      MethodType type) {
        try {
            MethodHandles.Lookup pl = MethodHandles.privateLookupIn(owner, lookup);
            return pl.findStatic(owner, name, type);
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

    private static MethodHandle findConstructorOrUnReflect(MethodHandles.Lookup lookup,
                                                           Class<?> owner,
                                                           MethodType type) {
        try {
            MethodHandles.Lookup pl = MethodHandles.privateLookupIn(owner, lookup);
            return pl.findConstructor(owner, type);
        } catch (Throwable ignored) {
            try {
                var c = owner.getDeclaredConstructor(type.parameterArray());
                c.setAccessible(true);
                return lookup.unreflectConstructor(c);
            } catch (ReflectiveOperationException e2) {
                return null;
            }
        }
    }
}
