package org.gtlcore.gtlcore.common.item;

import org.gtlcore.gtlcore.api.pattern.util.IMultiblockStateGet;
import org.gtlcore.gtlcore.client.renderer.BlockHighlightHandler;

import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.tool.behavior.IToolBehavior;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.common.item.TooltipBehavior;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author EasterFG on 2024/10/25
 */
public class StructureDetectBehavior extends TooltipBehavior implements IToolBehavior, IInteractionItem {

    public static final StructureDetectBehavior INSTANCE = new StructureDetectBehavior(lines -> {
        lines.add(Component.translatable("item.gtlcore.structure_detect.tooltip.0"));
        lines.add(Component.translatable("item.gtlcore.structure_detect.tooltip.1"));
    });

    /**
     * @param tooltips a consumer adding translated tooltips to the tooltip list
     */
    public StructureDetectBehavior(@NotNull Consumer<List<Component>> tooltips) {
        super(tooltips);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            Level level = context.getLevel();
            if (level.isClientSide) return InteractionResult.PASS;
            BlockPos blockPos = context.getClickedPos();
            if (MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                if (controller.isFormed()) {
                    player.sendSystemMessage(Component.literal("已成型").withStyle(ChatFormatting.GREEN));
                } else {
                    MultiblockState multiblockState = controller.getMultiblockState();
                    if (multiblockState instanceof IMultiblockStateGet stateGet && stateGet.isError()) {
                        if (stateGet.getErrorNormal() != null) showError(player, stateGet.getErrorNormal(), false);
                        if (stateGet.getErrorFlip() != null) showError(player, stateGet.getErrorFlip(), true);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private void showError(Player player, PatternError error, boolean flip) {
        List<Component> show = new ObjectArrayList<>();
        if (error instanceof PatternStringError pe) {
            player.sendSystemMessage(pe.getErrorInfo());
            return;
        }
        var pos = error.getPos();
        var posComponent = Component.translatable("item.gtlcore.structure_detect.error.2", pos.getX(), pos.getY(), pos.getZ(), flip ?
                Component.translatable("item.gtlcore.structure_detect.error.3").withStyle(ChatFormatting.GREEN) :
                Component.translatable("item.gtlcore.structure_detect.error.4").withStyle(ChatFormatting.YELLOW));
        if (error instanceof SinglePredicateError) {
            List<List<ItemStack>> candidates = error.getCandidates();
            var root = candidates.get(0).get(0).getHoverName();
            show.add(Component.translatable("item.gtlcore.structure_detect.error.1", posComponent));
            show.add(Component.literal(" - ").append(root).append(error.getErrorInfo()));
        } else {
            show.add(Component.translatable("item.gtlcore.structure_detect.error.0", posComponent));
            List<List<ItemStack>> candidates = error.getCandidates();
            for (List<ItemStack> candidate : candidates) {
                if (!candidate.isEmpty()) {
                    show.add(Component.literal(" - ").append(candidate.get(0).getDisplayName()));
                }
            }
        }
        show.forEach(player::sendSystemMessage);
        BlockHighlightHandler.highlight(error.getPos(), error.getWorld().dimension(), System.currentTimeMillis() + 15000);
    }
}
