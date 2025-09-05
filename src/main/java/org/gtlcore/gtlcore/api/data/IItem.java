package org.gtlcore.gtlcore.api.data;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

public interface IItem {

    @NotNull
    ResourceLocation getResourceLocation();

    String getIdString();
}
