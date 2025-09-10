package org.gtlcore.gtlcore.mixin.ae2.crafting;

import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.*;
import appeng.crafting.*;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.execution.InputTemplate;
import appeng.crafting.inv.ICraftingInventory;
import appeng.crafting.pattern.AEProcessingPattern;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CraftingTreeNode.class)
public class CraftingTreeNodeMixin {

    @Unique
    private IPatternDetails patternDetails;

    @Shadow(remap = false)
    @Final
    @Mutable
    final IPatternDetails.@Nullable IInput parentInput;
    @Shadow(remap = false)
    @Final
    @Mutable
    private final Level level;
    @Shadow(remap = false)
    @Final
    @Mutable
    private final AEKey what;

    public CraftingTreeNodeMixin(IPatternDetails.@Nullable IInput parentInput, Level level, AEKey what) {
        this.parentInput = parentInput;
        this.level = level;
        this.what = what;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void CraftingTreeNode(ICraftingService cc, CraftingCalculation job, AEKey what, long amount, CraftingTreeProcess par, int slot, CallbackInfo ci) {
        this.patternDetails = slot == -1 ? null : ((CraftingTreeProcessAccessor) par).getDetails();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    private Iterable<InputTemplate> getValidItemTemplates(ICraftingInventory inv) {
        if (this.parentInput == null)
            return List.of(new InputTemplate(what, 1));
        else if (this.patternDetails instanceof AEProcessingPattern) {
            GenericStack stack = this.parentInput.getPossibleInputs()[0];
            return List.of(new InputTemplate(stack.what(), stack.amount()));
        }
        return CraftingCpuHelper.getValidItemTemplates(inv, this.parentInput, level);
    }
}
