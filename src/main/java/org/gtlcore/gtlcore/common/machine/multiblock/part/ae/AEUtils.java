package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

public class AEUtils {

    /* ==================== 返回 AE 网络 ==================== */
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

    /* ==================== 并发累加器（写线程 -> 主线程） ==================== */

    /** 写线程直接 add；主线程每 tick drain 到单线程的 buffer。 */
    public static final class AEAccumulator {

        private final ConcurrentHashMap<AEKey, LongAdder> acc = new ConcurrentHashMap<>();

        public void add(AEKey key, long delta) {
            if (key == null || delta == 0) return;
            acc.computeIfAbsent(key, k -> new LongAdder()).add(delta);
        }

        public void drainTo(Object2LongOpenHashMap<AEKey> buffer) {
            for (var it = acc.entrySet().iterator(); it.hasNext();) {
                var entry = it.next();
                LongAdder adder = entry.getValue();
                long d = adder.sumThenReset();
                if (d != 0) {
                    buffer.addTo(entry.getKey(), d);
                }
                if (adder.sum() == 0) it.remove();
            }
        }

        public boolean isEmpty() {
            return acc.isEmpty();
        }

        public void clear() {
            acc.clear();
        }
    }

    /* ==================== 写线程 ==================== */

    /**
     * 一个专用的单线程执行器：把 List<Ingredient> 的累加工作放到“写线程”。
     */
    public static final class Writer implements AutoCloseable {

        private ThreadPoolExecutor executor;
        private final AEAccumulator accumulator;
        private final String threadName;
        public static int THREAD_PRIORITY = Thread.NORM_PRIORITY - 5;
        public static int QUEUE_CAPACITY = 64;

        public Writer(AEAccumulator accumulator, String threadName) {
            this.accumulator = Objects.requireNonNull(accumulator);
            this.threadName = threadName;
            this.executor = newExecutor(this.threadName);
        }

        public void submitIngredientLeft(List<Ingredient> left) {
            if (left == null || left.isEmpty()) return;
            executor.execute(() -> {
                for (Ingredient ingredient : left) {
                    if (ingredient instanceof IntProviderIngredient intProvider) {
                        intProvider.setItemStacks(null);
                        intProvider.setSampledCount(null);
                    }

                    ItemStack[] items = ingredient.getItems();
                    if (items.length != 0) {
                        ItemStack output = items[0];
                        if (!output.isEmpty()) {
                            accumulator.add(AEItemKey.of(output), ingredient instanceof LongIngredient longIngredient ? longIngredient.getActualAmount() : output.getCount());
                        }
                    }
                }
            });
        }

        public void submitFluidIngredientLeft(List<FluidIngredient> left) {
            if (left == null || left.isEmpty()) return;
            executor.execute(() -> {
                for (FluidIngredient fluidIngredient : left) {
                    if (!fluidIngredient.isEmpty()) {
                        FluidStack[] fluids = fluidIngredient.getStacks();
                        if (fluids.length != 0) {
                            FluidStack output = fluids[0];
                            accumulator.add(AEFluidKey.of(output.getFluid()), output.getAmount());
                        }
                    }
                }
            });
        }

        public void ensureAlive() {
            ThreadPoolExecutor ex = this.executor;
            if (ex.isShutdown() || ex.isTerminated()) {
                ThreadPoolExecutor neo = newExecutor(this.threadName);
                neo.prestartCoreThread();
                this.executor = neo;
            }
        }

        private static ThreadPoolExecutor newExecutor(String threadName) {
            return new ThreadPoolExecutor(
                    1, 1,
                    30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                    r -> {
                        Thread t = new Thread(r, threadName);
                        t.setDaemon(true);
                        t.setPriority(Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, THREAD_PRIORITY)));
                        return t;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }
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
