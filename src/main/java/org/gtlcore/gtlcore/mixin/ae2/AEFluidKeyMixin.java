package org.gtlcore.gtlcore.mixin.ae2;

import org.gtlcore.gtlcore.api.data.IFluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import appeng.api.stacks.AEFluidKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AEFluidKey.class)
public class AEFluidKeyMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    private final Fluid fluid;
    @Shadow(remap = false)
    @Final
    @Mutable
    private final @Nullable CompoundTag tag;

    public AEFluidKeyMixin(Fluid fluid, @Nullable CompoundTag tag) {
        this.fluid = fluid;
        this.tag = tag;
    }

    @ModifyArg(method = "toTag",
               at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putString(Ljava/lang/String;Ljava/lang/String;)V"),
               index = 1,
               remap = false)
    public String toTag(String key) {
        return ((IFluid) this.fluid).getResourceLocation().toString();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public ResourceLocation getId() {
        return ((IFluid) this.fluid).getResourceLocation();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public String toString() {
        String idString = ((IFluid) this.fluid).getIdString();
        return this.tag == null ? idString : idString + " (+tag)";
    }
}
