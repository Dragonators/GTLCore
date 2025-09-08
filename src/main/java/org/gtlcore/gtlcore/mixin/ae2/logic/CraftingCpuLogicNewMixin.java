package org.gtlcore.gtlcore.mixin.ae2.logic;

import org.gtlcore.gtlcore.api.item.tool.ae2.patternTool.Ae2BaseProcessingPatternHelper;
import org.gtlcore.gtlcore.api.machine.trait.IMEPatternCraftingProvider;
import org.gtlcore.gtlcore.integration.ae2.Ae2CompatMH;

import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
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
public abstract class CraftingCpuLogicNewMixin {

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

            var expectedOutputs = new KeyCounter();
            var expectedContainerItems = new KeyCounter();
            @Nullable
            KeyCounter[] craftingContainer = null;

            boolean needExtract = true;

            for (var provider : craftingService.getProviders(details)) {
                final boolean isMEPatternProvider = provider instanceof IMEPatternCraftingProvider;
                IPatternDetails useDetails = (isProcessing && isMEPatternProvider) ? Ae2BaseProcessingPatternHelper.multiplyScale(taskProgress.getValue(), (AEProcessingPattern) details, level) : details;

                if (needExtract) {
                    craftingContainer = Ae2CompatMH.extractPatternInputs5Args(useDetails, inventory, level, expectedOutputs, expectedContainerItems);
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

                boolean pushed = isMEPatternProvider ? ((IMEPatternCraftingProvider) provider).pushMultiplyPattern(details, useDetails, craftingContainer) : provider.pushPattern(useDetails, craftingContainer);
                if (pushed) {
                    energyService.extractAEPower(patternPower, Actionable.MODULATE, PowerMultiplier.CONFIG);
                    pushedPatterns++;

                    for (var expectedOutput : expectedOutputs) {
                        job.getWaitingFor().insert(expectedOutput.getKey(), expectedOutput.getLongValue(),
                                Actionable.MODULATE);
                    }
                    for (var expectedContainerItem : expectedContainerItems) {
                        job.getWaitingFor().insert(expectedContainerItem.getKey(), expectedContainerItem.getLongValue(),
                                Actionable.MODULATE);
                        Ae2CompatMH.elapsedTimeTrackerAddMaxItems(job.getTimeTracker(), expectedContainerItem.getLongValue(),
                                expectedContainerItem.getKey().getType());
                    }

                    cluster.markDirty();

                    // 1) MEPatternBuffer
                    if (isMEPatternProvider) {
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
                    expectedContainerItems.reset();
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
