package org.gtlcore.gtlcore.api.machine.trait.AECraft;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.NotifiableMAHandlerTrait;

import net.minecraft.core.BlockPos;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface IMECraftIOPart extends ICraftingProvider {

    boolean pushPattern(IPatternDetails details, long multiply);

    void init(@NotNull Set<BlockPos> proxies);

    @NotNull
    NotifiableMAHandlerTrait getNotifiableMAHandlerTrait();
}
