package org.gtlcore.gtlcore.common.machine.multiblock.part;

import org.gtlcore.gtlcore.api.gui.TurnsConfiguratorButton;
import org.gtlcore.gtlcore.api.machine.trait.IMEPartMachine;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;
import org.gtlcore.gtlcore.client.gui.widget.AEDualConfigWidget;
import org.gtlcore.gtlcore.integration.ae2.slot.LongAEStockingSlot;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.slot.*;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author EasterFG on 2025/3/2
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MEDualHatchStockPartMachine extends MEBusPartMachine implements IDataStickInteractable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MEDualHatchStockPartMachine.class,
            MEBusPartMachine.MANAGED_FIELD_HOLDER);

    protected static final int CONFIG_SIZE = 64;
    protected static final int AUTO_PULL_OFF = 0;
    protected static final int AUTO_PULL_ALL = 1;
    protected static final int AUTO_PULL_ITEM = 2;
    protected static final int AUTO_PULL_FLUID = 3;

    private static final IGuiTexture AUTO_PULL_ALL_ICON = new TextTexture("ALL", 0xFFAA00);
    private static final IGuiTexture AUTO_PULL_ITEM_ICON = new ItemStackTexture(Items.IRON_INGOT);
    private static final IGuiTexture AUTO_PULL_FLUID_ICON = new ItemStackTexture(Items.WATER_BUCKET);

    protected ExportOnlyAEItemList aeItemHandler;

    protected ExportOnlyAEFluidList aeFluidHandler;

    @Persisted
    protected NotifiableFluidTank fluidTank;

    @Setter
    protected int page = 1;

    @DescSynced
    @Persisted
    @Getter
    private int autoPullMode;

    public MEDualHatchStockPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.IN, args);
        fluidTank = createTank();
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.aeItemHandler = new ExportOnlyAEStockingItemList(this, CONFIG_SIZE);
        return this.aeItemHandler;
    }

    protected NotifiableFluidTank createTank() {
        this.aeFluidHandler = new ExportOnlyAEStockingFluidList(this, CONFIG_SIZE);
        return this.aeFluidHandler;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void autoIO() {
        if (!this.isWorkingEnabled()) return;
        if (!shouldSyncME()) return;

        if (updateMEStatus()) {
            syncME();
            updateInventorySubscription();
        }

        if (autoPullMode != AUTO_PULL_OFF && getOffsetTimer() % 50 == 0) {
            refreshList();
            syncME();
        }
    }

    private void refreshList() {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) {
            aeItemHandler.clearInventory(0);
            aeFluidHandler.clearInventory(0);
            return;
        }
        IStorageService storageService = grid.getStorageService();
        MEStorage networkStorage = storageService.getInventory();
        var counter = networkStorage.getAvailableStacks();
        int index = 0;
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            if (index >= CONFIG_SIZE) break;
            AEKey what = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0) continue;
            boolean isItem = what instanceof AEItemKey;
            if (autoPullMode != AUTO_PULL_ALL) {
                if (autoPullMode == AUTO_PULL_ITEM && !isItem) {
                    continue;
                } else if (autoPullMode == AUTO_PULL_FLUID && isItem) {
                    continue;
                }
            }
            long request = networkStorage.extract(what, amount, Actionable.SIMULATE, actionSource);
            if (request == 0) continue;
            if (isItem) {
                this.aeFluidHandler.getInventory()[index].setConfig(null);
            } else {
                this.aeItemHandler.getInventory()[index].setConfig(null);
            }
            var itemSlot = this.aeItemHandler.getInventory()[index];
            var fluidSlot = this.aeFluidHandler.getInventory()[index];
            var slot = isItem ? itemSlot : fluidSlot;
            if (isItem) {
                fluidSlot.setConfig(null);
                fluidSlot.setStock(null);
            } else {
                itemSlot.setConfig(null);
                itemSlot.setStock(null);
            }
            slot.setConfig(new GenericStack(what, 1));
            slot.setStock(new GenericStack(what, request));
            index++;
        }
        aeItemHandler.clearInventory(index);
        aeFluidHandler.clearInventory(index);
    }

    protected void syncME() {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null) {
            return;
        }
        MEStorage networkInv = grid.getStorageService().getInventory();
        ExportOnlyAEItemSlot[] aeItem = aeItemHandler.getInventory();
        ExportOnlyAEFluidSlot[] aeFluid = aeFluidHandler.getInventory();
        ExportOnlyAESlot slot;
        for (int i = 0; i < aeItem.length; i++) {
            slot = aeItem[i];
            var config = slot.getConfig();
            if (config == null) {
                slot = aeFluid[i];
                config = slot.getConfig();
            }
            if (config != null) {
                var key = config.what();
                long extracted = networkInv.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
                if (extracted > 0) {
                    slot.setStock(new GenericStack(key, extracted));
                    continue;
                }
            }
            slot.setStock(null);
        }
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(176, 180, this, entityPlayer)
                .widget(new FancyMachineUIWidget(this, 176, 185));
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(new Position(0, 0));
        // ME Network status
        group.addWidget(new LabelWidget(3, 0, () -> this.isOnline ?
                "gtceu.gui.me_network.online" :
                "gtceu.gui.me_network.offline"));

        // Config slots
        group.addWidget(new AEDualConfigWidget(3, 10, this.aeItemHandler, this.aeFluidHandler, this, page));

        return group;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new TurnsConfiguratorButton(
                this::getAutoPullMode,
                (clickData, mode) -> setAutoPullMode(mode),
                GuiTextures.BUTTON_AUTO_PULL.getSubTexture(0, 0, 1, 0.5),
                AUTO_PULL_ALL_ICON,
                AUTO_PULL_ITEM_ICON,
                AUTO_PULL_FLUID_ICON).setTooltipsSupplier(mode -> List.of(Component.translatable("gtlcore.machine.me_dual_hatch_stock.turns." + mode))));
    }

    protected void setAutoPullMode(int autoPullMode) {
        this.autoPullMode = autoPullMode;
        if (!isRemote()) {
            if (this.autoPullMode == 0) {
                this.aeItemHandler.clearInventory(0);
                this.aeFluidHandler.clearInventory(0);
            } else if (updateMEStatus()) {
                this.refreshList();
                updateInventorySubscription();
            }
        }
    }

    protected CompoundTag writeConfigToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("AutoPullMode", autoPullMode);
        CompoundTag configStacks = new CompoundTag();
        if (autoPullMode == 0) {
            tag.put("ConfigStacks", configStacks);
            for (int i = 0; i < CONFIG_SIZE; i++) {
                var slot = this.aeItemHandler.getInventory()[i];
                GenericStack config = slot.getConfig();
                if (config == null) {
                    config = this.aeFluidHandler.getInventory()[i].getConfig();
                    if (config == null) {
                        continue;
                    }
                }
                CompoundTag stackTag = GenericStack.writeTag(config);
                configStacks.put(Integer.toString(i), stackTag);
            }
        }
        tag.putByte("GhostCircuit",
                (byte) IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0)));
        return tag;
    }

    protected void readConfigFromTag(CompoundTag tag) {
        if (tag.contains("AutoPullMode")) {
            var autoPullMode = tag.getInt("AutoPullMode");
            this.setAutoPullMode(autoPullMode);
        }

        if (tag.contains("ConfigStacks")) {
            CompoundTag configStacks = tag.getCompound("ConfigStacks");
            for (int i = 0; i < CONFIG_SIZE; i++) {
                String key = Integer.toString(i);
                if (configStacks.contains(key)) {
                    CompoundTag configTag = configStacks.getCompound(key);
                    var stack = GenericStack.readTag(configTag);
                    if (stack != null) {
                        if (stack.what() instanceof AEItemKey) {
                            this.aeItemHandler.getInventory()[i].setConfig(stack);
                        } else {
                            this.aeFluidHandler.getInventory()[i].setConfig(stack);
                        }
                        continue;
                    }
                }
                this.aeItemHandler.getInventory()[i].setConfig(null);
                this.aeFluidHandler.getInventory()[i].setConfig(null);
            }
        }

        if (tag.contains("GhostCircuit")) {
            circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit")));
        }
    }

    @Override
    public InteractionResult onDataStickRightClick(Player player, ItemStack dataStick) {
        CompoundTag tag = dataStick.getTag();
        if (tag == null || !tag.contains("MEDualHatchStock")) {
            return InteractionResult.PASS;
        }

        if (!isRemote()) {
            readConfigFromTag(tag.getCompound("MEDualHatchStock"));
            this.updateInventorySubscription();
            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"));
        }
        return InteractionResult.sidedSuccess(isRemote());
    }

    @Override
    public boolean onDataStickLeftClick(Player player, ItemStack dataStick) {
        if (!isRemote()) {
            CompoundTag tag = new CompoundTag();
            tag.put("MEDualHatchStock", writeConfigToTag());
            dataStick.setTag(tag);
            dataStick.setHoverName(Component.translatable("gtceu.machine.me.me_dual_hatch_stock.data_stick.name"));
            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"));
        }
        return true;
    }

    private class ExportOnlyAEStockingItemList extends ExportOnlyAEItemList implements IMEPartMachine {

        public ExportOnlyAEStockingItemList(MetaMachine holder, int slots) {
            super(holder, slots, ExportOnlyAEStockingItemSlot::new);
        }

        @Override
        public boolean isAutoPull() {
            return autoPullMode > 0;
        }

        @Override
        public boolean isStocking() {
            return true;
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
                    setChanged(true);
                    this.onContentsChanged();
                }
            }
            return left.isEmpty() ? null : left;
        }

        @Override
        public @Nullable Object2LongMap<ItemStack> getMEItemMap() {
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

    private class ExportOnlyAEStockingItemSlot extends ExportOnlyAEItemSlot implements LongAEStockingSlot {

        public ExportOnlyAEStockingItemSlot() {
            super();
        }

        public ExportOnlyAEStockingItemSlot(@Nullable GenericStack config, @Nullable GenericStack stock) {
            super(config, stock);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate, boolean notifyChanges) {
            if (slot == 0 && this.stock != null) {
                if (this.config != null) {
                    // Extract the items from the real net to either validate (simulate)
                    // or extract (modulate) when this is called
                    if (!isOnline()) return ItemStack.EMPTY;
                    MEStorage aeNetwork = getMainNode().getGrid().getStorageService().getInventory();
                    Actionable action = simulate ? Actionable.SIMULATE : Actionable.MODULATE;
                    var key = config.what();
                    long extracted = aeNetwork.extract(key, amount, action, actionSource);
                    if (extracted > 0) {
                        ItemStack resultStack = key instanceof AEItemKey itemKey ? itemKey.toStack((int) extracted) : ItemStack.EMPTY;
                        if (!simulate) {
                            // may as well update the display here
                            this.stock = ExportOnlyAESlot.copy(stock, stock.amount() - extracted);
                            if (this.stock.amount() == 0) {
                                this.stock = null;
                            }
                            if (notifyChanges && this.onContentsChanged != null) {
                                this.onContentsChanged.run();
                            }
                        }
                        return resultStack;
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ExportOnlyAEStockingItemSlot copy() {
            return new ExportOnlyAEStockingItemSlot(this.config == null ? null : copy(this.config), this.stock == null ? null : copy(this.stock));
        }

        @Override
        public long extractLong(int slot, long amount, boolean simulate, boolean notifyChanges) {
            if (slot == 0 && stock != null && config != null) {
                if (!isOnline()) return 0;

                MEStorage aeNetwork = Objects.requireNonNull(getMainNode().getGrid()).getStorageService().getInventory();
                AEKey key = config.what();
                if (key instanceof AEItemKey) {
                    long extracted = aeNetwork.extract(key, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
                    if (extracted > 0L) {
                        if (!simulate) {
                            this.stock = ExportOnlyAESlot.copy(stock, stock.amount() - extracted);
                            if (this.stock.amount() == 0) {
                                this.stock = null;
                            }

                            if (notifyChanges && this.onContentsChanged != null) {
                                this.onContentsChanged.run();
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
            if (this.stock != null && this.stock.amount() > 0L) {
                return this.stock.what() instanceof AEItemKey itemKey ? Pair.of(itemKey.toStack(), this.stock.amount()) : null;
            }
            return null;
        }
    }

    private class ExportOnlyAEStockingFluidList extends ExportOnlyAEFluidList {

        public ExportOnlyAEStockingFluidList(MetaMachine holder, int slots) {
            super(holder, slots, ExportOnlyAEStockingFluidSlot::new);
        }

        @Override
        public boolean isAutoPull() {
            return autoPullMode > 0;
        }

        @Override
        public boolean isStocking() {
            return true;
        }
    }

    private class ExportOnlyAEStockingFluidSlot extends ExportOnlyAEFluidSlot {

        public ExportOnlyAEStockingFluidSlot() {
            super();
        }

        public ExportOnlyAEStockingFluidSlot(@Nullable GenericStack config, @Nullable GenericStack stock) {
            super(config, stock);
        }

        @Override
        public ExportOnlyAEFluidSlot copy() {
            return new ExportOnlyAEStockingFluidSlot(this.config == null ? null : copy(this.config), this.stock == null ? null : copy(this.stock));
        }

        @Override
        public FluidStack drain(long maxDrain, boolean simulate, boolean notifyChanges) {
            if (this.stock != null && this.config != null) {
                // Extract the items from the real net to either validate (simulate)
                // or extract (modulate) when this is called
                if (!isOnline()) return FluidStack.empty();
                MEStorage aeNetwork = getMainNode().getGrid().getStorageService().getInventory();
                Actionable action = simulate ? Actionable.SIMULATE : Actionable.MODULATE;
                var key = config.what();
                long extracted = aeNetwork.extract(key, maxDrain, action, actionSource);
                if (extracted > 0) {
                    FluidStack resultStack = key instanceof AEFluidKey fluidKey ? AEUtil.toFluidStack(fluidKey, extracted) : FluidStack.empty();
                    if (!simulate) {
                        // may as well update the display here
                        this.stock = ExportOnlyAESlot.copy(stock, stock.amount() - extracted);
                        if (this.stock.amount() == 0) {
                            this.stock = null;
                        }
                        if (notifyChanges && this.onContentsChanged != null) {
                            this.onContentsChanged.run();
                        }
                    }
                    return resultStack;
                }
            }
            return FluidStack.empty();
        }
    }
}
