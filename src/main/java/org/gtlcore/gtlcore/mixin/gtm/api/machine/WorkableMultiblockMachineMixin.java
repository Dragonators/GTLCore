package org.gtlcore.gtlcore.mixin.gtm.api.machine;

import org.gtlcore.gtlcore.api.machine.trait.*;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;

import com.hepdd.gtmthings.common.block.machine.multiblock.part.appeng.MEOutputPartMachine;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(WorkableMultiblockMachine.class)
public abstract class WorkableMultiblockMachineMixin extends MultiblockControllerMachine implements IRecipeCapabilityMachine, IRecipeLogicMachine {

    @Shadow(remap = false)
    @Final
    protected List<ISubscription> traitSubscriptions;
    @Shadow(remap = false)
    @Final
    public RecipeLogic recipeLogic;

    @Getter
    private boolean MEOutPutBus = false;
    @Getter
    private boolean MEOutPutHatch = false;
    @Getter
    private boolean MEOutPutDual = false;
    @Persisted
    @DescSynced
    @Getter
    @Setter
    public boolean isDistinct = false;
    @Getter
    private final List<RecipeHandlePart> recipeHandleParts = new ObjectArrayList<>();
    @Getter
    private final List<MERecipeHandlePart> mERecipeHandleParts = new ObjectArrayList<>();
    @Getter
    private final List<MERecipeHandlePart> mERecipeOutputHandleParts = new ObjectArrayList<>();
    @Getter
    private final Map<GTRecipe, IRecipeHandlePart> recipeHandleMap = new Object2ObjectOpenHashMap<>();
    @Getter
    protected Map<IO, List<RecipeHandlePart>> capabilities = new EnumMap<>(IO.class);
    @Getter
    protected Map<IO, Map<RecipeCapability<?>, List<IRecipeHandler<?>>>> capabilitiesFlat = new EnumMap<>(IO.class);

    public WorkableMultiblockMachineMixin(IMachineBlockEntity holder) {
        super(holder);
    }

    @Inject(method = "onStructureFormed", at = @At("TAIL"), remap = false)
    public void onStructureFormed(CallbackInfo ci) {
        if (this.getLevel() instanceof ServerLevel sl) {
            sl.getServer().tell(new TickTask(1, this::upDate));
        }
        RecipeResult.of(this, null);
        RecipeResult.ofWorking(this, null);
    }

    @Inject(method = "onStructureInvalid", at = @At("TAIL"), remap = false)
    public void onStructureInvalid(CallbackInfo ci) {
        MEOutPutBus = false;
        MEOutPutHatch = false;
        MEOutPutDual = false;
    }

    @Inject(method = "onPartUnload", at = @At("TAIL"), remap = false)
    public void onPartUnload(CallbackInfo ci) {
        MEOutPutBus = false;
        MEOutPutHatch = false;
        MEOutPutDual = false;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public boolean onWorking() {
        return IRecipeLogicMachine.super.onWorking();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void onWaiting() {}

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        return IRecipeLogicMachine.super.beforeWorking(recipe);
    }

    @Override
    public boolean isRecipeOutput(GTRecipe recipe) {
        if (MEOutPutDual) return true;
        else if (MEOutPutBus || recipe.getOutputContents(ItemRecipeCapability.CAP).isEmpty()) {
            return MEOutPutHatch || recipe.getOutputContents(FluidRecipeCapability.CAP).isEmpty();
        }
        return false;
    }

    @Override
    public void setRecipeHandleMap(RecipeHandlePart hatch, GTRecipe recipe) {
        this.recipeHandleMap.put(recipe, hatch);
    }

    @Override
    public void setMERecipeHandleMap(MERecipeHandlePart hatch, GTRecipe recipe, int slot) {
        hatch.getSlotMap().forcePut(recipe, slot);
        this.recipeHandleMap.put(recipe, hatch);
    }

    public void upDate() {
        MEOutPutBus = false;
        MEOutPutHatch = false;
        MEOutPutDual = false;
        capabilities.clear();
        capabilitiesFlat.clear();
        recipeHandleParts.clear();
        mERecipeHandleParts.clear();
        mERecipeOutputHandleParts.clear();
        recipeHandleMap.clear();
        if (!this.isFormed) return;

        // IMultiPart
        var distinctParts = new ObjectArrayList<IRecipeHandler<?>>();
        for (IMultiPart part : this.getParts()) {
            if (part instanceof IMEIOPartMachine mePart) {
                var meHandlers = mePart.getMERecipeHandlerTraits();
                for (var meHandlerTrait : meHandlers) {
                    traitSubscriptions.add(meHandlerTrait.addBufferChangedListener(recipeLogic::updateTickSubscription));
                }
                var me = MERecipeHandlePart.of(mePart);
                if (mePart.getIO() == IO.OUT) {
                    mERecipeOutputHandleParts.add(me);
                    MEOutPutBus = true;
                    MEOutPutHatch = true;
                    MEOutPutDual = true;
                } else {
                    me.restoreMachineCache(recipeHandleMap);
                    mERecipeHandleParts.add(me);
                    if (mePart.getIO() == IO.BOTH) {
                        mERecipeOutputHandleParts.add(me);
                        MEOutPutBus = true;
                        MEOutPutHatch = true;
                        MEOutPutDual = true;
                    }
                }
            } else if (part instanceof FluidHatchPartMachine || part instanceof IDistinctPart) {
                if (part instanceof MEOutputPartMachine) {
                    MEOutPutBus = true;
                    MEOutPutHatch = true;
                    MEOutPutDual = true;
                } else if (part instanceof MEOutputBusPartMachine) {
                    MEOutPutBus = true;
                } else if (part instanceof MEOutputHatchPartMachine) {
                    MEOutPutHatch = true;
                }
                List<IRecipeHandler<?>> hatch = new ObjectArrayList<>();
                boolean isOutput = false;
                for (var v : part.getRecipeHandlers()) {
                    if (!v.isProxy()) {
                        if (v.getHandlerIO() == IO.IN) hatch.add(v);
                        else if (v.getHandlerIO() == IO.OUT) {
                            isOutput = true;
                            hatch.add(v);
                        }
                    }
                }
                if (isDistinct) {
                    recipeHandleParts.add(RecipeHandlePart.of(isOutput ? IO.OUT : IO.IN, hatch));
                } else {
                    if (part instanceof IDistinctPart distinctPart && distinctPart.isDistinct()) {
                        recipeHandleParts.add(RecipeHandlePart.of(IO.IN, hatch));
                    } else {
                        if (isOutput) recipeHandleParts.add(RecipeHandlePart.of(IO.OUT, hatch));
                        else distinctParts.addAll(hatch);
                    }
                }
            }
        }
        if (!mERecipeOutputHandleParts.isEmpty()) mERecipeOutputHandleParts.sort(MERecipeHandlePart.COMPARATOR.reversed());
        if (!distinctParts.isEmpty()) recipeHandleParts.add(RecipeHandlePart.of(IO.IN, distinctParts));
        for (var recipeHandle : getRecipeHandleParts()) {
            this.addHandlerList(recipeHandle);
        }
    }
}
