package org.gtlcore.gtlcore.mixin.ae2.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingTreeProcess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftingTreeProcess.class)
public interface CraftingTreeProcessAccessor {

    @Accessor(value = "details", remap = false)
    IPatternDetails getDetails();
}
