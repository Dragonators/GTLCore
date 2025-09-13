package org.gtlcore.gtlcore.mixin.ae2.logic;

import org.gtlcore.gtlcore.api.machine.trait.AECraft.IMECraftIOPart;
import org.gtlcore.gtlcore.api.machine.trait.MEPart.IMEPatternPartMachine;
import org.gtlcore.gtlcore.integration.ae2.AEUtils;
import org.gtlcore.gtlcore.integration.ae2.Ae2CompatMH;

import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CraftingCpuLogic.class)
public abstract class CraftingCpuLogicOldMixin {

    @Shadow(remap = false)
    private ExecutingCraftingJob job;

    @Shadow(remap = false)
    @Final
    CraftingCPUCluster cluster;

    @Shadow(remap = false)
    @Final
    private ListCraftingInventory inventory;

    /**
     * @author Dragons
     * @reason ME样板总成自动翻倍
     */
    @Overwrite(remap = false)
    public int executeCrafting(int maxPatterns, CraftingService craftingService, IEnergyService energyService,
                               Level level) {
        var job = (ExecutingCraftingJobAccessor) (this.job);
        if (job == null) return 0;

        var pushedPatterns = 0;

        var it = job.getTasks().entrySet().iterator();
        taskLoop:
        while (it.hasNext()) {
            var task = it.next();
            var taskProgress = (ExecutingCraftingJobTaskProgressAccessor) (task.getValue());
            if (taskProgress.getValue() <= 0) {
                it.remove();
                continue;
            }

            var details = task.getKey();
            final boolean isProcessing = details instanceof AEProcessingPattern;

            KeyCounter expectedOutputs = new KeyCounter();
            @Nullable
            KeyCounter[] craftingContainer = null;

            boolean needExtract = true;

            for (var provider : craftingService.getProviders(details)) {
                final boolean isMEPatternProvider = isProcessing && provider instanceof IMEPatternPartMachine;
                final boolean isMECraftProvider = provider instanceof IMECraftIOPart;

                if (needExtract) {
                    craftingContainer = isMEPatternProvider ? AEUtils.extractForMEPatternBuffer((AEProcessingPattern) details, inventory, taskProgress.getValue(), expectedOutputs) : isMECraftProvider ? Ae2CompatMH.extractPatternInputs4Args(details, inventory, level, expectedOutputs, taskProgress.getValue()) : Ae2CompatMH.extractPatternInputs4Args(details, inventory, level, expectedOutputs);

                    needExtract = false;
                    if (craftingContainer == null) {
                        break;
                    }
                }

                if (provider.isBusy()) continue;

                var patternPower = CraftingCpuHelper.calculatePatternPower(craftingContainer);
                if (energyService.extractAEPower(patternPower, Actionable.SIMULATE, PowerMultiplier.CONFIG) < patternPower - 0.01) {
                    break;
                }

                if (isMECraftProvider ? ((IMECraftIOPart) provider).pushPattern(details, taskProgress.getValue()) : provider.pushPattern(details, craftingContainer)) {
                    energyService.extractAEPower(patternPower, Actionable.MODULATE, PowerMultiplier.CONFIG);
                    pushedPatterns++;

                    for (var expectedOutput : expectedOutputs) {
                        job.getWaitingFor().insert(expectedOutput.getKey(), expectedOutput.getLongValue(),
                                Actionable.MODULATE);
                    }

                    cluster.markDirty();

                    // 1) MEPatternBuffer || MECraftProvider
                    if (isMEPatternProvider || isMECraftProvider) {
                        it.remove();
                        continue taskLoop;
                    }

                    // 2) Others
                    taskProgress.setValue(taskProgress.getValue() - 1);
                    if (taskProgress.getValue() <= 0) {
                        it.remove();
                        continue taskLoop;
                    }

                    if (pushedPatterns == maxPatterns) {
                        break taskLoop;
                    }

                    expectedOutputs.reset();
                    craftingContainer = null;
                    needExtract = true;
                }
            }

            if (craftingContainer != null) {
                CraftingCpuHelper.reinjectPatternInputs(inventory, craftingContainer);
            }
        }

        return pushedPatterns;
    }
}
