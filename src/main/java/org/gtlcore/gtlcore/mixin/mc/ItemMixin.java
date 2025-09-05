package org.gtlcore.gtlcore.mixin.mc;

import org.gtlcore.gtlcore.api.data.IItem;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

@Mixin(Item.class)
public class ItemMixin implements IItem {

    @Unique
    private ResourceLocation resourceLocation;
    @Unique
    private String idString;

    @Shadow(remap = false)
    private String descriptionId;

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public String toString() {
        return this.getResourceLocation().getPath();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("item", this.getResourceLocation());
        }
        return this.descriptionId;
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
