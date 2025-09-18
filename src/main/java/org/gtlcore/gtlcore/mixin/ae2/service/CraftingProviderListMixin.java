package org.gtlcore.gtlcore.mixin.ae2.service;

import org.gtlcore.gtlcore.integration.ae2.ICraftingProviderList;

import appeng.api.networking.crafting.ICraftingProvider;
import org.spongepowered.asm.mixin.*;

import java.util.List;

@Implements(@Interface(
                       iface = ICraftingProviderList.class,
                       prefix = "gTLCore$"))
@Mixin(targets = "appeng.me.service.helpers.NetworkCraftingProviders$CraftingProviderList")
public abstract class CraftingProviderListMixin {

    @Shadow(remap = false)
    @Final
    private List<ICraftingProvider> providers;

    @Unique
    public int gTLCore$size() {
        return providers.size();
    }
}
