package org.gtlcore.gtlcore.mixin.ae2.gui;

import appeng.client.gui.me.items.SetProcessingPatternAmountScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SetProcessingPatternAmountScreen.class)
public class SetProcessingPatternAmountScreenMixin {

    /**
     * @author .
     * @reason 样板终端中键设置数量上限
     */
    @Overwrite(remap = false)
    private long getMaxAmount() {
        return Integer.MAX_VALUE;
    }
}
