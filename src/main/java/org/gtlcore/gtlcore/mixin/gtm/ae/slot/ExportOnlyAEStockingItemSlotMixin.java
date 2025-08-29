package org.gtlcore.gtlcore.mixin.gtm.ae.slot;

import org.gtlcore.gtlcore.integration.ae2.slot.LongAEStockingSlot;

import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;

import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(targets = "com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine$ExportOnlyAEStockingItemSlot", remap = false)
public class ExportOnlyAEStockingItemSlotMixin implements LongAEStockingSlot {

    @SuppressWarnings("target")
    @Shadow(remap = false)
    @Final
    MEStockingBusPartMachine this$0;

    @Unique
    public long extractLong(int slot, long amount, boolean simulate, boolean notifyChanges) {
        final var accessor = (ExportOnlyAESlotAccessor) this;
        if (slot == 0 && accessor.getStock() != null && accessor.getConfig() != null) {
            if (!this$0.isOnline()) {
                return 0;
            }

            MEStorage aeNetwork = Objects.requireNonNull(this$0.getMainNode().getGrid()).getStorageService().getInventory();
            AEKey key = accessor.getConfig().what();
            if (key instanceof AEItemKey) {
                long extracted = aeNetwork.extract(key, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, this$0.getActionSource());
                if (extracted > 0L) {
                    if (!simulate) {
                        accessor.setStock(ExportOnlyAESlot.copy(accessor.getStock(), accessor.getStock().amount() - extracted));
                        if (accessor.getStock().amount() == 0L) {
                            accessor.setStock(null);
                        }

                        if (notifyChanges && accessor.getOnContentsChanged() != null) {
                            accessor.getOnContentsChanged().run();
                        }
                    }
                    return extracted;
                }
            }
        }
        return 0;
    }

    @Override
    public @Nullable Pair<ItemStack, Long> getStackWithLongInSlot() {
        final var stock = ((ExportOnlyAESlotAccessor) this).getStock();
        if (stock != null && stock.amount() > 0L) {
            return stock.what() instanceof AEItemKey itemKey ? Pair.of(itemKey.toStack(), stock.amount()) : null;
        }
        return null;
    }
}
