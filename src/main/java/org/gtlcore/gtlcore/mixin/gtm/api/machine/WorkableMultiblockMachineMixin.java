package org.gtlcore.gtlcore.mixin.gtm.api.machine;

import org.gtlcore.gtlcore.api.machine.trait.*;
import org.gtlcore.gtlcore.api.machine.trait.MEPart.IMEPatternPartMachine;
import org.gtlcore.gtlcore.api.machine.trait.MEPart.IMETraitIOPartMachine;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;

import com.gregtechceu.gtceu.api.capability.IDataAccessHatch;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.*;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@SuppressWarnings({ "AddedMixinMembersNamePattern", "MissingUnique" })
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
    @Getter
    private boolean MEOutPutWithFilter = false;
    @Persisted
    @DescSynced
    @Getter
    @Setter
    public boolean isDistinct = false;
    @Getter
    private @Nullable IParallelHatch parallelHatch = null;
    private IMufflerMachine mufflerMachine = null;
    @Getter
    private IMaintenanceMachine maintenanceMachine = null;
    private IDataAccessHatch dataAccessHatch = null;
    @Getter
    private final List<RecipeHandlePart> recipeHandleParts = new ObjectArrayList<>();
    @Getter
    private final List<MEPatternRecipeHandlePart> mEPatternRecipeHandleParts = new ObjectArrayList<>();
    @Getter
    private final List<MEIORecipeHandlePart> mEIORecipeHandleParts = new ObjectArrayList<>();
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
    public final @Nullable GTRecipe doModifyRecipe(GTRecipe recipe, @NotNull OCParams params, @NotNull OCResult result) {
        if (this.maintenanceMachine != null) recipe = maintenanceMachine.modifyRecipe(recipe);
        if (recipe != null && this.mufflerMachine != null) recipe = mufflerMachine.modifyRecipe(recipe);
        if (recipe != null && this.dataAccessHatch != null) recipe = dataAccessHatch.modifyRecipe(recipe);
        if (recipe == null) return null;
        return this.self().getDefinition().getRecipeModifier().apply(this.self(), recipe, params, result);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public boolean onWorking() {
        return this.self().getDefinition().getOnWorking().test(this);
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
        return this.self().getDefinition().getBeforeWorking().test(this, recipe);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void afterWorking() {
        if (this.maintenanceMachine != null) maintenanceMachine.afterWorking((IWorkableMultiController) this);
        if (this.mufflerMachine != null) mufflerMachine.afterWorking((IWorkableMultiController) this);
        this.self().getDefinition().getAfterWorking().accept(this);
    }

    @Override
    public boolean isRecipeOutput(GTRecipe recipe) {
        if (MEOutPutWithFilter) return false;
        else if (MEOutPutDual) return true;
        else if (MEOutPutBus || recipe.getOutputContents(ItemRecipeCapability.CAP).isEmpty()) {
            return MEOutPutHatch || recipe.getOutputContents(FluidRecipeCapability.CAP).isEmpty();
        }
        return false;
    }

    @Override
    public void sortMEOutput() {
        if (!mEIORecipeHandleParts.isEmpty()) {
            mEIORecipeHandleParts.sort(MEPatternRecipeHandlePart.COMPARATOR.reversed());
            MEOutPutWithFilter = mEIORecipeHandleParts.get(0).getIoMachine().hasFilter();
        }
    }

    @Override
    public void setRecipeHandleMap(RecipeHandlePart hatch, GTRecipe recipe) {
        this.recipeHandleMap.put(recipe, hatch);
    }

    @Override
    public void setMERecipeHandleMap(MEPatternRecipeHandlePart hatch, GTRecipe recipe, int slot) {
        hatch.getSlotMap().forcePut(recipe, slot);
        this.recipeHandleMap.put(recipe, hatch);
    }

    public void upDate() {
        MEOutPutBus = false;
        MEOutPutHatch = false;
        MEOutPutDual = false;
        MEOutPutWithFilter = false;
        parallelHatch = null;
        mufflerMachine = null;
        maintenanceMachine = null;
        dataAccessHatch = null;
        capabilities.clear();
        capabilitiesFlat.clear();
        recipeHandleParts.clear();
        mEPatternRecipeHandleParts.clear();
        mEIORecipeHandleParts.clear();
        recipeHandleMap.clear();
        if (!this.isFormed) return;

        // IMultiPart
        var distinctParts = new ObjectArrayList<IRecipeHandler<?>>();
        for (IMultiPart part : this.getParts()) {
            if (part instanceof IMETraitIOPartMachine mePart) {
                var meHandlers = mePart.getMERecipeHandlerTraits();
                for (var meHandlerTrait : meHandlers) {
                    traitSubscriptions.add(meHandlerTrait.addBufferChangedListener(recipeLogic::updateTickSubscription));
                }

                if (mePart instanceof IMEPatternPartMachine mePatternPart) {
                    var me = MEPatternRecipeHandlePart.of(mePatternPart);
                    me.restoreMachineCache(recipeHandleMap);
                    mEPatternRecipeHandleParts.add(me);
                    if (mePart.getIO() == IO.BOTH) {
                        mEIORecipeHandleParts.add(me);
                        MEOutPutBus = true;
                        MEOutPutHatch = true;
                        MEOutPutDual = true;
                    }
                } else if (mePart.getIO() == IO.OUT) {
                    mEIORecipeHandleParts.add(MEIORecipeHandlePart.of(mePart));
                    MEOutPutBus = true;
                    MEOutPutHatch = true;
                    MEOutPutDual = true;
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
            } else if (part instanceof IParallelHatch parallel) this.parallelHatch = parallel;
            else if (part instanceof IMufflerMachine muffler) this.mufflerMachine = muffler;
            else if (part instanceof IMaintenanceMachine maintenance) this.maintenanceMachine = maintenance;
            else if (part instanceof IDataAccessHatch data) this.dataAccessHatch = data;
        }
        if (!distinctParts.isEmpty()) recipeHandleParts.add(RecipeHandlePart.of(IO.IN, distinctParts));
        for (var recipeHandle : getRecipeHandleParts()) {
            this.addHandlerList(recipeHandle);
        }
        sortMEOutput();
    }
}
