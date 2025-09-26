package org.gtlcore.gtlcore.mixin.gtm.ae.slot;

import org.gtlcore.gtlcore.integration.ae2.AEUtils;

import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;

import net.minecraft.nbt.CompoundTag;

import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ExportOnlyAESlot.class)
public abstract class ExportOnlyAESlotMixin {

    @Shadow(remap = false)
    protected @Nullable GenericStack config;

    @Shadow(remap = false)
    protected @Nullable GenericStack stock;

    /**
     * @author Dragons
     * @reason 修复序列化问题
     */
    @Overwrite(remap = false)
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (this.config != null) {
            CompoundTag configTag = AEUtils.writeTag(this.config);
            tag.put("config", configTag);
        }

        if (this.stock != null) {
            CompoundTag stockTag = AEUtils.writeTag(this.stock);
            tag.put("stock", stockTag);
        }

        return tag;
    }
}
