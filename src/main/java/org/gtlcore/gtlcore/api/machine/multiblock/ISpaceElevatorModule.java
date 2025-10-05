package org.gtlcore.gtlcore.api.machine.multiblock;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ISpaceElevatorModule {

    BlockPos getPos();

    void removeFromElevator(@Nullable SpaceElevatorMachine elevator);

    void connectToElevator(@NotNull SpaceElevatorMachine elevator);

    boolean isFormed();

    BlockState getBlockState();
}
