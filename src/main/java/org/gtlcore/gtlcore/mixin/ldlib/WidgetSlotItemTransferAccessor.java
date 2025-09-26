package org.gtlcore.gtlcore.mixin.ldlib;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SlotWidget.WidgetSlotItemTransfer.class)
public interface WidgetSlotItemTransferAccessor {

    @Accessor(remap = false)
    int getIndex();
}
