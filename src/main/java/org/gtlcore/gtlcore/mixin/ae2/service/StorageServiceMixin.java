package org.gtlcore.gtlcore.mixin.ae2.service;

import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.hooks.ticking.TickHandler;
import appeng.me.helpers.InterestManager;
import appeng.me.helpers.StackWatcher;
import appeng.me.service.StorageService;
import org.spongepowered.asm.mixin.*;

@Mixin(StorageService.class)
public abstract class StorageServiceMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    private final InterestManager<StackWatcher<IStorageWatcherNode>> interestManager;
    @Shadow(remap = false)
    private boolean cachedStacksNeedUpdate;

    @Shadow(remap = false)
    protected abstract void updateCachedStacks();

    public StorageServiceMixin(InterestManager<StackWatcher<IStorageWatcherNode>> interestManager) {
        this.interestManager = interestManager;
    }

    /**
     * @author .
     * @reason 减少更新频率
     */
    @Overwrite(remap = false)
    public void onServerEndTick() {
        if (this.interestManager.isEmpty()) {
            this.cachedStacksNeedUpdate = true;
        } else {
            if (TickHandler.instance().getCurrentTick() % 10 == 0) this.updateCachedStacks();
        }
    }
}
