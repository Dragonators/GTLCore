package org.gtlcore.gtlcore.mixin.gtm.api.machine;

import org.gtlcore.gtlcore.api.pattern.util.IMultiblockStateGet;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.common.machine.multiblock.part.*;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeBusPartMachine;
import it.unimi.dsi.fastutil.longs.*;
import lombok.Getter;
import lombok.Setter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiblockState.class)
public abstract class MultiblockStateMixin implements IMultiblockStateGet {

    @Getter
    private boolean isError = false;
    @Getter
    @Setter
    private PatternError errorNormal;
    @Getter
    @Setter
    private PatternError errorFlip;

    @Final
    @Shadow(remap = false)
    public Level world;

    @Final
    @Shadow(remap = false)
    public BlockPos controllerPos;

    @Shadow(remap = false)
    public IMultiController lastController;

    /**
     * @author Dragons
     * @reason Fix the issue that duplicate check form when the output work
     */
    @Overwrite(remap = false)
    public void onBlockStateChanged(BlockPos pos, BlockState state) {
        if (this.world instanceof ServerLevel serverLevel) {
            if (pos.equals(this.controllerPos)) {
                if (this.lastController != null && !state.is(this.lastController.self().getBlockState().getBlock())) {
                    this.lastController.onStructureInvalid();
                    MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                    mwsd.removeMapping((MultiblockState) (Object) this);
                }
            } else {
                final var tempThis = (MultiblockState) (Object) this;
                final IMultiController controller = tempThis.getController();
                if (controller != null) {
                    final boolean formed = controller.isFormed();
                    if (formed) {
                        if (state.getBlock() instanceof ActiveBlock) {
                            LongSet activeBlocks = tempThis.getMatchContext().getOrDefault("vaBlocks", LongSets.emptySet());
                            if (activeBlocks.contains(pos.asLong())) {
                                return;
                            }
                        } else if (serverLevel.getBlockEntity(pos) instanceof IMachineBlockEntity IMBE) {
                            var metaMachine = IMBE.getMetaMachine();
                            if (metaMachine instanceof ItemBusPartMachine ||
                                    metaMachine instanceof FluidHatchPartMachine ||
                                    metaMachine instanceof HugeBusPartMachine)
                                return;
                        }
                    }

                    if (formed && controller.checkPatternWithLock()) {
                        controller.self().setFlipped(tempThis.isNeededFlip());
                        controller.onStructureFormed();
                    } else {
                        controller.self().setFlipped(false);
                        controller.onStructureInvalid();
                        MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.removeMapping(tempThis);
                        mwsd.addAsyncLogic(controller);
                    }
                }
            }
        }
    }

    @Override
    public void cleanState() {
        this.clean();
    }

    @Override
    public boolean updateState(BlockPos posIn, TraceabilityPredicate predicate) {
        return this.update(posIn, predicate);
    }

    @Inject(method = "setError", at = @At("HEAD"), remap = false)
    public void setError(PatternError error, CallbackInfo ci) {
        this.isError = error != null;
    }

    @Shadow(remap = false)
    protected abstract void clean();

    @Shadow(remap = false)
    protected abstract boolean update(BlockPos posIn, TraceabilityPredicate predicate);
}
