package org.gtlcore.gtlcore.mixin.mc;

import org.gtlcore.gtlcore.api.data.IItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

@Mixin(value = Item.class, priority = 0)
public class ItemMixin implements IItem {

    @Unique
    private ResourceLocation resourceLocation;
    @Unique
    private String idString;

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public String toString() {
        return this.getResourceLocation().getPath();
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation() {
        if (this.resourceLocation == null) {
            this.resourceLocation = ForgeRegistries.ITEMS.getKey((Item) (Object) this);
            if (this.resourceLocation == null) {
                this.resourceLocation = new ResourceLocation("minecraft", "air");
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
