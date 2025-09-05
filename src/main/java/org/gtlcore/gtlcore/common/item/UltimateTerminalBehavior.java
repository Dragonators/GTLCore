package org.gtlcore.gtlcore.common.item;

import org.gtlcore.gtlcore.api.gui.BlockMapSelectorWidget;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.*;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.*;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;

import com.hepdd.gtmthings.api.gui.widget.TerminalInputWidget;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import static org.gtlcore.gtlcore.api.pattern.AdvancedBlockPattern.getAdvancedBlockPattern;
import static org.gtlcore.gtlcore.common.block.BlockMap.*;

public class UltimateTerminalBehavior implements IItemUIFactory {

    public UltimateTerminalBehavior() {}

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (context.getPlayer() != null && !level.isClientSide() &&
                    MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                AutoBuildSetting autoBuildSetting = getAutoBuildSetting(context.getPlayer().getMainHandItem());

                if (!controller.isFormed()) {
                    getAdvancedBlockPattern(controller.getPattern()).autoBuild(context.getPlayer(), controller.getMultiblockState(), autoBuildSetting);
                } else if (MetaMachine.getMachine(level, blockPos) instanceof WorkableMultiblockMachine machine && autoBuildSetting.isReplaceMode()) {
                    getAdvancedBlockPattern(controller.getPattern()).autoBuild(context.getPlayer(), controller.getMultiblockState(), autoBuildSetting);
                    machine.onPartUnload();
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private AutoBuildSetting getAutoBuildSetting(ItemStack mainHandItem) {
        AutoBuildSetting autoBuildSetting = new AutoBuildSetting();
        var tag = mainHandItem.getTag();
        if (tag != null && !tag.isEmpty()) {
            autoBuildSetting.setTier(tag.getInt("Tier"));
            autoBuildSetting.setRepeatCount(tag.getInt("RepeatCount"));
            autoBuildSetting.setNoHatchMode(tag.getBoolean("NoHatchMode"));
            autoBuildSetting.setReplaceMode(tag.getBoolean("ReplaceMode"));
            autoBuildSetting.setFlipped(tag.getBoolean("IsFlipped"));
            String block = tag.getString("blocks");
            if (!block.isEmpty()) {
                autoBuildSetting.tierBlock = tierBlockMap.get(block);
                autoBuildSetting.blocks = new ObjectOpenHashSet<>(autoBuildSetting.tierBlock);
            }
        }
        return autoBuildSetting;
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder heldItemHolder, Player player) {
        return (new ModularUI(176, 166, heldItemHolder, player)).widget(this.createWidget(player));
    }

    private Widget createWidget(Player player) {
        ItemStack handItem = player.getMainHandItem();
        WidgetGroup group = new WidgetGroup(0, 0, 190, 136);
        DraggableScrollableWidgetGroup contain = new DraggableScrollableWidgetGroup(4, 4, 182, 128)
                .setBackground(GuiTextures.DISPLAY).setYScrollBarWidth(2)
                .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1.0F));
        contain.addWidget(new LabelWidget(65, 8, () -> "终极终端设置界面"));
        contain.addWidget(new LabelWidget(14, 26, () -> "等级方块"));
        contain.addWidget(new LabelWidget(14, 46, () -> "重复次数"));
        contain.addWidget(new LabelWidget(14, 66, () -> "无仓室模式" + (getIsBuildHatches(handItem) ? "(是)" : "(否)"))
                .setHoverTooltips(Component.literal("启用后不会在非唯一时放置各种仓室")));
        contain.addWidget(new LabelWidget(14, 86, () -> "替换模式" + (getReplaceMode(handItem) ? "(是)" : "(否)"))
                .setHoverTooltips(Component.literal("启用后替换等级方块为设置的等级方块")));
        contain.addWidget(new LabelWidget(14, 106, () -> "镜像模式" + (getIsFlip(handItem) ? "(是)" : "(否)"))
                .setHoverTooltips(Component.literal("启用后可以镜像摆放机器方块")));
        contain.addWidget(new TerminalInputWidget(140, 45, 36, 12,
                () -> getRepeatCount(handItem), (v) -> setRepeatCount(v, handItem)).setMax(100).setMin(0));
        contain.addWidget(new SwitchWidget(140, 63, 36, 14,
                (c, b) -> setIsBuildHatches(!getIsBuildHatches(handItem), handItem)).setPressed(getIsBuildHatches(handItem))
                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON"))));
        contain.addWidget(new SwitchWidget(140, 83, 36, 14,
                (c, b) -> setReplaceMode(!getReplaceMode(handItem), handItem)).setPressed(getReplaceMode(handItem))
                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON"))));
        contain.addWidget(new SwitchWidget(140, 103, 36, 14,
                (c, b) -> setIsFlip(!getIsFlip(handItem), handItem)).setPressed(getIsFlip(handItem))
                .setTexture(new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON"))));
        BlockMapSelectorWidget tier = new BlockMapSelectorWidget(80, 23, 56, 16, List.of());
        SelectorWidget type = new SelectorWidget(140, 23, 36, 16,
                List.of("恒星热力容器", "太空电梯动力模块", "部件装配线外壳", "线圈"), -1);
        type.setValue(getBlock(handItem)).setOnChanged(selectedValue -> {
            String s = switch (selectedValue) {
                case "恒星热力容器" -> "sc";
                case "太空电梯动力模块" -> "sepm";
                case "部件装配线外壳" -> "cal";
                case "线圈" -> "coil";
                default -> "";
            };
            CompoundTag tag = handItem.getTag();
            tag.putString("blocks", s);
            Block[] blocks = tierBlockMap.getOrDefault(s, new Block[0]);
            if (blocks.length > 0) {
                tier.setBlocks(new ObjectArrayList<>(blocks));
                tier.isShow();
            }
        }).setBackground(GuiTextures.BACKGROUND_INVERSE).setButtonBackground(GuiTextures.BUTTON).textTexture.setRollSpeed(.5f);
        tier.setOnChanged(s -> setTier(tier.getIndex(s), handItem));
        String block = handItem.getTag().getString("blocks");
        if (!block.isEmpty()) tier.setIndex(getTier(handItem), List.of(tierBlockMap.get(block)));
        contain.addWidget(tier);
        contain.addWidget(type);
        group.addWidget(contain);
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    private static String getBlock(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag != null && !tag.isEmpty()) {
            switch (tag.getString("blocks")) {
                case "sc" -> {
                    return "恒星热力容器";
                }
                case "sepm" -> {
                    return "太空电梯动力模块";
                }
                case "cal" -> {
                    return "部件装配线外壳";
                }
                case "coil" -> {
                    return "线圈";
                }
            }
        }
        return "";
    }

    private static int getTier(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag != null && !tag.isEmpty() ? tag.getInt("Tier") : 0;
    }

    private static void setTier(int tier, ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putInt("Tier", tier);
        itemStack.setTag(tag);
    }

    private static int getRepeatCount(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag != null && !tag.isEmpty() ? tag.getInt("RepeatCount") : 0;
    }

    private static void setRepeatCount(int repeatCount, ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putInt("RepeatCount", repeatCount);
        itemStack.setTag(tag);
    }

    private static boolean getIsBuildHatches(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag == null || tag.isEmpty() || tag.getBoolean("NoHatchMode");
    }

    private static void setIsBuildHatches(boolean isBuildHatches, ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("NoHatchMode", isBuildHatches);
        itemStack.setTag(tag);
    }

    private static boolean getReplaceMode(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag != null && !tag.isEmpty() && tag.getBoolean("ReplaceMode");
    }

    private static void setReplaceMode(boolean isReplace, ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("ReplaceMode", isReplace);
        itemStack.setTag(tag);
    }

    private static boolean getIsFlip(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag != null && !tag.isEmpty() && tag.getBoolean("IsFlipped");
    }

    private static void setIsFlip(boolean isFlip, ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putBoolean("IsFlipped", isFlip);
        itemStack.setTag(tag);
    }

    @Setter
    @Getter
    public static class AutoBuildSetting {

        Block[] tierBlock;
        Set<Block> blocks = Collections.emptySet();
        private int tier, repeatCount;
        private boolean noHatchMode, replaceMode, isFlipped;

        public AutoBuildSetting() {
            this.tier = 0;
            this.repeatCount = 0;
            this.noHatchMode = true;
            this.replaceMode = false;
            this.isFlipped = false;
        }

        public List<ItemStack> apply(BlockInfo[] blockInfos) {
            List<ItemStack> candidates = new ObjectArrayList<>();
            if (blockInfos != null) {
                for (var info : blockInfos) {
                    if (this.tierBlock != null && this.tier >= 0 && blockInfos.length > 1 &&
                            this.blocks.contains(info.getBlockState().getBlock())) {
                        candidates.add(tierBlock[Math.min(this.tier, blockInfos.length - 1)].asItem().getDefaultInstance());
                        return candidates;
                    }
                    if (info.getBlockState().getBlock() != Blocks.AIR) candidates.add(info.getItemStackForm());
                }
            }
            return candidates;
        }

        public boolean isPlaceHatch(BlockInfo[] blockInfos) {
            if (!this.noHatchMode) return true;
            if (blockInfos != null && blockInfos.length > 0) {
                var blockInfo = blockInfos[0];
                return !(blockInfo.getBlockState().getBlock() instanceof MetaMachineBlock machineBlock) ||
                        !Hatch.Set.contains(machineBlock);
            }
            return true;
        }

        private static final class Hatch {

            public static final Set<Block> Set = new ObjectOpenHashSet<>();

            static {
                GTRegistries.MACHINES.forEach(d -> {
                    if (d.getRecipeTypes() != null || d instanceof MultiblockMachineDefinition) return;
                    var block = d.getBlock();
                    if (d.createMetaMachine((IMachineBlockEntity) d.getBlockEntityType().create(BlockPos.ZERO, block.defaultBlockState())) instanceof MultiblockPartMachine partMachine)
                        if (partMachine instanceof TieredIOPartMachine ||
                                partMachine instanceof IParallelHatch ||
                                partMachine instanceof IMaintenanceMachine) {
                                    Set.add(block);
                                }
                });
            }
        }
    }
}
