package org.gtlcore.gtlcore.api.machine.trait;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;

public interface IMEPatternCraftingProvider extends ICraftingProvider {

    boolean pushMultiplyPattern(IPatternDetails originPatternDetails, IPatternDetails multiplyPatternDetails, KeyCounter[] inputHolder);
}
