package org.gtlcore.gtlcore.common.machine.trait;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.recipe.IRecipeIterator;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.nbt.CompoundTag;

import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
        GTRecipe output = GTRecipeBuilder.ofRaw().buildRawRecipe();
        output.outputs.put(ItemRecipeCapability.CAP, new ArrayList<>());
        output.outputs.put(FluidRecipeCapability.CAP, new ArrayList<>());
        long totalEu = 0;
        long remain = (long) this.parallel.getMaxParallel() * MAX_THREADS;
        while (remain > 0 && iterator.hasNext()) {
            GTRecipe match = iterator.next();
            if (match == null) continue;
            long p = getMaxParallel(machine, match, remain);
            if (p <= 0) continue;
            else if (p > 1) match = match.copy(ContentModifier.multiplier(p), false);
            match.parallels = Ints.saturatedCast(p);
            getRecipeOutputChance(machine, match);
            remain -= p;
            if (handleRecipeInput(machine, match)) {
                totalEu += RecipeHelper.getInputEUt(match) * match.duration;
                var item = match.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) output.outputs.get(ItemRecipeCapability.CAP).addAll(item);
                var fluid = match.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) output.outputs.get(FluidRecipeCapability.CAP).addAll(fluid);
            }
            if (totalEu / maxEUt > 20 * 500) break;
        }
        if (output.outputs.get(ItemRecipeCapability.CAP).isEmpty() && output.outputs.get(FluidRecipeCapability.CAP).isEmpty())
            return null;
        double d = (double) totalEu / maxEUt;
        long eut = d > 20 ? maxEUt : (long) (maxEUt * d / 20);
        output.tickInputs.put(EURecipeCapability.CAP,
                List.of(new Content(eut, 10000, 10000, 0, null, null)));
        output.duration = (int) Math.max(d, 20);
        return output;
    }

    private Iterator<GTRecipe> lookupRecipeIterator() {
        if (this.isLock()) {
            if (this.getLockRecipe() == null) this.setLockRecipe(machine.getRecipeType().getLookup()
                    .find(machine, this::checkRecipe));
            else if (!checkRecipe(this.getLockRecipe())) return Collections.emptyIterator();
            return Collections.singleton(this.getLockRecipe()).iterator();
        } else if (this.machine instanceof IRecipeCapabilityMachine rlm) {
            if (!rlm.getMERecipeHandleParts().isEmpty()) {
                List<GTRecipe> recipes = new ObjectArrayList<>();
                rlm.getMERecipeHandleParts().forEach(m -> recipes.addAll(m.getMachine().getRecipe()));
                var list = recipes.stream().filter(Objects::nonNull).toList();
                if (!list.isEmpty()) return list.listIterator();
            }
        }
        return IRecipeIterator.findIteratorRecipeCollection(
                machine.getRecipeType().getLookup().getRecipeIterator(machine, this::checkRecipe)).iterator();
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
            lastRecipe.postWorking(this.machine);
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
