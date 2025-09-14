package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.AECraft.IMECraftIOPart;
import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.machine.multiblock.part.PaginationUIManager;
import org.gtlcore.gtlcore.integration.ae2.AEUtils;
import org.gtlcore.gtlcore.integration.lowdragmc.misc.MutableItemTransferList;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.helpers.patternprovider.PatternContainer;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static org.gtlcore.gtlcore.integration.ae2.AEUtils.*;

public class MEMolecularAssemblerIOPartMachine extends MEIOPartMachine implements PatternContainer, IMECraftIOPart {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MEMolecularAssemblerIOPartMachine.class, MEIOPartMachine.MANAGED_FIELD_HOLDER);

    private static final int ROWS_PER_PAGE = 8;
    private static final int PATTERNS_PER_ROW = 9;

    private final InternalInventory internalPatternInventory = new InternalInventory() {

        @Override
        public int size() {
            return mutableItemTransferList.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return mutableItemTransferList.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            mutableItemTransferList.setStackInSlot(slotIndex, stack);
            mutableItemTransferList.onContentsChanged();
            onPatternChange(slotIndex);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot <= mutableItemTransferList.getSlots() && AEUtils.molecularFilter(stack, getLevel());
        }
    };

    // ========================================
    // Status
    // ========================================

    @DescSynced
    @Persisted
    @Setter
    protected String customName = "";

    private boolean needPatternSync;

    private boolean shouldOpen = false;

    // ========================================
    // Handlers
    // ========================================

    protected final NotifiableMAHandlerTrait maHandler;

    @DescSynced
    private final MutableItemTransferList mutableItemTransferList;

    protected PaginationUIManager paginationUIManager;

    // ========================================
    // Inventory
    // ========================================

    private final Int2ReferenceMap<@NotNull IPatternDetails> patternSlotMap;

    @Getter
    private final Object2LongLinkedOpenHashMap<GenericStack> outputItems;  // must 1 count

    @Getter
    private final Object2LongOpenHashMap<AEItemKey> buffer;

    public MEMolecularAssemblerIOPartMachine(IMachineBlockEntity holder) {
        super(holder, IO.BOTH);
        getMainNode().addService(IGridTickable.class, new Ticker()).addService(ICraftingProvider.class, this);

        patternSlotMap = new Int2ReferenceOpenHashMap<>();
        outputItems = new Object2LongLinkedOpenHashMap<>();
        buffer = new Object2LongOpenHashMap<>();
        maHandler = new MECraftHandler(this);

        mutableItemTransferList = new MutableItemTransferList();
    }

    @Override
    public boolean pushPattern(IPatternDetails details, long multiply) {
        if (!getMainNode().isActive() || !(details instanceof IMolecularAssemblerSupportedPattern molecularAssemblerSupportedPattern)) {
            return false;
        }

        final GenericStack output = molecularAssemblerSupportedPattern.getOutputs()[0];
        if (!(output.what() instanceof AEItemKey)) return false;
        outputItems.addTo(output, multiply);
        maHandler.notifyListeners();

        return true;
    }

    @Override
    public void init(@NotNull List<IItemTransfer> transfers) {
        mutableItemTransferList.clear();
        patternSlotMap.clear();
        if (!transfers.isEmpty()) {
            mutableItemTransferList.addTransfers(transfers);
            for (int i = 0; i < mutableItemTransferList.getSlots(); i++) {
                var pattern = getPatternDetails(mutableItemTransferList.getStackInSlot(i));
                if (pattern != null) patternSlotMap.put(i, pattern);
            }
            shouldOpen = true;
        } else {
            shouldOpen = false;
        }
        needPatternSync = true;
    }

    @Override
    public @NotNull NotifiableMAHandlerTrait getNotifiableMAHandlerTrait() {
        return this.maHandler;
    }

    @Override
    public boolean canShared() {
        return false;
    }

    protected void clear() {
        mutableItemTransferList.clear();
        patternSlotMap.clear();
        shouldOpen = false;
    }

    private @Nullable IPatternDetails getPatternDetails(ItemStack stack) {
        if (!stack.isEmpty()) {
            if (stack.getItem() instanceof EncodedPatternItem encodedPatternItem) {
                return encodedPatternItem.decode(stack, getLevel(), false);
            }
        }
        return null;
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ========================================
    // LIFECYCLE & NETWORK MANAGEMENT
    // ========================================

    @Nullable
    protected TickableSubscription updateSubs;

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.@NotNull State reason) {
        super.onMainNodeStateChanged(reason);
        this.updateSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        clear();
    }

    @Override
    public void removedFromController(@NotNull IMultiController controller) {
        super.removedFromController(controller);
        clear();
        needPatternSync = true;
    }

    protected void updateSubscription() {
        if (getMainNode().isOnline()) {
            updateSubs = subscribeServerTick(updateSubs, this::update);
        } else if (updateSubs != null) {
            updateSubs.unsubscribe();
            updateSubs = null;
        }
    }

    protected void update() {
        if (needPatternSync) {
            ICraftingProvider.requestUpdate(getMainNode());
            this.needPatternSync = false;
        }
    }

    protected void onPatternChange(int index) {
        if (isRemote()) return;

        var newPattern = mutableItemTransferList.getStackInSlot(index);
        var newPatternDetails = getPatternDetails(newPattern);

        if (newPatternDetails != null) patternSlotMap.put(index, newPatternDetails);
        else patternSlotMap.remove(index);

        needPatternSync = true;
    }

    // ========================================
    // GUI
    // ========================================

    @Override
    public @NotNull Widget createUIWidget() {
        final int totalCount = mutableItemTransferList.getSlots();
        final int colSize = 9;
        final int uiWidth = Math.max(PATTERNS_PER_ROW * 18 + 16, 106);
        final int uiHeight = ROWS_PER_PAGE * 18 + 28;

        this.paginationUIManager = new PaginationUIManager(9, ROWS_PER_PAGE, totalCount,
                uiWidth, uiHeight,
                this::onPatternChange,
                mutableItemTransferList);

        var group = new WidgetGroup(0, 0, paginationUIManager.getUiWidth(), paginationUIManager.getUiHeight());

        // ME Network status indicator
        group.addWidget(new LabelWidget(8, 2,
                () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));

        // Custom name input widget
        group.addWidget(new AETextInputButtonWidget(18 * colSize + 8 - 70, 2, 70, 10)
                .setText(customName)
                .setOnConfirm(this::setCustomName)
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc")));

        group.addWidget(paginationUIManager.createPaginationUI(null));

        return group;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return shouldOpen;
    }

    // ========================================
    // Persist
    // ========================================

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (buffer.isEmpty() && outputItems.isEmpty()) return;
        ListTag bufferTag = AEUtils.createListTag(AEItemKey::toTag, buffer);
        if (!bufferTag.isEmpty()) tag.put("buffer", bufferTag);

        ListTag outputTag = AEUtils.createListTag(GenericStack::writeTag, outputItems);
        if (!outputTag.isEmpty()) tag.put("outputItems", outputTag);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        buffer.clear();
        ListTag bufferTag = tag.getList("buffer", Tag.TAG_COMPOUND);
        AEUtils.loadInventory(bufferTag, AEItemKey::fromTag, buffer);

        ListTag outputTag = tag.getList("outputItems", Tag.TAG_COMPOUND);
        AEUtils.loadInventory(outputTag, GenericStack::readTag, outputItems);
    }

    // ========================================
    // Pattern
    // ========================================

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return new ObjectArrayList<>(patternSlotMap.values());
    }

    @Override
    public boolean pushPattern(IPatternDetails iPatternDetails, KeyCounter[] keyCounters) {
        // Only Use pushPattern(IPatternDetails details, long multiply)
        return false;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public @Nullable IGrid getGrid() {
        return getMainNode().getGrid();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return internalPatternInventory;
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        List<IMultiController> controllers = getControllers();

        // Handle multiblock controller grouping
        if (!controllers.isEmpty()) {
            IMultiController controller = controllers.get(0);
            MultiblockMachineDefinition controllerDefinition = controller.self().getDefinition();

            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()),
                        Component.literal(customName),
                        Collections.emptyList());
            } else {
                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()), Component.translatable(controllerDefinition.getDescriptionId()), Collections.emptyList());
            }
        } else {
            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(GTLMachines.GTAEMachines.ME_EXTEND_PATTERN_BUFFER.getItem()),
                        Component.literal(customName),
                        Collections.emptyList());
            } else {
                return new PatternContainerGroup(
                        AEItemKey.of(GTLMachines.GTAEMachines.ME_EXTEND_PATTERN_BUFFER.getItem()),
                        GTLMachines.GTAEMachines.ME_EXTEND_PATTERN_BUFFER.get().getDefinition().getItem().getDescription(),
                        Collections.emptyList());
            }
        }
    }

    protected class Ticker implements IGridTickable {

        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(MEExtendedOutputPartMachineBase.MIN_FREQUENCY, MEExtendedOutputPartMachineBase.MAX_FREQUENCY, false, true);
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            if (!getMainNode().isActive()) {
                return TickRateModulation.SLEEP;
            }

            if (buffer.isEmpty()) {
                if (ticksSinceLastCall >= MEExtendedOutputPartMachineBase.MAX_FREQUENCY) {
                    isSleeping = true;
                    return TickRateModulation.SLEEP;
                } else return TickRateModulation.SLOWER;
            } else return reFunds(buffer, getMainNode().getGrid(), actionSource) ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
    }
}
