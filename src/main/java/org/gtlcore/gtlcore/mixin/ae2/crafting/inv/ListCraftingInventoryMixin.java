package org.gtlcore.gtlcore.mixin.ae2.crafting.inv;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ListCraftingInventory;
import com.google.common.collect.Iterables;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(ListCraftingInventory.class)
public class ListCraftingInventoryMixin {

    @Shadow(remap = false)
    @Mutable
    @Final
    public final KeyCounter list;

    public ListCraftingInventoryMixin(KeyCounter list) {
        this.list = list;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public Iterable<AEKey> findFuzzyTemplates(AEKey what) {
        if (this.list.get(what) != 0L) return Collections.singleton(what);
        return Iterables.transform(this.list.findFuzzy(what, FuzzyMode.IGNORE_ALL), Map.Entry::getKey);
    }
}
