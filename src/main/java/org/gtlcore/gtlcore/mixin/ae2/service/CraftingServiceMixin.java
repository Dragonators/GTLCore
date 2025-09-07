package org.gtlcore.gtlcore.mixin.ae2.service;

import appeng.hooks.ticking.TickHandler;
import appeng.me.service.CraftingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingService.class)
public class CraftingServiceMixin {

    @Inject(method = "onServerEndTick", at = @At("HEAD"), cancellable = true, remap = false)
    public void onServerEndTick(CallbackInfo ci) {
        if ((TickHandler.instance().getCurrentTick() & 3) == 0) {
            ci.cancel();
        }
    }
}
