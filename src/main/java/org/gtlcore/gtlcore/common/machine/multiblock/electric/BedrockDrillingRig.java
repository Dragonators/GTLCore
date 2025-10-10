package org.gtlcore.gtlcore.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BedrockDrillingRig extends WorkableElectricMultiblockMachine {

    protected TickableSubscription tickCheckSubs;

    public BedrockDrillingRig(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTickSubscription));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickCheckSubs != null) {
            tickCheckSubs.unsubscribe();
            tickCheckSubs = null;
        }
    }

    protected void updateTickSubscription() {
        if (isFormed) {
            tickCheckSubs = subscribeServerTick(tickCheckSubs, this::checkBedrock);
        } else if (tickCheckSubs != null) {
            tickCheckSubs.unsubscribe();
            tickCheckSubs = null;
        }
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTickSubscription));
        }
    }

    private void checkBedrock() {
        if (this.getOffsetTimer() % 20L == 0) {
            var state = Objects.requireNonNull(getLevel()).getBlockState(getPos().offset(0, -9, 0));
            if (state.getBlock().kjs$getId().equals("minecraft:bedrock")) this.recipeLogic.serverTick();
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        Level level = this.self().getLevel();
        if (level != null) {
            if (Math.random() < 0.1) {
                level.setBlockAndUpdate(this.self().getPos().offset(0, -9, 0), Blocks.AIR.defaultBlockState());
            }
            return Objects.equals(level.getBlockState(this.self().getPos().offset(0, -9, 0)).getBlock().kjs$getId(), "minecraft:bedrock");
        }
        return false;
    }
}
