package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;
import org.gtlcore.gtlcore.config.ConfigHolder;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.gtlcore.gtlcore.common.machine.multiblock.part.ae.AEUtils.reFunds;

public class MEExtendedOutputPartMachine extends MEIOPartMachine {

    @Getter
    private final Object2LongOpenHashMap<AEKey> buffer = new Object2LongOpenHashMap<>();

    @Getter
    protected final NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> itemOutputHandler;

    @Getter
    protected final NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> fluidOutputHandler;

    public MEExtendedOutputPartMachine(IMachineBlockEntity holder) {
        super(holder, IO.OUT);
        itemOutputHandler = new MEItemOutputHandler(this);
        fluidOutputHandler = new MEFluidOutputHandler(this);
        getMainNode().addService(IGridTickable.class, new Ticker());;
    }

    public Iterable<IMERecipeHandlerTrait<?, ?>> getMERecipeHandlerTraits() {
        return List.of(itemOutputHandler, fluidOutputHandler);
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);

        if (buffer.isEmpty()) return;

        ListTag listTag = new ListTag();
        for (var entry : buffer.object2LongEntrySet()) {
            var aeKey = entry.getKey();
            long amount = entry.getLongValue();
            if (amount > 0) {
                var keyTag = aeKey.toTagGeneric();
                keyTag.putLong("amount", amount);
                listTag.add(keyTag);
            }
        }

        if (!listTag.isEmpty()) {
            tag.put("buffer", listTag);
        }
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);

        buffer.clear();
        ListTag listTag = tag.getList("buffer", Tag.TAG_COMPOUND);
        for (Tag t : listTag) {
            if (!(t instanceof CompoundTag keyTag)) continue;
            var aeKey = AEKey.fromTagGeneric(keyTag);
            long amount = keyTag.getLong("amount");
            if (aeKey != null && amount > 0) {
                buffer.put(aeKey, amount);
            }
        }
    }

    protected class MEItemOutputHandler extends NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> {

        public MEItemOutputHandler(MEExtendedOutputPartMachine machine) {
            super(machine);
        }

        public MEExtendedOutputPartMachine getMachine() {
            return (MEExtendedOutputPartMachine) this.machine;
        }

        @Override
        public RecipeCapability<Ingredient> getCapability() {
            return ItemRecipeCapability.CAP;
        }

        @Override
        public IO getIo() {
            return IO.OUT;
        }

        // region Unused
        @Override
        public List<Integer> getActiveSlots() {
            return List.of();
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveAndUnCachedSlotsLimitContentsMap() {
            return Int2ObjectMaps.emptyMap();
        }

        @Override
        public Object2LongMap<ItemStack> getCustomSlotsStackMap(List<Integer> list) {
            return Object2LongMaps.emptyMap();
        }

        @Override
        public boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<Ingredient> left, boolean simulate, int trySlot) {
            return false;
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<Ingredient> left, boolean simulate) {}

        @Override
        public Object2LongMap<Ingredient> getPreparedMEHandleContents() {
            return Object2LongMaps.emptyMap();
        }
        // endregion

        @Override
        public boolean meHandleRecipeOutputInner(List<Ingredient> left, boolean simulate) {
            if (simulate) return true; // Todo Filter
            for (Ingredient ingredient : left) {
                if (ingredient instanceof IntProviderIngredient intProvider) {
                    intProvider.setItemStacks(null);
                    intProvider.setSampledCount(null);
                }

                ItemStack[] items = ingredient.getItems();
                if (items.length != 0) {
                    ItemStack output = items[0];
                    if (!output.isEmpty()) {
                        buffer.addTo(AEItemKey.of(output), ingredient instanceof LongIngredient longIngredient ? longIngredient.getActualAmount() : output.getCount());
                    }
                }
            }
            return true;
        }
    }

    public class MEFluidOutputHandler extends NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> {

        public MEFluidOutputHandler(MEExtendedOutputPartMachine machine) {
            super(machine);
        }

        public MEExtendedOutputPartMachine getMachine() {
            return (MEExtendedOutputPartMachine) this.machine;
        }

        @Override
        public RecipeCapability<FluidIngredient> getCapability() {
            return FluidRecipeCapability.CAP;
        }

        @Override
        public IO getIo() {
            return IO.OUT;
        }

        // region Unused
        @Override
        public List<Integer> getActiveSlots() {
            return List.of();
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveAndUnCachedSlotsLimitContentsMap() {
            return Int2ObjectMaps.emptyMap();
        }

        @Override
        public Object2LongMap<FluidStack> getCustomSlotsStackMap(List<Integer> list) {
            return Object2LongMaps.emptyMap();
        }

        @Override
        public boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<FluidIngredient> left, boolean simulate, int trySlot) {
            return false;
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<FluidIngredient> left, boolean simulate) {}

        @Override
        public Object2LongMap<FluidIngredient> getPreparedMEHandleContents() {
            return Object2LongMaps.emptyMap();
        }
        // endregion

        @Override
        public boolean meHandleRecipeOutputInner(List<FluidIngredient> left, boolean simulate) {
            if (simulate) return true; // Todo Filter
            for (FluidIngredient fluidIngredient : left) {
                if (!fluidIngredient.isEmpty()) {
                    FluidStack[] fluids = fluidIngredient.getStacks();
                    if (fluids.length != 0) {
                        FluidStack output = fluids[0];
                        buffer.addTo(AEFluidKey.of(output.getFluid()), output.getAmount());
                    }
                }
            }
            return true;
        }
    }

    protected class Ticker implements IGridTickable {

        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(ConfigHolder.INSTANCE.MEPatternOutputMin, ConfigHolder.INSTANCE.MEPatternOutputMax, false, true);
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            if (!getMainNode().isActive()) {
                return TickRateModulation.SLEEP;
            }

            if (buffer.isEmpty()) {
                if (ticksSinceLastCall >= ConfigHolder.INSTANCE.MEPatternOutputMax) {
                    isSleeping = true;
                    return TickRateModulation.SLEEP;
                } else return TickRateModulation.SLOWER;
            } else return reFunds(buffer, getMainNode().getGrid(), actionSource) ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
    }
}
