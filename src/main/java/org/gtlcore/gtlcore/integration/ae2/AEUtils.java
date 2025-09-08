package org.gtlcore.gtlcore.integration.ae2;

import net.minecraft.nbt.*;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.crafting.inv.ICraftingInventory;
import appeng.crafting.pattern.AEProcessingPattern;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static appeng.crafting.execution.CraftingCpuHelper.reinjectPatternInputs;

public class AEUtils {

    private static final int BATCH_SIZE = 64;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    public static boolean reFunds(Object2LongMap<AEKey> buffer, @Nullable IGrid network, IActionSource actionSource) {
        if (buffer.isEmpty()) return false;

        if (network == null) return false;

        final MEStorage networkInv = network.getStorageService().getInventory();
        final var energy = network.getEnergyService();
        int operationsBatched = 0, consecutiveFailures = 0;
        boolean didWork = false;

        for (var it = Object2LongMaps.fastIterator(buffer); it.hasNext() && operationsBatched < BATCH_SIZE;) {
            var entry = it.next();
            long amount = entry.getLongValue();

            if (amount <= 0) {
                it.remove();
                continue;
            }

            long inserted = StorageHelper.poweredInsert(energy, networkInv, entry.getKey(), amount, actionSource);
            operationsBatched++;

            if (inserted > 0) {
                didWork = true;
                consecutiveFailures = 0;
                long left = amount - inserted;
                if (left <= 0) {
                    it.remove();
                } else {
                    entry.setValue(left);
                }
            } else {
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_FAILED_ATTEMPTS) {
                    break;
                }
            }
        }
        return didWork;
    }

    public static <T extends AEKey> ListTag createListTag(Function<T, CompoundTag> keySerializer, Object2LongMap<T> map) {
        ListTag tag = new ListTag();
        for (var it = Object2LongMaps.fastIterator(map); it.hasNext();) {
            var entry = it.next();
            var ct = keySerializer.apply(entry.getKey());
            ct.putLong("real", entry.getLongValue());
            tag.add(ct);
        }
        return tag;
    }

    public static <K> void loadInventory(ListTag tag, Function<CompoundTag, K> keyExtractor, Object2LongMap<K> targetMap) {
        for (Tag t : tag) {
            if (!(t instanceof CompoundTag ct)) continue;
            K key = keyExtractor.apply(ct);
            long value = ct.getLong("real");
            if (key != null && value > 0) {
                targetMap.put(key, value);
            }
        }
    }

    public static <T extends AEKey> ListTag createListTag(Function<T, CompoundTag> keySerializer, ObjectSet<T> map) {
        ListTag tag = new ListTag();
        for (T t : map) {
            var ct = keySerializer.apply(t);
            tag.add(ct);
        }
        return tag;
    }

    public static <K> void loadInventory(ListTag tag, Function<CompoundTag, K> keyExtractor, ObjectSet<K> targetMap) {
        for (Tag t : tag) {
            if (!(t instanceof CompoundTag ct)) continue;
            K key = keyExtractor.apply(ct);
            if (key != null) {
                targetMap.add(key);
            }
        }
    }

    // ========================================
    // ME Processing Pattern Multiply
    // ========================================

    public static void pushInputsToMEPatternBufferInventory(KeyCounter[] inputHolder, IPatternDetails.PatternInputSink inputSink) {
        for (var inputList : inputHolder) {
            for (var input : inputList) {
                inputSink.pushInput(input.getKey(), input.getLongValue());
            }
        }
    }

    public static KeyCounter[] extractForMEPatternBuffer(AEProcessingPattern originDetail,
                                                         ICraftingInventory sourceInv,
                                                         long multiplier,
                                                         KeyCounter expectedOutputs) {
        IPatternDetails.IInput[] inputs = originDetail.getInputs();
        KeyCounter[] inputHolder = new KeyCounter[inputs.length];
        boolean found = true;

        for (int x = 0; x < inputs.length; x++) {
            var list = inputHolder[x] = new KeyCounter();
            AEKey key = inputs[x].getPossibleInputs()[0].what();
            long amount = inputs[x].getMultiplier() * multiplier;
            long extracted = AEUtils.extractTemplates(sourceInv, key, amount);
            list.add(key, extracted);
            if (extracted < amount) {
                found = false;
                break;
            }
        }

        if (!found) {
            reinjectPatternInputs(sourceInv, inputHolder);
            return null;
        } else {
            for (GenericStack output : originDetail.getOutputs()) {
                expectedOutputs.add(output.what(), output.amount() * multiplier);
            }
            return inputHolder;
        }
    }

    private static long extractTemplates(ICraftingInventory inv, AEKey key, long amount) {
        if (amount == 0 || inv.extract(key, amount, Actionable.SIMULATE) == 0) return 0;
        long extracted = inv.extract(key, amount, Actionable.MODULATE);
        if (extracted == 0 || extracted != amount) {
            throw new IllegalStateException("Failed to correctly extract whole number. Invalid simulation!");
        }
        return amount;
    }
}
