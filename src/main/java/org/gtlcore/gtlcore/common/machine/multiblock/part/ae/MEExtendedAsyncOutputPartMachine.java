package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.config.ConfigHolder;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.gtlcore.gtlcore.common.machine.multiblock.part.ae.AEUtils.reFunds;

public class MEExtendedAsyncOutputPartMachine extends MEExtendedOutputPartMachine {

    private final AEUtils.AEAccumulator accumulator = new AEUtils.AEAccumulator();
    private final AEUtils.Writer writer = new AEUtils.Writer(accumulator, "ME-Writer-" + hashCode());

    public MEExtendedAsyncOutputPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    protected NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> createItemOutputHandler() {
        return new MEItemOutputHandler(this) {

            @Override
            public boolean meHandleRecipeOutputInner(List<Ingredient> left, boolean simulate) {
                if (simulate) return true; // Todo Filter
                writer.submitIngredientLeft(left);
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
                writer.submitFluidIngredientLeft(left);
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
        try {
            writer.close();
        } catch (Exception ignored) {}
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) writer.ensureAlive();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!isRemote()) {
            try {
                writer.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        accumulator.drainTo(buffer);
        super.saveCustomPersistedData(tag, forDrop);
    }
}
