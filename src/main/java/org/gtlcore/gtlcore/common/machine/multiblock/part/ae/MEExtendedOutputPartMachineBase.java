package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.client.gui.widget.MEOutListGridWidget;
import org.gtlcore.gtlcore.config.ConfigHolder;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class MEExtendedOutputPartMachineBase extends MEIOPartMachine {

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEExtendedOutputPartMachineBase.class, MEIOPartMachine.MANAGED_FIELD_HOLDER);

    protected final static int MIN_FREQUENCY = ConfigHolder.INSTANCE.MEPatternOutputMin;
    protected final static int MAX_FREQUENCY = ConfigHolder.INSTANCE.MEPatternOutputMax;

    @Getter
    protected final Object2LongOpenHashMap<AEKey> buffer = new Object2LongOpenHashMap<>();

    @Getter
    protected final NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> itemOutputHandler;

    @Getter
    protected final NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> fluidOutputHandler;

    @Persisted
    @Getter
    @Setter
    protected int priority = 10;

    public MEExtendedOutputPartMachineBase(IMachineBlockEntity holder) {
        super(holder, IO.OUT);
        itemOutputHandler = createItemOutputHandler();
        fluidOutputHandler = createFluidOutputHandler();
        registerDefaultServices();
    }

    @Override
    public @NotNull Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(new Position(0, 0));
        // ME Network status
        group.addWidget(new LabelWidget(0, 0, () -> this.isOnline ?
                "gtceu.gui.me_network.online" :
                "gtceu.gui.me_network.offline"));

        group.addWidget(new MEOutListGridWidget(5, 20, 7, this.buffer));
        return group;
    }

    // ========================================
    // ME Output Handlers && Tick Service
    // ========================================

    @Override
    public Iterable<IMERecipeHandlerTrait<?, ?>> getMERecipeHandlerTraits() {
        return List.of(itemOutputHandler, fluidOutputHandler);
    }

    protected abstract NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> createItemOutputHandler();

    protected abstract NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> createFluidOutputHandler();

    protected void registerDefaultServices() {}

    // ========================================
    // Filter && Priority SYSTEM
    // ========================================

    protected void notifyHandlers() {
        itemOutputHandler.notifyListeners();
        fluidOutputHandler.notifyListeners();
    }

    protected void updatePriority() {
        for (var controller : this.getControllers()) {
            if (controller instanceof IRecipeCapabilityMachine machine) {
                machine.sortMEOutput();
            }
        }
    }

    // ========================================
    // Persist
    // ========================================

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

    protected abstract class MEItemOutputHandler extends NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> {

        public MEItemOutputHandler(MEExtendedOutputPartMachineBase machine) {
            super(machine);
        }

        public MEExtendedOutputPartMachineBase getMachine() {
            return (MEExtendedOutputPartMachineBase) this.machine;
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
        public int getPriority() {
            return priority;
        }
    }

    public abstract class MEFluidOutputHandler extends NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> {

        public MEFluidOutputHandler(MEExtendedOutputPartMachineBase machine) {
            super(machine);
        }

        public MEExtendedOutputPartMachineBase getMachine() {
            return (MEExtendedOutputPartMachineBase) this.machine;
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
        public int getPriority() {
            return priority;
        }
    }
}
