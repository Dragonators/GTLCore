package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.config.ConfigHolder;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;

import java.lang.ref.WeakReference;
import java.util.List;

import static org.gtlcore.gtlcore.common.machine.multiblock.part.ae.AEUtils.reFunds;

public class MEExtendedAsyncOutputPartMachine extends MEExtendedOutputPartMachine {

    private final AEAccumulator accumulator = new AEAccumulator();
    private final WeakReference<AEAccumulator> accRef = new WeakReference<>(accumulator);

    public MEExtendedAsyncOutputPartMachine(IMachineBlockEntity holder) {
        super(holder);
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
                if (!getMainNode().isActive()) {
                    return TickRateModulation.SLEEP;
                }

                accumulator.drainTo(buffer);

                if (buffer.isEmpty()) {
                    if (ticksSinceLastCall >= ConfigHolder.INSTANCE.MEPatternOutputMax) {
                        isSleeping = true;
                        return TickRateModulation.SLEEP;
                    } else return TickRateModulation.SLOWER;
                } else return reFunds(buffer, getMainNode().getGrid(), actionSource) ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
            }
        });
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        accumulator.clear();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!isRemote()) {
            accumulator.drainTo(buffer);
        }
    }
}
