package org.gtlcore.gtlcore.mixin.ae2.crafting.inv;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.NetworkCraftingSimulationState;
import com.google.common.collect.Iterables;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(NetworkCraftingSimulationState.class)
public class NetworkCraftingSimulationStateMixin {

    @Shadow(remap = false)
    @Mutable
    @Final
    private final KeyCounter list;

    public NetworkCraftingSimulationStateMixin(KeyCounter list) {
        this.list = list;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    protected Iterable<AEKey> findFuzzyParent(AEKey what) {
        if (this.list.get(what) != 0L) return Collections.singleton(what);
        return Iterables.transform(this.list.findFuzzy(what, FuzzyMode.IGNORE_ALL), Map.Entry::getKey);
    }
}
