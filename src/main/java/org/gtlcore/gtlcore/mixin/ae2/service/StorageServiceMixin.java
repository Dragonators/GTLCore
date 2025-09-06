package org.gtlcore.gtlcore.mixin.ae2.service;

import org.gtlcore.gtlcore.integration.ae2.stacks.IKeyCounter;

import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.*;
import appeng.hooks.ticking.TickHandler;
import appeng.me.helpers.InterestManager;
import appeng.me.helpers.StackWatcher;
import appeng.me.service.StorageService;
import appeng.me.storage.NetworkStorage;
import it.unimi.dsi.fastutil.objects.*;
import org.spongepowered.asm.mixin.*;

@Mixin(StorageService.class)
public abstract class StorageServiceMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    private final InterestManager<StackWatcher<IStorageWatcherNode>> interestManager;
    @Shadow(remap = false)
    @Final
    @Mutable
    private final Object2LongMap<AEKey> cachedAvailableAmounts;
    @Shadow(remap = false)
    @Final
    @Mutable
    private final NetworkStorage storage;
    @Shadow(remap = false)
    private boolean cachedStacksNeedUpdate;
    @Shadow(remap = false)
    private KeyCounter cachedAvailableStacks;

    @Shadow(remap = false)
    protected abstract void postWatcherUpdate(AEKey what, long newAmount);

    public StorageServiceMixin(InterestManager<StackWatcher<IStorageWatcherNode>> interestManager, Object2LongMap<AEKey> cachedAvailableAmounts, NetworkStorage storage) {
        this.interestManager = interestManager;
        this.cachedAvailableAmounts = cachedAvailableAmounts;
        this.storage = storage;
    }

    /**
     * @author .
     * @reason 减少更新频率
     */
    @Overwrite(remap = false)
    public void onServerEndTick() {
        if (this.interestManager.isEmpty() || TickHandler.instance().getCurrentTick() % 10 != 0) {
            this.cachedStacksNeedUpdate = true;
        } else {
            this.updateCachedStacks();
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    private void updateCachedStacks() {
        cachedStacksNeedUpdate = false;

        cachedAvailableStacks.clear();
        storage.getAvailableStacks(cachedAvailableStacks);
        IKeyCounter.of(cachedAvailableStacks).removeEmptySubmaps();

        for (var it = Object2LongMaps.fastIterator(cachedAvailableAmounts); it.hasNext();) {
            var entry = it.next();
            var what = entry.getKey();
            var newAmount = cachedAvailableStacks.get(what);
            if (newAmount != cachedAvailableAmounts.getLong(what)) {
                postWatcherUpdate(what, newAmount);
                if (newAmount == 0) it.remove();
                else entry.setValue(newAmount);
            }
        }

        for (var entry : cachedAvailableStacks) {
            var what = entry.getKey();
            var newAmount = entry.getLongValue();
            if (newAmount != cachedAvailableAmounts.getLong(what)) {
                postWatcherUpdate(what, newAmount);
                cachedAvailableAmounts.put(what, newAmount);
            }
        }
    }
}
