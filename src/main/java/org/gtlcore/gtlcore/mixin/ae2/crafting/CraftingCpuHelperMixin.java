package org.gtlcore.gtlcore.mixin.ae2.crafting;

import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.*;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.execution.InputTemplate;
import appeng.crafting.inv.ICraftingInventory;
import appeng.crafting.pattern.AEProcessingPattern;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static appeng.crafting.execution.CraftingCpuHelper.extractTemplates;
import static appeng.crafting.execution.CraftingCpuHelper.reinjectPatternInputs;

@Mixin(CraftingCpuHelper.class)
public class CraftingCpuHelperMixin {

    @SuppressWarnings("all")
    @Group(name = "extractInputs", min = 1)
    @Inject(method = "extractPatternInputs" +
            "(Lappeng/api/crafting/IPatternDetails;" +
            "Lappeng/crafting/inv/ICraftingInventory;" +
            "Lnet/minecraft/world/level/Level;" +
            "Lappeng/api/stacks/KeyCounter;" +
            "Lappeng/api/stacks/KeyCounter;" +
            ")[Lappeng/api/stacks/KeyCounter;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private static void extractPatternInputs(IPatternDetails details,
                                             ICraftingInventory sourceInv,
                                             Level level,
                                             KeyCounter expectedOutputs,
                                             KeyCounter expectedContainerItems,
                                             CallbackInfoReturnable<KeyCounter[]> cir) {
        if (details instanceof AEProcessingPattern) {
            IPatternDetails.IInput[] inputs = details.getInputs();
            KeyCounter[] inputHolder = new KeyCounter[inputs.length];
            boolean found = true;

            for (int x = 0; x < inputs.length; x++) {
                var list = inputHolder[x] = new KeyCounter();
                long remainingMultiplier = inputs[x].getMultiplier();
                for (var template : getValidItemTemplate(inputs[x], level)) {
                    long extracted = extractTemplates(sourceInv, template, remainingMultiplier);
                    list.add(template.key(), extracted * template.amount());

                    var containerItem = inputs[x].getRemainingKey(template.key());
                    if (containerItem != null) {
                        expectedContainerItems.add(containerItem, extracted);
                    }

                    remainingMultiplier -= extracted;
                    if (remainingMultiplier == 0)
                        break;
                }

                if (remainingMultiplier > 0) {
                    found = false;
                    break;
                }
            }

            if (!found) {
                reinjectPatternInputs(sourceInv, inputHolder);
                cir.setReturnValue(null);
            } else {
                for (var output : details.getOutputs()) expectedOutputs.add(output.what(), output.amount());
                cir.setReturnValue(inputHolder);
            }
        }
    }

    @SuppressWarnings("all")
    @Group(name = "extractInputs")
    @Inject(method = "extractPatternInputs" +
            "(Lappeng/api/crafting/IPatternDetails;" +
            "Lappeng/crafting/inv/ICraftingInventory;" +
            "Lnet/minecraft/world/level/Level;" +
            "Lappeng/api/stacks/KeyCounter;" +
            ")[Lappeng/api/stacks/KeyCounter;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private static void extractPatternInputs(IPatternDetails details,
                                             ICraftingInventory sourceInv,
                                             Level level,
                                             KeyCounter expectedOutputs,
                                             CallbackInfoReturnable<KeyCounter[]> cir) {
        if (details instanceof AEProcessingPattern) {
            IPatternDetails.IInput[] inputs = details.getInputs();
            KeyCounter[] inputHolder = new KeyCounter[inputs.length];
            boolean found = true;

            for (int x = 0; x < inputs.length; x++) {
                var list = inputHolder[x] = new KeyCounter();
                long remainingMultiplier = inputs[x].getMultiplier();
                for (var template : getValidItemTemplate(inputs[x], level)) {
                    long extracted = extractTemplates(sourceInv, template, remainingMultiplier);
                    list.add(template.key(), extracted * template.amount());

                    var containerItem = inputs[x].getRemainingKey(template.key());
                    if (containerItem != null) {
                        expectedOutputs.add(containerItem, extracted);
                    }

                    remainingMultiplier -= extracted;
                    if (remainingMultiplier == 0)
                        break;
                }

                if (remainingMultiplier > 0) {
                    found = false;
                    break;
                }
            }

            if (!found) {
                reinjectPatternInputs(sourceInv, inputHolder);
                cir.setReturnValue(null);
            } else {
                for (var output : details.getOutputs()) expectedOutputs.add(output.what(), output.amount());
                cir.setReturnValue(inputHolder);
            }
        }
    }

    /**
     * @author .
     * @reason 提升性能
     */
    @Overwrite(remap = false)
    public static Iterable<InputTemplate> getValidItemTemplates(ICraftingInventory inv,
                                                                IPatternDetails.IInput input, Level level) {
        var possibleInputs = input.getPossibleInputs();
        List<InputTemplate> substitutes = new ObjectArrayList<>(possibleInputs.length);
        for (var stack : possibleInputs) {
            for (var fuzz : inv.findFuzzyTemplates(stack.what())) {
                if (fuzz instanceof AEItemKey aeItemKey && !aeItemKey.matches(stack)) continue;
                else if (fuzz instanceof AEFluidKey aeFluidKey && !aeFluidKey.matches(stack)) continue;
                substitutes.add(new InputTemplate(fuzz, stack.amount()));
            }
        }
        return Iterables.filter(substitutes, stack -> input.isValid(stack.key(), level));
    }

    @Unique
    private static Iterable<InputTemplate> getValidItemTemplate(IPatternDetails.IInput input, Level level) {
        var possibleInputs = input.getPossibleInputs();
        List<InputTemplate> substitutes = new ObjectArrayList<>(possibleInputs.length);
        for (var stack : possibleInputs) substitutes.add(new InputTemplate(stack.what(), stack.amount()));
        return Iterables.filter(substitutes, stack -> input.isValid(stack.key(), level));
    }
}
