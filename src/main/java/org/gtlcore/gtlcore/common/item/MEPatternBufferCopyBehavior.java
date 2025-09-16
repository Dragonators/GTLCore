package org.gtlcore.gtlcore.common.item;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.common.item.TooltipBehavior;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import appeng.core.definitions.AEItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.item.tool.ToolHelper.getBehaviorsTag;
import static org.gtlcore.gtlcore.api.pattern.AdvancedBlockPattern.foundItem;

public class MEPatternBufferCopyBehavior extends TooltipBehavior implements IInteractionItem {

    public static final MEPatternBufferCopyBehavior INSTANCE = new MEPatternBufferCopyBehavior((list -> {
        list.add(Component.translatable("tooltip.gtlcore.copy_pattern_buffer_sneak_right_click"));
        list.add(Component.translatable("tooltip.gtlcore.apply_pattern_buffer_right_click"));
    }));

    public MEPatternBufferCopyBehavior(@NotNull Consumer<List<Component>> tooltips) {
        super(tooltips);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MetaMachineBlockEntity machineBlock) {
            if (machineBlock.getMetaMachine() instanceof MEPatternBufferPartMachine partMachine) {
                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    var tags = getBehaviorsTag(itemStack);
                    if (!serverPlayer.isShiftKeyDown()) {
                        if (tags.isEmpty()) serverPlayer.displayClientMessage(Component.translatable("message.gtlcore.pattern_buffer_not_copied"), true);
                        else {
                            var tag = (CompoundTag) tags.get("tag");
                            partMachine.setCustomName(tag.getString("name"));
                            var list = tag.getList("patterns", 10);
                            int index = 0, listIndex = 0;
                            var inv = partMachine.getTerminalPatternInventory();
                            while (index < inv.size() && listIndex < list.size()) {
                                if (inv.getStackInSlot(index) != ItemStack.EMPTY) {
                                    index++;
                                    continue;
                                }
                                var result = foundItem(serverPlayer,
                                        List.of(AEItems.BLANK_PATTERN.stack()), AEItems.BLANK_PATTERN.stack()::is);
                                if (result.getA() == null) break;
                                var handler = result.getB();
                                inv.setItemDirect(index, ItemStack.of((CompoundTag) list.get(listIndex)));
                                if (handler != null) handler.extractItem(result.getC(), 1, false);
                                index++;
                                listIndex++;
                            }
                        }
                    } else {
                        var tag = new CompoundTag();
                        tag.putString("name", partMachine.getCustomName());
                        var listPattern = new ListTag();
                        partMachine.getPatternSlotMap().values().forEach((i -> {
                            listPattern.add(partMachine.getTerminalPatternInventory().getStackInSlot(i).serializeNBT());
                        }));
                        tag.put("patterns", listPattern);
                        tags.put("tag", tag);
                        if (tags.isEmpty()) serverPlayer.displayClientMessage(Component.translatable("message.gtlcore.pattern_buffer_copy_failed"), true);
                        else serverPlayer.displayClientMessage(Component.translatable("message.gtlcore.pattern_buffer_copied"), true);
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }
}
