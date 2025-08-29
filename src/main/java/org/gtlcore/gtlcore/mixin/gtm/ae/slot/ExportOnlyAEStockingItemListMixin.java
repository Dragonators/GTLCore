package org.gtlcore.gtlcore.mixin.gtm.ae.slot;

import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;
import org.gtlcore.gtlcore.integration.ae2.slot.LongAEStockingSlot;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Function;

@Mixin(targets = "com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine$ExportOnlyAEStockingItemList", remap = false)
public abstract class ExportOnlyAEStockingItemListMixin extends ExportOnlyAEItemListMixin {

    public ExportOnlyAEStockingItemListMixin(MetaMachine machine, int slots, @NotNull IO handlerIO, @NotNull IO capabilityIO, Function<Integer, ItemStackTransfer> transferFactory) {
        super(machine, slots, handlerIO, capabilityIO, transferFactory);
    }

    @Override
    public List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe, List<Ingredient> left, @Nullable String slotName, boolean simulate) {
        if (io == IO.IN) {
            boolean changed = false;
            var listIterator = left.listIterator();
            while (listIterator.hasNext()) {
                Ingredient ingredient = listIterator.next();
                if (ingredient.isEmpty()) {
                    listIterator.remove();
                } else {
                    long amount;
                    if (ingredient instanceof LongIngredient li) amount = li.getActualAmount();
                    else if (ingredient instanceof SizedIngredient si) amount = si.getAmount();
                    else amount = 1;
                    if (amount < 1) listIterator.remove();
                    else {
                        for (ExportOnlyAEItemSlot i : this.inventory) {
                            GenericStack stored = i.getStock();
                            if (stored != null && stored.amount() != 0) {
                                if (ingredient.test(i.getStackInSlot(0)) && i instanceof LongAEStockingSlot longAEStockingSlot) {
                                    long extracted = longAEStockingSlot.extractLong(0, amount, simulate, !simulate);
                                    if (extracted > 0) {
                                        changed = true;
                                        amount -= extracted;
                                    }
                                }
                                if (amount <= 0L) {
                                    listIterator.remove();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (!simulate && changed) {
                this.changed = true;
                this.onContentsChanged();
            }
        }
        return left.isEmpty() ? null : left;
    }

    @Override
    public Object2LongOpenCustomHashMap<ItemStack> getMEItemMap() {
        if (getChanged()) {
            setChanged(false);
            getItemMap().clear();
            for (var slot : inventory) {
                if (slot instanceof LongAEStockingSlot longAEStockingSlot) {
                    var pair = longAEStockingSlot.getStackWithLongInSlot();
                    if (pair != null) {
                        this.getItemMap().addTo(pair.left(), pair.right());
                    }
                }
            }
        }
        return getItemMap().isEmpty() ? null : getItemMap();
    }
}
