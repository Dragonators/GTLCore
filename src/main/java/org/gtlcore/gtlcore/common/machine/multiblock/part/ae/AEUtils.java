package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class AEUtils {

    private static final int BATCH_SIZE = 64;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    public static boolean reFunds(Object2LongMap<AEKey> buffer, @Nullable IGrid network, IActionSource actionSource) {
        if (buffer.isEmpty()) return false;

        if (network == null) return false;

        final MEStorage networkInv = network.getStorageService().getInventory();
        final var energy = network.getEnergyService();
        int operationsBatched = 0;
        int consecutiveFailures = 0;
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
}
