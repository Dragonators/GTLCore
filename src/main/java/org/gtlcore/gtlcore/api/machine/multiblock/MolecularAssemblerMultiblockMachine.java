package org.gtlcore.gtlcore.api.machine.multiblock;

import org.gtlcore.gtlcore.api.machine.trait.AECraft.IMECraftPatternContainer;
import org.gtlcore.gtlcore.api.machine.trait.ICheckPatternMachine;
import org.gtlcore.gtlcore.common.data.GTLMachines;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class MolecularAssemblerMultiblockMachine extends MolecularAssemblerMultiblockMachineBase implements IFancyUIMachine, IDisplayUIMachine, ICheckPatternMachine, IInteractedMachine {

    public MolecularAssemblerMultiblockMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit) {
        if (this.isFormed() && !world.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                final int tier;
                if (stack.is(GTItems.FIELD_GENERATOR_UHV.asItem())) tier = 9;
                else if (stack.is(GTItems.FIELD_GENERATOR_UEV.asItem())) tier = 10;
                else if (stack.is(GTItems.FIELD_GENERATOR_UIV.asItem())) tier = 11;
                else if (stack.is(GTItems.FIELD_GENERATOR_UXV.asItem())) tier = 12;
                else tier = -1;
                if (tier != -1) {
                    var patternContainers = this.getParts().stream()
                            .filter(p -> p instanceof IMECraftPatternContainer && p instanceof ITieredMachine tieredPartMachine && tieredPartMachine.getTier() < tier)
                            .sorted(Comparator.comparingInt(part -> IMECraftPatternContainer.sumNonEmpty(((IMECraftPatternContainer) part).getItemTransfer())).reversed()
                                    .thenComparingInt(part -> ((ITieredMachine) part).getTier()))
                            .toList();

                    if (!patternContainers.isEmpty()) {
                        this.onStructureInvalid();
                        ItemStack replaceStack = GTLMachines.GTAEMachines.ME_CRAFT_PATTERN_CONTAINER[tier].asStack();
                        for (IMultiPart patternContainer : patternContainers) {
                            BlockPos replacePos = patternContainer.self().getPos();
                            IItemTransfer oldTransfer = ((IMECraftPatternContainer) patternContainer).getItemTransfer();

                            Int2ObjectMap<ItemStack> savedItems = new Int2ObjectOpenHashMap<>();
                            for (int i = 0; i < oldTransfer.getSlots(); i++) {
                                ItemStack oldStack = oldTransfer.extractItem(i, 1, false, false);
                                if (oldStack.isEmpty()) continue;
                                savedItems.put(i, oldStack);
                            }

                            if (!world.removeBlock(replacePos, true)) {
                                for (var entry : Int2ObjectMaps.fastIterable(savedItems)) {
                                    oldTransfer.setStackInSlot(entry.getIntKey(), entry.getValue());
                                }
                                this.setTime(0);
                                return InteractionResult.PASS;
                            }

                            BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND,
                                    replaceStack, BlockHitResult.miss(player.getEyePosition(0), Direction.UP, replacePos));
                            InteractionResult interactionResult = ((BlockItem) (replaceStack.getItem())).place(context);

                            if (interactionResult == InteractionResult.FAIL) {
                                for (var entry : Int2ObjectMaps.fastIterable(savedItems)) {
                                    Containers.dropItemStack(world,
                                            player.getX(),
                                            player.getY() + 0.5,
                                            player.getZ(),
                                            entry.getValue());
                                }
                                savedItems.clear();
                                return InteractionResult.PASS;
                            }

                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, ItemStack.EMPTY);
                                break;
                            }

                            if (world.getBlockEntity(replacePos) instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof IMECraftPatternContainer newContainer) {
                                final var newTransfer = newContainer.getItemTransfer();
                                for (var entry : Int2ObjectMaps.fastIterable(savedItems)) {
                                    newTransfer.setStackInSlot(entry.getIntKey(), entry.getValue());
                                }
                            }
                        }

                        this.setTime(0);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    // ========================================
    // GUI
    // ========================================

    @Override
    public void addDisplayText(List<Component> textList) {
        IDisplayUIMachine.super.addDisplayText(textList);
        if (isFormed()) {
            textList.add(Component.translatable(getRecipeType().registryName.toLanguageKey())
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("gtceu.gui.machinemode.title")))));
            if (!isWorkingEnabled()) {
                textList.add(Component.translatable("gtceu.multiblock.work_paused"));
            } else if (isActive()) {
                textList.add(Component.translatable("gtceu.multiblock.running"));
                int currentProgress = (int) (recipeLogic.getProgressPercent() * 100);
                textList.add(Component.translatable("gtceu.multiblock.progress", currentProgress));
            } else {
                textList.add(Component.translatable("gtceu.multiblock.idling"));
            }
            if (recipeLogic.isWaiting()) {
                textList.add(Component.translatable("gtceu.multiblock.waiting")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            }
            if (maxParallel > 1) {
                textList.add(Component.translatable("gtceu.multiblock.parallel", Component.literal(FormattingUtil.formatNumbers(maxParallel)).withStyle(ChatFormatting.DARK_PURPLE))
                        .withStyle(ChatFormatting.GRAY));
            }
            textList.add(Component.translatable("gtlcore.multiblock.tick_Duration", Component.literal(FormattingUtil.formatNumbers(tickDuration)).withStyle(ChatFormatting.BLUE))
                    .withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable("gtlcore.multiblock.contains_Patttern", Component.literal(FormattingUtil.formatNumbers(patternSize)).withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            Component tooltip = Component.translatable("gtceu.multiblock.invalid_structure.tooltip")
                    .withStyle(ChatFormatting.GRAY);
            textList.add(Component.translatable("gtceu.multiblock.invalid_structure")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
        getDefinition().getAdditionalDisplay().accept(this, textList);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IFancyUIMachine.super.attachConfigurators(configuratorPanel);
        ICheckPatternMachine.attachConfigurators(configuratorPanel, this);
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182 + 8, 117 + 8);
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 182, 117)
                .setBackground(getScreenTexture())
                .addWidget(new LabelWidget(4, 5, self().getBlockState().getBlock().getDescriptionId()))
                .addWidget(new ComponentPanelWidget(4, 17, this::addDisplayText)
                        .setMaxWidthLimit(150)
                        .clickHandler(this::handleDisplayClick)));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(198, 208, this, entityPlayer)
                .widget(new FancyMachineUIWidget(this, 198, 208));
    }

    @Override
    public List<IFancyUIProvider> getSubTabs() {
        return getParts().stream()
                .filter(Objects::nonNull)
                .map(IFancyUIProvider.class::cast)
                .toList();
    }

    @Override
    public void attachTooltips(TooltipsPanel tooltipsPanel) {
        for (IMultiPart part : getParts()) {
            part.attachFancyTooltipsToController(this, tooltipsPanel);
        }
    }

    @Override
    public boolean hasButton() {
        return true;
    }
}
