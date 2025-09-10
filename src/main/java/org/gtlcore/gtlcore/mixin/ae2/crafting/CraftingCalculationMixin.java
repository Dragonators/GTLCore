package org.gtlcore.gtlcore.mixin.ae2.crafting;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.core.AELog;
import appeng.crafting.CraftingCalculation;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(CraftingCalculation.class)
public abstract class CraftingCalculationMixin {

    @Unique
    private final AtomicBoolean gTLCore$done = new AtomicBoolean(false);

    @Shadow(remap = false)
    private void logCraftingJob(ICraftingPlan plan) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private ICraftingPlan computePlan() throws InterruptedException {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason 优化性能
     */
    @Overwrite(remap = false)
    public ICraftingPlan run() {
        try {
            var plan = computePlan();
            this.logCraftingJob(plan);
            return plan;
        } catch (Exception ex) {
            AELog.info(ex, "Exception during async crafting calculation.");
            throw new RuntimeException(ex);
        } finally {
            this.finish();
        }
    }

    /**
     * @author Dragons
     * @reason 优化性能
     */
    @Overwrite(remap = false)
    private void finish() {
        gTLCore$done.set(true);
    }

    /**
     * @author Dragons
     * @reason 优化性能
     */
    @Overwrite(remap = false)
    public boolean simulateFor(int micros) {
        return !this.gTLCore$done.get();
    }

    /**
     * @author Dragons
     * @reason 优化性能
     */
    @Overwrite(remap = false)
    public void handlePausing() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
