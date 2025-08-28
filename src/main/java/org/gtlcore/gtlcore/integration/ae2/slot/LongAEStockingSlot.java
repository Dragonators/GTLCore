package org.gtlcore.gtlcore.integration.ae2.slot;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;

public interface LongAEStockingSlot {

    long extractLong(int slot, long amount, boolean simulate, boolean notifyChanges);

    @Nullable
    Pair<ItemStack, Long> getStackWithLongInSlot();
}
