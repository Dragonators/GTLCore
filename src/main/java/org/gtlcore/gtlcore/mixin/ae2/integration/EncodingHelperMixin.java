package org.gtlcore.gtlcore.mixin.ae2.integration;

import appeng.api.stacks.*;
import appeng.integration.modules.jeirei.EncodingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(EncodingHelper.class)
public class EncodingHelperMixin {

    @Inject(method = "findBestIngredient", at = @At("HEAD"), remap = false, cancellable = true)
    private static void findBestIngredient(Map<AEKey, Integer> ingredientPriorities,
                                           List<GenericStack> possibleIngredients,
                                           CallbackInfoReturnable<GenericStack> cir) {
        for (var stack : possibleIngredients) {
            if (stack.what() instanceof AEItemKey aeItemKey) {
                if (aeItemKey.getItem().kjs$getId().contains("universal_circuit") &&
                        ingredientPriorities.containsKey(stack.what()))
                    cir.setReturnValue(stack);
            }

        }
    }
}
