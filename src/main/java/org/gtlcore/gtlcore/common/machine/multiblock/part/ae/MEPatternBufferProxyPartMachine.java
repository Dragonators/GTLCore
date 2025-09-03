package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.*;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
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

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MEPatternBufferProxyPartMachine extends MultiblockPartMachine implements IMachineLife, IMEPatternPartMachine, IInteractedMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEPatternBufferProxyPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    protected final ISubscription[] handlerSubscriptions = new ISubscription[2];

    @Getter
    protected MEPatternBufferProxyRecipeHandler<Ingredient, ItemStack> itemProxyHandler;

    @Getter
    protected MEPatternBufferProxyRecipeHandler<FluidIngredient, FluidStack> fluidProxyHandler;

    @Persisted
    @Getter
    @DescSynced
    private BlockPos bufferPos;

    private @Nullable MEPatternBufferPartMachine buffer = null;
    private boolean bufferResolved = false;

    public MEPatternBufferProxyPartMachine(IMachineBlockEntity holder) {
        super(holder);
        this.itemProxyHandler = new MEPatternBufferProxyRecipeHandler<>(this, ItemRecipeCapability.CAP);
        this.fluidProxyHandler = new MEPatternBufferProxyRecipeHandler<>(this, FluidRecipeCapability.CAP);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel level) {
            level.getServer().tell(new TickTask(0, () -> this.setBuffer(bufferPos)));
        }
    }

    @SuppressWarnings("unchecked")
    public void setBuffer(@Nullable BlockPos pos) {
        bufferResolved = true;
        var level = getLevel();
        releaseBuffer();
        if (level != null && pos != null) {
            if (MetaMachine.getMachine(level, pos) instanceof MEPatternBufferPartMachine machine) {
                bufferPos = pos;
                buffer = machine;
                machine.addProxy(this);
                if (!isRemote()) {
                    var map = machine.getMERecipeHandlerMap();
                    final var itemHandler = (IMERecipeHandlerTrait<Ingredient, ItemStack>) map.get(ItemRecipeCapability.CAP);
                    final var fluidHandler = (IMERecipeHandlerTrait<FluidIngredient, FluidStack>) map.get(FluidRecipeCapability.CAP);
                    itemProxyHandler.setHandler(itemHandler);
                    fluidProxyHandler.setHandler(fluidHandler);
                    handlerSubscriptions[0] = itemHandler.addChangedListener(() -> itemProxyHandler.notifyListeners());
                    handlerSubscriptions[1] = fluidHandler.addChangedListener(() -> fluidProxyHandler.notifyListeners());
                }
            }
        }
        if (!isRemote()) updateIO();
    }

    protected void releaseBuffer() {
        buffer = null;
        bufferPos = null;
        if (!isRemote()) {
            itemProxyHandler.setHandler(null);
            fluidProxyHandler.setHandler(null);
            for (int i = 0; i < handlerSubscriptions.length; i++) {
                if (handlerSubscriptions[i] != null) {
                    handlerSubscriptions[i].unsubscribe();
                    handlerSubscriptions[i] = null;
                }
            }
        }
    }

    protected void updateIO() {
        for (var controller : this.getControllers()) {
            if (controller instanceof IRecipeCapabilityMachine machine) {
                machine.upDate();
            }
        }
        itemProxyHandler.notifyListeners();
        fluidProxyHandler.notifyListeners();
    }

    @Nullable
    public MEPatternBufferPartMachine getBuffer() {
        if (!bufferResolved) setBuffer(bufferPos);
        return buffer;
    }

    @Override
    public MetaMachine self() {
        var buffer = getBuffer();
        return buffer != null ? buffer.self() : super.self();
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return getBuffer() != null;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        assert getBuffer() != null; // UI should never be able to be opened when buffer is null
        return getBuffer().createUI(entityPlayer);
    }

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onMachineRemoved() {
        var buf = getBuffer();
        if (buf != null) {
            buf.removeProxy(this);
        }
        for (int i = 0; i < handlerSubscriptions.length; i++) {
            if (handlerSubscriptions[i] != null) {
                handlerSubscriptions[i].unsubscribe();
                handlerSubscriptions[i] = null;
            }
        }
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;

        if (stack.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                Pair<GTRecipeType, String> researchData = ResearchManager.readResearchId(stack);
                if (researchData != null) {
                    return InteractionResult.PASS;
                }

                // Read pattern buffer position from the data stick
                var tag = stack.getTag();
                if (tag != null && tag.contains("pos")) {
                    int[] posArray = tag.getIntArray("pos");
                    if (posArray.length == 3) {
                        BlockPos bufferPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                        player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"));
                        setBuffer(bufferPos);
                    }
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void setSlotCacheRecipe(int index, GTRecipe recipe) {
        if (buffer != null) {
            buffer.setSlotCacheRecipe(index, recipe);
        }
    }

    @Override
    public void restoreMachineCache(Map<GTRecipe, IRecipeHandlePart> map, MERecipeHandlePart mePart) {
        if (this.buffer == null) return;
        this.buffer.restoreMachineCache(map, mePart);
    }

    @Override
    public @NotNull List<@NotNull GTRecipe> getCachedGTRecipe() {
        if (buffer == null) return Collections.emptyList();
        return buffer.getCachedGTRecipe();
    }

    @Override
    public boolean hasCacheInSlot(int slot) {
        if (buffer == null) return false;
        return buffer.hasCacheInSlot(slot);
    }

    @Override
    public Iterable<IMERecipeHandlerTrait<?, ?>> getMERecipeHandlerTraits() {
        return List.of(itemProxyHandler, fluidProxyHandler);
    }

    @Override
    public void notifySelfIO() {
        if (buffer != null) {
            buffer.notifySelfIO();
        }
    }

    @Override
    public IO getIO() {
        if (buffer != null) {
            return buffer.getIO();
        }
        return IO.NONE;
    }
}
