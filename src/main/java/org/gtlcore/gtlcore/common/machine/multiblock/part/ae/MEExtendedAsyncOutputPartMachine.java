package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.gtlcore.gtlcore.common.machine.multiblock.part.ae.AEUtils.reFunds;

public class MEExtendedAsyncOutputPartMachine extends MEExtendedOutputPartMachine {

    private final AEAccumulator accumulator = new AEAccumulator();
    private final WeakReference<AEAccumulator> accRef = new WeakReference<>(accumulator);
    private final AtomicReference<Object2LongOpenHashMap<AEKey>> pendingData = new AtomicReference<>();
    private final AtomicBoolean drainRequested = new AtomicBoolean(false);

    public MEExtendedAsyncOutputPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    private void requestAsyncDrain() {
        if (pendingData.get() == null && drainRequested.compareAndSet(false, true)) {
            AEWriteService.INSTANCE.prepareDrainedData(accRef, pendingData, drainRequested);
        }
    }

    private boolean mergeFromPendingData() {
        Object2LongOpenHashMap<AEKey> data = pendingData.getAndSet(null);
        if (data != null && !data.isEmpty()) {
            data.object2LongEntrySet().fastForEach(e -> buffer.addTo(e.getKey(), e.getLongValue()));
            return true;
        }
        return false;
    }

    @Override
    protected NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> createItemOutputHandler() {
        return new MEItemOutputHandler(this) {

            @Override
            public boolean meHandleRecipeOutputInner(List<Ingredient> left, boolean simulate) {
                if (simulate) return true; // Todo Filter
                AEWriteService.INSTANCE.submitIngredientLeft(accRef, left);
                return true;
            }
        };
    }

    @Override
    protected NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> createFluidOutputHandler() {
        return new MEFluidOutputHandler(this) {

            @Override
            public boolean meHandleRecipeOutputInner(List<FluidIngredient> left, boolean simulate) {
                if (simulate) return true; // Todo Filter
                AEWriteService.INSTANCE.submitFluidIngredientLeft(accRef, left);
                return true;
            }
        };
    }

    @Override
    protected void registerDefaultServices() {
        getMainNode().addService(IGridTickable.class, new Ticker() {

            @Override
            public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
                final boolean isActive = getMainNode().isActive();
                final boolean dataMerged = mergeFromPendingData();
                final boolean hasPendingWork = pendingData.get() != null || !accumulator.isEmpty();

                if (hasPendingWork) {
                    requestAsyncDrain();
                }

                if (!isActive) {
                    if (hasPendingWork) {
                        return TickRateModulation.FASTER;
                    } else {
                        if (ticksSinceLastCall >= MAX_FREQUENCY) {
                            isSleeping = true;
                            return TickRateModulation.SLEEP;
                        } else return TickRateModulation.SLOWER;
                    }
                }

                if (buffer.isEmpty()) {
                    if (hasPendingWork) {
                        return TickRateModulation.FASTER;
                    }
                    if (ticksSinceLastCall >= MAX_FREQUENCY) {
                        isSleeping = true;
                        return TickRateModulation.SLEEP;
                    } else return TickRateModulation.SLOWER;
                } else {
                    if (reFunds(buffer, getMainNode().getGrid(), actionSource) || dataMerged) {
                        return TickRateModulation.URGENT;
                    } else {
                        return TickRateModulation.SLOWER;
                    }
                }
            }
        });
    }

    @Override
    public void onMachineRemoved() {
        accumulator.clear();
        super.onMachineRemoved();
    }
}
