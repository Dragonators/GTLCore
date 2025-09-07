package org.gtlcore.gtlcore.mixin.ae2.logic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "appeng.crafting.execution.ExecutingCraftingJob$TaskProgress")
public interface ExecutingCraftingJobTaskProgressAccessor {

    @Accessor("value")
    long getValue();

    @Accessor("value")
    void setValue(long v);
}
