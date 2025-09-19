package org.gtlcore.gtlcore.common.machine.trait;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.nbt.CompoundTag;

import com.google.common.primitives.Ints;
import lombok.Getter;

import java.util.*;
import java.util.function.BiPredicate;

import static org.gtlcore.gtlcore.api.recipe.IParallelLogic.*;
import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

@Getter
public class MultipleRecipesLogic extends RecipeLogic implements ILockRecipe {

    private final ParallelMachine parallel;

    private final BiPredicate<CompoundTag, IRecipeLogicMachine> dataCheck;

    private static final int MAX_THREADS = 64;

    public MultipleRecipesLogic(ParallelMachine machine) {
        this(machine, null);
    }

    public MultipleRecipesLogic(ParallelMachine machine, BiPredicate<CompoundTag, IRecipeLogicMachine> dataCheck) {
        super((IRecipeLogicMachine) machine);
        this.parallel = machine;
        this.dataCheck = dataCheck;
    }

    @Override
    public WorkableElectricMultiblockMachine getMachine() {
        return (WorkableElectricMultiblockMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        lastRecipe = null;
        var match = getRecipe();
        if (match != null) {
            if (matchRecipeOutput(machine, match)) {
                setupRecipe(match);
            }
        }
    }

    private GTRecipe getRecipe() {
        if (!machine.hasProxies()) return null;
        long maxEUt = getMachine().getOverclockVoltage();
        if (maxEUt <= 0) return null;
        var iterator = lookupRecipeIterator();
        var output = GTRecipeBuilder.ofRaw();
        long totalEu = 0;
        long remain = (long) this.parallel.getMaxParallel() * MAX_THREADS;
        while (remain > 0 && iterator.hasNext()) {
            GTRecipe match = iterator.next();
            if (match == null) continue;
            long p = getMaxParallel(machine, match, remain);
            if (p <= 0) continue;
            else if (p > 1) match = match.copy(ContentModifier.multiplier(p), false);
            match.parallels = Ints.saturatedCast(p);
            match = getRecipeOutputChance(machine, match);
            remain -= p;
            if (handleRecipeInput(machine, match)) {
                totalEu += RecipeHelper.getInputEUt(match) * match.duration;
                output.output.putAll(match.outputs);
            }
            if (totalEu / maxEUt > 20 * 500) break;
        }
        if (output.output.isEmpty()) return null;
        double d = (double) totalEu / maxEUt;
        long eut = d > 20 ? maxEUt : (long) (maxEUt * d / 20);
        output.EUt(eut);
        output.duration = (int) Math.max(d, 20);
        return output.buildRawRecipe();
    }

    private Iterator<GTRecipe> lookupRecipeIterator() {
        if (this.isLock()) {
            if (this.getLockRecipe() == null) {
                this.setLockRecipe(machine.getRecipeType().getLookup()
                        .find(machine, this::checkRecipe));
            } else if (!checkRecipe(this.getLockRecipe())) return Collections.emptyIterator();
            return Collections.singleton(this.getLockRecipe()).iterator();
        } else return machine.getRecipeType().getLookup().getRecipeIterator(machine, this::checkRecipe);
    }

    private boolean checkRecipe(GTRecipe recipe) {
        return matchRecipe(machine, recipe) &&
                recipe.data.getInt("euTier") <= getMachine().getTier() &&
                recipe.checkConditions(this).isSuccess() &&
                (dataCheck == null || dataCheck.test(recipe.data, machine));
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            handleRecipeOutput(this.machine, lastRecipe);
        }
        var match = getRecipe();
        if (match != null) {
            if (matchRecipeOutput(machine, match)) {
                setupRecipe(match);
                return;
            }
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }
}
