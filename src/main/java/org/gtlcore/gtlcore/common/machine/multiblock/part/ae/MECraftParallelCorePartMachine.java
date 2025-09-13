package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.AECraft.IMECraftParallelCore;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class MECraftParallelCorePartMachine extends TieredPartMachine implements IMECraftParallelCore {

    private final int parallel;

    public MECraftParallelCorePartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        parallel = (int) Math.pow(4, tier - 3);
    }

    @Override
    public int getParallel() {
        return parallel;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return false;
    }
}
