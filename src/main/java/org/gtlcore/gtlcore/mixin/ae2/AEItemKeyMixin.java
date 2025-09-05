package org.gtlcore.gtlcore.mixin.ae2;

import org.gtlcore.gtlcore.api.data.IItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import appeng.api.stacks.AEItemKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AEItemKey.class)
public abstract class AEItemKeyMixin {

    private int fuzzySearchMaxValue = -1;

    @Shadow(remap = false)
    @Final
    @Mutable
    private final Item item;

    @Shadow(remap = false)
    public abstract @Nullable CompoundTag getTag();

    public AEItemKeyMixin(Item item) {
        this.item = item;
    }

    @ModifyArg(method = "toTag",
               at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putString(Ljava/lang/String;Ljava/lang/String;)V"),
               index = 1,
               remap = false)
    public String toTag(String key) {
        return ((IItem) this.item).getResourceLocation().toString();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public int getFuzzySearchMaxValue() {
        if (this.fuzzySearchMaxValue < 0) this.fuzzySearchMaxValue = this.item.getMaxDamage();
        return fuzzySearchMaxValue;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public ResourceLocation getId() {
        return ((IItem) this.item).getResourceLocation();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public String toString() {
        String idString = ((IItem) this.item).getIdString();
        return this.getTag() == null ? idString : idString + " (+tag)";
    }
}
