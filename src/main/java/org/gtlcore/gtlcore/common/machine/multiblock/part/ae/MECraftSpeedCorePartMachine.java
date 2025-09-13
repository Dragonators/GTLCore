package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.AECraft.IMECraftSpeedCore;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class MECraftSpeedCorePartMachine extends TieredPartMachine implements IMECraftSpeedCore {

    public MECraftSpeedCorePartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
    }

    @Override
    public int getSpeedTier() {
        return this.tier;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return false;
    }
}
