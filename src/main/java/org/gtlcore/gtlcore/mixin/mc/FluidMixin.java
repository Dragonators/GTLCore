package org.gtlcore.gtlcore.mixin.mc;

import org.gtlcore.gtlcore.api.data.IFluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Fluid.class)
public class FluidMixin implements IFluid {

    @Unique
    private ResourceLocation resourceLocation;
    @Unique
    private String idString;

    @Override
    public @NotNull ResourceLocation getResourceLocation() {
        if (this.resourceLocation == null) {
            this.resourceLocation = ForgeRegistries.FLUIDS.getKey((Fluid) (Object) this);
            if (this.resourceLocation == null) {
                this.resourceLocation = new ResourceLocation("minecraft", "water");
            }
        }
        return this.resourceLocation;
    }

    @Override
    public String getIdString() {
        if (this.idString == null || this.idString.isEmpty()) {
            this.idString = this.getResourceLocation().toString();
        }
        return this.idString;
    }
}
