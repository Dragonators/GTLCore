package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.gui.MEPatternCatalystUIManager;
import org.gtlcore.gtlcore.api.machine.trait.*;
import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.config.ConfigHolder;
import org.gtlcore.gtlcore.integration.ae2.widget.AEPatternViewExtendSlotWidget;
import org.gtlcore.gtlcore.utils.GTLUtil;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.ButtonConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyInvConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyTankConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.misc.FluidTransferList;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
import appeng.api.stacks.*;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.crafting.pattern.ProcessingPatternItem;
import appeng.helpers.patternprovider.PatternContainer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystItemStackHandler;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.gtlcore.gtlcore.common.machine.multiblock.part.ae.AEUtils.createListTag;
import static org.gtlcore.gtlcore.common.machine.multiblock.part.ae.AEUtils.loadInventory;

public class MEPatternBufferPartMachine extends MEIOPartMachine implements IInteractedMachine, ICraftingProvider, PatternContainer, IMEPatternPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEPatternBufferPartMachine.class, MEIOPartMachine.MANAGED_FIELD_HOLDER);

    protected final int maxPatternCount;

    private final boolean[] hasPatternArray;
    private final long[] lastNotifyTickBySlot;
    private final ItemStack[] lastSnapshotBySlot;
    @DescSynced
    private final boolean[] cacheRecipe;

    private final InternalInventory internalPatternInventory = new InternalInventory() {

        @Override
        public int size() {
            return maxPatternCount;
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return patternInventory.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            patternInventory.setStackInSlot(slotIndex, stack);
            patternInventory.onContentsChanged(slotIndex);
            onPatternChange(slotIndex);
        }
    };

    @Getter
    @Persisted
    @DescSynced
    private final ItemStackTransfer patternInventory;

    @Getter
    @Persisted
    protected final CatalystItemStackHandler shareInventory;

    @Getter
    @Persisted
    protected final CatalystFluidStackHandler shareTank;

    @Getter
    @Persisted
    protected final NotifiableItemStackHandler mePatternCircuitInventory;

    @Persisted
    private final ItemStackTransfer[] catalystItems;

    @Persisted
    @LazyManaged
    private final FluidTransferList[] catalystFluids;

    /** Pattern circuit handler for managing circuit logic */
    @Getter
    protected final PatternCircuitHandler circuitHandler;

    @Getter
    @Persisted
    protected final InternalSlot[] internalInventory;

    protected final Int2ObjectMap<GTRecipe> gtRecipeCacheMap = new Int2ObjectArrayMap<>();

    private final BiMap<IPatternDetails, Integer> patternSlotMap;

    private boolean needPatternSync;

    @Persisted
    private final ObjectOpenHashSet<BlockPos> proxies = new ObjectOpenHashSet<>();

    @DescSynced
    @Persisted
    @Setter
    private String customName = "";

    @Persisted
    @Getter
    private final PendingRefundData pendingRefundData;

    /** Recipe handler trait for ME Pattern Buffer */
    protected final MEPatternBufferRecipeHandlerTrait recipeHandler;

    public MEPatternBufferPartMachine(IMachineBlockEntity holder, int maxPatternCount, IO io) {
        super(holder, io);

        // Initialize UI configuration
        this.maxPatternCount = maxPatternCount;

        // Initialize arrays with calculated size
        this.hasPatternArray = new boolean[maxPatternCount];
        this.cacheRecipe = new boolean[maxPatternCount];
        this.internalInventory = new InternalSlot[maxPatternCount];
        this.patternSlotMap = HashBiMap.create(maxPatternCount);
        this.lastNotifyTickBySlot = new long[maxPatternCount];
        this.lastSnapshotBySlot = new ItemStack[maxPatternCount];
        this.catalystItems = new ItemStackTransfer[maxPatternCount];
        this.catalystFluids = new FluidTransferList[maxPatternCount];

        // Initialize inventories
        this.patternInventory = new ItemStackTransfer(maxPatternCount);
        this.patternInventory.setFilter(stack -> stack.getItem() instanceof ProcessingPatternItem);
        Arrays.fill(lastNotifyTickBySlot, Long.MIN_VALUE);
        Arrays.setAll(internalInventory, InternalSlot::new);
        Arrays.setAll(catalystItems, i -> {
            var transfer = new ItemStackTransfer(9);
            transfer.setFilter(stack -> !(stack.getItem() instanceof ProcessingPatternItem));
            return transfer;
        });
        Arrays.setAll(catalystFluids, i -> new FluidTransferList(Stream.generate(() -> (IFluidTransfer) new FluidStorage(16 * FluidHelper.getBucket()))
                .limit(9)
                .toList()));
        getMainNode().addService(ICraftingProvider.class, this).addService(IGridTickable.class, new Ticker());

        this.pendingRefundData = new PendingRefundData();
        this.mePatternCircuitInventory = new NotifiableCircuitItemStackHandler(this);
        this.circuitHandler = new PatternCircuitHandler((NotifiableCircuitItemStackHandler) mePatternCircuitInventory);
        this.shareInventory = new CatalystItemStackHandler(this, 9, IO.IN, IO.NONE);
        this.shareTank = new CatalystFluidStackHandler(this, 9, 16 * FluidHelper.getBucket(), IO.IN, IO.NONE);
        this.recipeHandler = new MEPatternBufferRecipeHandlerTrait(this, pendingRefundData, io);
    }

    // ========================================
    // LIFECYCLE & NETWORK MANAGEMENT
    // ========================================

    @Nullable
    protected TickableSubscription updateSubs;

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(1, () -> {
                for (int i = 0; i < patternInventory.getSlots(); i++) {
                    var pattern = patternInventory.getStackInSlot(i);
                    var realPattern = getRealPattern(i, pattern);
                    if (realPattern != null) {
                        this.patternSlotMap.forcePut(realPattern, i);
                        hasPatternArray[i] = true;
                    }
                }
                needPatternSync = true;
            }));
        }
        this.getMERecipeHandlerTraits().forEach(handler -> handler.addChangedListener(() -> getProxies().forEach(proxy -> {
            if (handler.getCapability() == ItemRecipeCapability.CAP) {
                proxy.itemProxyHandler.notifyListeners();
            } else {
                proxy.fluidProxyHandler.notifyListeners();
            }
        })));
        for (int i = 0; i < maxPatternCount; i++) {
            final int index = i;
            catalystItems[index].setOnContentsChanged(() -> reCalculateCatalystItemMap(index));
            for (IFluidTransfer transfer : catalystFluids[index].transfers) {
                if (transfer instanceof FluidStorage storage) {
                    storage.setOnContentsChanged(() -> reCalculateCatalystFluidMap(index));
                }
            }
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.@NotNull State reason) {
        super.onMainNodeStateChanged(reason);
        this.updateSubscription();
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

    private void onPatternChange(int index) {
        if (isRemote()) return;

        var internalInv = internalInventory[index];
        var newPattern = patternInventory.getStackInSlot(index);
        var newPatternDetailsWithOutCircuit = getRealPattern(index, newPattern);
        var oldPatternDetails = patternSlotMap.inverse().get(index);

        // Update pattern mapping and tracking
        if (newPatternDetailsWithOutCircuit != null) {
            patternSlotMap.forcePut(newPatternDetailsWithOutCircuit, index);
            hasPatternArray[index] = true;
        } else {
            patternSlotMap.inverse().remove(index);
            hasPatternArray[index] = false;
        }

        // remove old pattern cache
        if (oldPatternDetails != null && !oldPatternDetails.equals(newPatternDetailsWithOutCircuit)) {
            // 样板更换时清理缓存的电路和原始样板
            internalInv.cacheManager.clearAllCaches();
            cacheRecipe[index] = false;
            gtRecipeCacheMap.remove(index);
            refundSlot(internalInv);
            pendingRefundData.reFunds();
        }

        needPatternSync = true;
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(patternInventory);
        clearInventory(shareInventory);
        for (ItemStackTransfer catalystItem : catalystItems) {
            clearInventory(catalystItem);
        }
    }

    private void reCalculateCatalystItemMap(int slot) {
        final var itemCatalystInventory = internalInventory[slot].itemCatalystInventory;
        itemCatalystInventory.clear();
        var catalystItem = catalystItems[slot];
        for (int i = 0; i < catalystItem.getSlots(); i++) {
            ItemStack stack = catalystItem.getStackInSlot(i);
            if (!stack.isEmpty()) {
                itemCatalystInventory.mergeLong(AEItemKey.of(stack), stack.getCount(), Long::sum);
            }
        }
        internalInventory[slot].onContentsChanged.run();
    }

    private void reCalculateCatalystFluidMap(int slot) {
        final var fluidCatalystInventory = internalInventory[slot].fluidCatalystInventory;
        fluidCatalystInventory.clear();
        var catalystFluid = catalystFluids[slot];
        for (int i = 0; i < catalystFluid.getTanks(); i++) {
            FluidStack stack = catalystFluid.getFluidInTank(i);
            if (!stack.isEmpty()) {
                fluidCatalystInventory.mergeLong(AEFluidKey.of(stack.getFluid()), stack.getAmount(), Long::sum);
            }
        }
        internalInventory[slot].onContentsChanged.run();
    }

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ========================================
    // PROXY MANAGEMENT SYSTEM
    // ========================================

    public void addProxy(MEPatternBufferProxyPartMachine proxy) {
        proxies.add(proxy.getPos());
    }

    public void removeProxy(MEPatternBufferProxyPartMachine proxy) {
        proxies.remove(proxy.getPos());
    }

    public Set<MEPatternBufferProxyPartMachine> getProxies() {
        var activatedProxies = new ObjectOpenHashSet<MEPatternBufferProxyPartMachine>();
        for (var pos : proxies) {
            if (MetaMachine.getMachine(Objects.requireNonNull(getLevel()), pos) instanceof MEPatternBufferProxyPartMachine proxy) {
                activatedProxies.add(proxy);
            }
        }
        return activatedProxies;
    }

    // ========================================
    // REFUND SYSTEM
    // ========================================

    private void refundAll(ClickData clickData) {
        if (!clickData.isRemote) {
            // Move all slot contents to pending refund
            Arrays.stream(internalInventory)
                    .filter(InternalSlot::isActive)
                    .forEach(this::refundSlot);

            // Immediately try to process pending refunds
            pendingRefundData.reFunds();
        }
    }

    public void refundSlot(InternalSlot slot) {
        // Move all item contents to pending refund
        for (var it = slot.itemInventory.object2LongEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            long amount = entry.getLongValue();
            if (amount > 0) {
                pendingRefundData.addTo(entry.getKey(), amount);
                it.remove();
            }
        }

        // Move all fluid contents to pending refund
        for (var it = slot.fluidInventory.object2LongEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            long amount = entry.getLongValue();
            if (amount > 0) {
                pendingRefundData.addTo(entry.getKey(), amount);
                it.remove();
            }
        }
    }

    // ========================================
    // DATASTICK INTERACTION
    // ========================================

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;

        if (stack.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                Pair<GTRecipeType, String> researchData = ResearchManager.readResearchId(stack);
                if (researchData != null) {
                    return InteractionResult.PASS;
                }

                // Store this pattern buffer's position in the data stick
                stack.getOrCreateTag().putIntArray("pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
                player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"));
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    // ========================================
    // GUI SYSTEM
    // ========================================

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        // Refund all button
        configuratorPanel.attachConfigurators(new ButtonConfigurator(
                new GuiTextureGroup(GuiTextures.BUTTON, GuiTextures.REFUND_OVERLAY), this::refundAll)
                .setTooltips(List.of(Component.translatable("gui.gtceu.refund_all.desc"))));

        // Circuit configurator
        configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(mePatternCircuitInventory.storage));

        // Share inventory configurator
        configuratorPanel.attachConfigurators(new FancyInvConfigurator(
                shareInventory.storage, Component.translatable("gui.gtceu.share_inventory.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_inventory.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));

        // Share tank configurator
        configuratorPanel.attachConfigurators(new FancyTankConfigurator(
                shareTank.getStorages(), Component.translatable("gui.gtceu.share_tank.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_tank.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));
    }

    @Override
    public @NotNull Widget createUIWidget() {
        int rowSize = 9;
        int colSize = maxPatternCount / rowSize;
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);

        // ME Network status indicator
        group.addWidget(new LabelWidget(8, 2,
                () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));

        // Custom name input widget
        group.addWidget(new AETextInputButtonWidget(18 * rowSize + 8 - 70, 2, 70, 10)
                .setText(customName)
                .setOnConfirm(this::setCustomName)
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc")));

        final var catalystUIManager = new MEPatternCatalystUIManager(group.getSizeWidth() + 4, catalystItems, catalystFluids);
        group.waitToAdded(catalystUIManager);

        int index = 0;
        for (int y = 0; y < colSize; ++y) {
            for (int x = 0; x < rowSize; ++x) {
                int finalI = index;

                var slot = new AEPatternViewExtendSlotWidget(patternInventory, index++, x * 18 + 8, y * 18 + 14)
                        .setOnMiddleClick(() -> catalystUIManager.toggleFor(finalI))
                        .setOccupiedTexture(GuiTextures.SLOT)
                        .setItemHook(stack -> {
                            if (!stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem iep) {
                                final ItemStack out = iep.getOutput(stack);
                                if (!out.isEmpty()) return out;
                            }
                            return stack;
                        })
                        .setChangeListener(debounceAndFilter(finalI, () -> this.onPatternChange(finalI)))
                        .setOnAddedTooltips((s, l) -> {
                            if (cacheRecipe[finalI])
                                l.add(Component.translatable("gtceu.machine.pattern.recipe.cache"));
                        })
                        .setBackground(GuiTextures.SLOT, GuiTextures.PATTERN_OVERLAY);
                group.addWidget(slot);
            }
        }
        return group;
    }

    // ========================================
    // PERFORMANCE OPTIMIZATION UTILITIES
    // ========================================

    private Runnable debounceAndFilter(int slotIndex, Runnable delegate) {
        return () -> {
            long now = getGameTick();
            if (lastNotifyTickBySlot[slotIndex] == now) {
                return;
            }

            ItemStack cur = this.patternInventory.getStackInSlot(slotIndex);
            ItemStack prev = lastSnapshotBySlot[slotIndex];
            if (sameStack(prev, cur)) {
                lastNotifyTickBySlot[slotIndex] = now;
                return;
            }

            lastNotifyTickBySlot[slotIndex] = now;
            lastSnapshotBySlot[slotIndex] = cur.isEmpty() ? ItemStack.EMPTY : cur.copy();

            delegate.run();
        };
    }

    private static boolean sameStack(@Nullable ItemStack a, @Nullable ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() ^ b.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(a, b) && a.getCount() == b.getCount();
    }

    private long getGameTick() {
        var level = getLevel();
        return level != null ? level.getGameTime() : System.nanoTime();
    }

    // ========================================
    // CIRCUIT HANDLING
    // ========================================

    private IPatternDetails getRealPattern(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            var internalSlot = internalInventory[slot];
            return circuitHandler.processPatternWithCircuit(
                    stack, internalSlot.cacheManager::setCircuitCache, getLevel());
        }
        return null;
    }

    /**
     * 获取用于配方的电路
     *
     * @param slotIndex 槽位索引
     * @return 电路ItemStack，可能为空
     */
    public ItemStack getCircuitForRecipe(int slotIndex) {
        return circuitHandler.getCircuitForRecipe(internalInventory[slotIndex].cacheManager.getCircuitStack());
    }

    // ========================================
    // AE2 CRAFTING
    // ========================================

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patternSlotMap.keySet().stream().filter(Objects::nonNull).toList();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !patternSlotMap.containsKey(patternDetails) || !checkInput(inputHolder)) {
            return false;
        }

        var slotIndex = patternSlotMap.get(patternDetails);
        if (slotIndex != null && slotIndex >= 0) {
            internalInventory[slotIndex].pushPattern(patternDetails, inputHolder);
            recipeHandler.onChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private boolean checkInput(KeyCounter[] inputHolder) {
        for (KeyCounter input : inputHolder) {
            var illegal = input.keySet().stream()
                    .map(AEKey::getType)
                    .map(AEKeyType::getId)
                    .anyMatch(id -> !id.equals(AEKeyType.items().getId()) && !id.equals(AEKeyType.fluids().getId()));
            if (illegal) return false;
        }
        return true;
    }

    // ========================================
    // PATTERN CONTAINER IMPLEMENTATION
    // ========================================

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
                ItemStack circuitStack = mePatternCircuitInventory.storage.getStackInSlot(0);
                int circuitConfiguration = circuitStack.isEmpty() ? -1 :
                        IntCircuitBehaviour.getCircuitConfiguration(circuitStack);

                Component groupName = circuitConfiguration != -1 ?
                        Component.translatable(controllerDefinition.getDescriptionId())
                                .append(" - " + circuitConfiguration) :
                        Component.translatable(controllerDefinition.getDescriptionId());

                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()), groupName, Collections.emptyList());
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

    // ========================================
    // IMEPatternPartMachine
    // ========================================

    @Override
    public @NotNull List<@NotNull GTRecipe> getCachedGTRecipe() {
        List<GTRecipe> recipes = new ObjectArrayList<>();
        for (var it = Int2ObjectMaps.fastIterator(gtRecipeCacheMap); it.hasNext();) {
            var entry = it.next();
            GTRecipe recipe = entry.getValue();
            int slot = entry.getIntKey();
            if (recipe == null) it.remove();
            else if (internalInventory[slot].isActive()) recipes.add(recipe);
        }
        return recipes;
    }

    @Override
    public void setSlotCacheRecipe(int index, GTRecipe recipe) {
        if (recipe != null) {
            gtRecipeCacheMap.put(index, recipe);
            cacheRecipe[index] = true;
        }
    }

    @Override
    public void restoreMachineCache(Map<GTRecipe, IRecipeHandlePart> map, MERecipeHandlePart mePart) {
        if (this.gtRecipeCacheMap.isEmpty()) return;
        for (var it = Int2ObjectMaps.fastIterator(gtRecipeCacheMap); it.hasNext();) {
            var entry = it.next();
            var r = entry.getValue();
            mePart.getSlotMap().forcePut(r, entry.getIntKey());
            map.put(r, mePart);
        }
    }

    @Override
    public boolean hasCacheInSlot(int slot) {
        return cacheRecipe[slot];
    }

    @Override
    public Iterable<IMERecipeHandlerTrait<?, ?>> getMERecipeHandlerTraits() {
        return recipeHandler.getMERecipeHandlers();
    }

    public Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<? extends Predicate<?>, ?>> getMERecipeHandlerMap() {
        return recipeHandler.getMERecipeHandlerMap();
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (!gtRecipeCacheMap.isEmpty()) {
            CompoundTag recipeCacheTag = new CompoundTag();
            for (var entry : Int2ObjectMaps.fastIterable(gtRecipeCacheMap)) {
                GTRecipe recipe = entry.getValue();
                if (recipe != null) {
                    recipeCacheTag.put(String.valueOf(entry.getIntKey()), GTLUtil.serializeNBT(recipe));
                }
            }
            tag.put("gtRecipeCache", recipeCacheTag);
        }
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (tag.contains("gtRecipeCache")) {
            gtRecipeCacheMap.clear();
            CompoundTag recipeCacheTag = tag.getCompound("gtRecipeCache");
            for (String key : recipeCacheTag.getAllKeys()) {
                int slotIndex = Integer.parseInt(key);
                Tag recipeTag = recipeCacheTag.get(key);
                GTRecipe recipe = GTLUtil.deserializeNBT(recipeTag);
                if (recipe != null && slotIndex >= 0 && slotIndex < maxPatternCount) {
                    gtRecipeCacheMap.put(slotIndex, recipe);
                    cacheRecipe[slotIndex] = true;
                }
            }
        }
    }

    /**
     * Internal Slot: Pattern-specific inventory management
     * Each slot represents a single pattern's ingredient storage with separate item and fluid inventories.
     * Provides optimized handling for recipe matching
     * and ingredient consumption with efficient serialization.
     * Features:
     * - Separate inventories per pattern
     * - Efficient recipe matching with early exit conditions
     */
    public class InternalSlot implements ITagSerializable<CompoundTag>, IContentChangeAware {

        @Getter
        @Setter
        protected Runnable onContentsChanged = () -> {
            recipeHandler.getMeFluidHandler().notifyListeners();
            recipeHandler.getMeItemHandler().notifyListeners();
        };

        @Getter
        private final Object2LongOpenHashMap<AEItemKey> itemInventory = new Object2LongOpenHashMap<>();

        @Getter
        private final Object2LongOpenHashMap<AEFluidKey> fluidInventory = new Object2LongOpenHashMap<>();

        private final Object2LongMap<AEItemKey> itemCatalystInventory = new Object2LongArrayMap<>();

        private final Object2LongMap<AEFluidKey> fluidCatalystInventory = new Object2LongArrayMap<>();

        @Persisted
        @Getter
        private final SlotCacheManager cacheManager = new SlotCacheManager();

        @Getter
        private final int slotIndex;

        public InternalSlot(int slotIndex) {
            this.slotIndex = slotIndex;
            itemInventory.defaultReturnValue(0);
            fluidInventory.defaultReturnValue(0);
            itemCatalystInventory.defaultReturnValue(0);
            fluidCatalystInventory.defaultReturnValue(0);
        }

        public boolean isActive() {
            return hasPatternArray[slotIndex] && (!itemInventory.isEmpty() || !fluidInventory.isEmpty() || !itemCatalystInventory.isEmpty() || !fluidCatalystInventory.isEmpty());
        }

        public boolean isActive(RecipeCapability<?> recipeCapability) {
            if (recipeCapability == ItemRecipeCapability.CAP) {
                return hasPatternArray[slotIndex] &&
                        (!itemInventory.isEmpty() || !shareInventory.isEmpty() || !circuitHandler.getCircuitForRecipe(cacheManager.getCircuitStack()).isEmpty() || !itemCatalystInventory.isEmpty());
            } else {
                return hasPatternArray[slotIndex] &&
                        (!fluidInventory.isEmpty() || !shareTank.isEmpty() || !fluidCatalystInventory.isEmpty());
            }
        }

        private void add(AEKey what, long amount) {
            if (amount <= 0L) return;
            if (what instanceof AEItemKey itemKey) {
                itemInventory.addTo(itemKey, amount);
            } else if (what instanceof AEFluidKey fluidKey) {
                fluidInventory.addTo(fluidKey, amount);
            }
        }

        public Object2LongMap<ItemStack> getItemStackInputMap() {
            var itemInputMap = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
            for (Object2LongMap.Entry<AEItemKey> entry : Object2LongMaps.fastIterable(itemInventory)) {
                AEItemKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount <= 0) continue;

                ItemStack stack = key.toStack(1);
                itemInputMap.addTo(stack, amount);
            }
            return itemInputMap;
        }

        public Object2LongMap<FluidStack> getFluidStackInputMap() {
            var fluidInputMap = new Object2LongOpenCustomHashMap<>(FluidStackHashStrategy.comparingAllButAmount());
            for (Object2LongMap.Entry<AEFluidKey> entry : Object2LongMaps.fastIterable(fluidInventory)) {
                AEFluidKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount <= 0) continue;

                FluidStack stack = FluidStack.create(key.getFluid(), 1);
                fluidInputMap.addTo(stack, amount);
            }
            return fluidInputMap;
        }

        public List<Object> getLimitItemStackInput() {
            var limitInput = new ObjectArrayList<>(itemInventory.size());
            for (var it = Object2LongMaps.fastIterator(itemInventory); it.hasNext();) {
                var entry = it.next();
                long amount = entry.getLongValue();
                if (amount <= 0) {
                    it.remove();
                    continue;
                }
                limitInput.add(entry.getKey().toStack(Ints.saturatedCast(amount)));
            }
            for (Object2LongMap.Entry<AEItemKey> entry : Object2LongMaps.fastIterable(itemCatalystInventory)) {
                limitInput.add(entry.getKey().toStack(Ints.saturatedCast(entry.getLongValue())));
            }
            return limitInput;
        }

        public List<Object> getLimitFluidStackInput() {
            var limitInput = new ObjectArrayList<>(fluidInventory.size());
            for (var it = Object2LongMaps.fastIterator(fluidInventory); it.hasNext();) {
                var entry = it.next();
                long amount = entry.getLongValue();
                if (amount <= 0) {
                    it.remove();
                    continue;
                }
                limitInput.add(FluidStack.create(entry.getKey().getFluid(), amount));
            }
            for (Object2LongMap.Entry<AEFluidKey> entry : Object2LongMaps.fastIterable(fluidCatalystInventory)) {
                limitInput.add(FluidStack.create(entry.getKey().getFluid(), entry.getLongValue()));
            }
            return limitInput;
        }

        public void pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
            patternDetails.pushInputsToExternalInventory(inputHolder, this::add);
            onContentsChanged.run();
        }

        public boolean testCatalystItemInternal(GTRecipe recipe) {
            for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
                if (content.chance <= 0) continue;
                var ingredient = (Ingredient) content.getContent();
                for (ItemStack item : ingredient.getItems()) {
                    AEItemKey key = AEItemKey.of(item);
                    if (itemCatalystInventory.containsKey(key)) return false;
                }
            }
            return true;
        }

        public boolean testCatalystFluidInternal(GTRecipe recipe) {
            for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
                if (content.chance <= 0) continue;
                var fluidIngredient = (FluidIngredient) content.getContent();
                for (FluidStack stack : fluidIngredient.getStacks()) {
                    AEFluidKey key = AEFluidKey.of(stack.getFluid());
                    if (fluidCatalystInventory.containsKey(key)) return false;
                }
            }
            return true;
        }

        public boolean handleItemInternal(Object2LongMap<Ingredient> left, int leftCircuit, boolean simulate) {
            if (left.isEmpty() && leftCircuit < 0) return true;

            if (simulate && leftCircuit > 0 && !(leftCircuit == cacheManager.getCircuitCache())) {
                return false;
            }

            for (var it = Object2LongMaps.fastIterator(left); it.hasNext();) {
                var entry = it.next();
                var ingredient = entry.getKey();
                long needAmount = entry.getLongValue();
                if (needAmount <= 0) {
                    it.remove();
                    continue;
                }

                AEItemKey bestMatch = simulate ? cacheManager.getBestItemMatchSimulate(ingredient, itemInventory, itemCatalystInventory, needAmount) :
                        cacheManager.getBestItemMatch(ingredient, itemInventory, needAmount);
                if (bestMatch == null) {
                    return false;
                }
            }

            if (!simulate) {
                for (var it = Object2LongMaps.fastIterator(left); it.hasNext();) {
                    var entry = it.next();
                    var ingredient = entry.getKey();
                    long needAmount = entry.getLongValue();

                    var bestMatch = cacheManager.getBestItemMatch(ingredient, itemInventory, needAmount);
                    if (bestMatch != null) {
                        long amount = itemInventory.getLong(bestMatch);
                        long except = amount - needAmount;
                        if (except <= 0) {
                            itemInventory.removeLong(bestMatch);
                        } else {
                            itemInventory.put(bestMatch, except);
                        }
                        it.remove();
                    }
                }
            }

            return true;
        }

        public boolean handleFluidInternal(Object2LongMap<FluidIngredient> left, boolean simulate) {
            if (left.isEmpty()) return true;

            for (var it = Object2LongMaps.fastIterator(left); it.hasNext();) {
                var entry = it.next();
                var ingredient = entry.getKey();
                long needAmount = entry.getLongValue();
                if (needAmount <= 0) {
                    it.remove();
                    continue;
                }

                AEFluidKey bestMatch = simulate ? cacheManager.getBestFluidMatchSimulate(ingredient, fluidInventory, fluidCatalystInventory, needAmount) :
                        cacheManager.getBestFluidMatch(ingredient, fluidInventory, needAmount);
                if (bestMatch == null) {
                    return false;
                }
            }

            if (!simulate) {
                for (var it = Object2LongMaps.fastIterator(left); it.hasNext();) {
                    var entry = it.next();
                    var ingredient = entry.getKey();
                    long needAmount = entry.getLongValue();

                    AEFluidKey bestMatch = cacheManager.getBestFluidMatch(ingredient, fluidInventory, needAmount);
                    if (bestMatch != null) {
                        long amount = fluidInventory.getLong(bestMatch);
                        long except = amount - needAmount;
                        if (except <= 0) {
                            fluidInventory.removeLong(bestMatch);
                        } else {
                            fluidInventory.put(bestMatch, except);
                        }
                        it.remove();
                    }
                }
            }

            return true;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();

            ListTag itemsTag = createListTag(AEItemKey::toTag, itemInventory);
            if (!itemsTag.isEmpty()) tag.put("inventory", itemsTag);

            ListTag itemCatalystTag = createListTag(AEItemKey::toTag, itemCatalystInventory);
            if (!itemCatalystTag.isEmpty()) tag.put("catalystInventory", itemCatalystTag);

            ListTag fluidsTag = createListTag(AEFluidKey::toTag, fluidInventory);
            if (!fluidsTag.isEmpty()) tag.put("fluidInventory", fluidsTag);

            ListTag fluidCatalystTag = createListTag(AEFluidKey::toTag, fluidCatalystInventory);
            if (!fluidCatalystTag.isEmpty()) tag.put("catalystFluidInventory", fluidCatalystTag);

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            itemInventory.clear();
            itemCatalystInventory.clear();
            fluidInventory.clear();
            fluidCatalystInventory.clear();

            ListTag items = tag.getList("inventory", Tag.TAG_COMPOUND);
            loadInventory(items, AEItemKey::fromTag, itemInventory);

            ListTag catalystItems = tag.getList("catalystInventory", Tag.TAG_COMPOUND);
            loadInventory(catalystItems, AEItemKey::fromTag, itemCatalystInventory);

            ListTag fluids = tag.getList("fluidInventory", Tag.TAG_COMPOUND);
            loadInventory(fluids, AEFluidKey::fromTag, fluidInventory);

            ListTag catalystFluids = tag.getList("catalystFluidInventory", Tag.TAG_COMPOUND);
            loadInventory(catalystFluids, AEFluidKey::fromTag, fluidCatalystInventory);
        }
    }

    public class PendingRefundData implements ITagSerializable<CompoundTag>, IContentChangeAware {

        @Getter
        @Setter
        protected Runnable onContentsChanged = () -> {};
        @Getter
        private final Object2LongOpenHashMap<AEKey> buffer = new Object2LongOpenHashMap<>();

        public PendingRefundData() {
            buffer.defaultReturnValue(0L);
        }

        public void addTo(AEKey key, long amount) {
            if (amount > 0) {
                buffer.addTo(key, amount);
            }
        }

        public boolean reFunds() {
            return AEUtils.reFunds(buffer, getMainNode().getGrid(), actionSource);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag listTag = createListTag(AEKey::toTagGeneric, buffer);
            if (!listTag.isEmpty()) tag.put("buffer", listTag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            buffer.clear();
            ListTag listTag = tag.getList("buffer", Tag.TAG_COMPOUND);
            loadInventory(listTag, AEKey::fromTagGeneric, buffer);
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

            if (pendingRefundData.buffer.isEmpty()) {
                if (ticksSinceLastCall >= ConfigHolder.INSTANCE.MEPatternOutputMax) {
                    isSleeping = true;
                    return TickRateModulation.SLEEP;
                } else return TickRateModulation.SLOWER;
            } else return pendingRefundData.reFunds() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
    }
}
