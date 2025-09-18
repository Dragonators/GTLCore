package org.gtlcore.gtlcore.integration.ae2;

import appeng.api.networking.crafting.ICraftingProvider;

public interface ICraftingProviderList extends Iterable<ICraftingProvider> {

    int size();
}
