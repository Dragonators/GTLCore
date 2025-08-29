package org.gtlcore.gtlcore.api.machine.trait;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import javax.annotation.Nullable;

public interface IMEPartMachine {

    default @Nullable Object2LongMap<ItemStack> getMEItemMap() {
        return null;
    }

    default Object2LongOpenCustomHashMap<ItemStack> getItemMap() {
        return null;
    }

    default @Nullable Object2LongOpenHashMap<FluidStack> getFluidMap() {
        return null;
    }

    default void setChanged(boolean value) {}

    default boolean getChanged() {
        return false;
    }
}
