package org.gtlcore.gtlcore.mixin.ae2.logic;

import org.gtlcore.gtlcore.api.machine.trait.AECraft.IMECraftIOPart;
import org.gtlcore.gtlcore.api.machine.trait.MEPart.IMEPatternPartMachine;
import org.gtlcore.gtlcore.integration.ae2.AEUtils;
import org.gtlcore.gtlcore.integration.ae2.Ae2CompatMH;
import org.gtlcore.gtlcore.integration.ae2.ICraftingProviderList;

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
import org.spongepowered.asm.mixin.*;

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

        int remainingPatterns = maxPatterns;

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
            if (!(craftingService.getProviders(details) instanceof ICraftingProviderList list)) continue;

            final boolean isProcessing = details instanceof AEProcessingPattern;
            final int baseSize = maxPatterns / list.size();
            final int remainder = maxPatterns % list.size();

            KeyCounter expectedOutputs = new KeyCounter(), expectedContainerItems = new KeyCounter();
            KeyCounter[] craftingContainer = null;
            boolean needExtract = true;
            int index = 0;

            for (var provider : list) {
                final boolean isMEPatternProvider = isProcessing && provider instanceof IMEPatternPartMachine;
                final boolean isMECraftProvider = provider instanceof IMECraftIOPart;
                final int size = isProcessing ? (int) Math.min(Math.min(baseSize + (index++ < remainder ? 1 : 0), remainingPatterns), taskProgress.getValue()) : 1;

                if (needExtract) {
                    craftingContainer = isMEPatternProvider ? AEUtils.extractForProcessingPattern((AEProcessingPattern) details, inventory, expectedOutputs, taskProgress.getValue()) : isMECraftProvider ? Ae2CompatMH.extractForCraftPattern5Args(details, inventory, level, expectedOutputs, expectedContainerItems, taskProgress.getValue()) : isProcessing ? AEUtils.extractForProcessingPattern((AEProcessingPattern) details, inventory, expectedOutputs, size) : Ae2CompatMH.extractForCraftPattern5Args(details, inventory, level, expectedOutputs, expectedContainerItems);
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

                    // 1) MEPatternBuffer || MECraftProvider
                    if (isMEPatternProvider || isMECraftProvider) {
                        taskProgress.setValue(0);
                        remainingPatterns--;
                        it.remove();
                        continue taskLoop;
                    }

                    // 2) Others
                    taskProgress.setValue(taskProgress.getValue() - size);
                    remainingPatterns -= size;
                    if (taskProgress.getValue() <= 0) {
                        it.remove();
                        continue taskLoop;
                    }

                    if (remainingPatterns == 0) {
                        break taskLoop;
                    }

                    expectedOutputs.reset();
                    expectedContainerItems.reset();
                    craftingContainer = null;
                    needExtract = true;
                }
            }

            if (craftingContainer != null) {
                CraftingCpuHelper.reinjectPatternInputs(inventory, craftingContainer);
            }
        }

        return maxPatterns - remainingPatterns;
    }
}
