package org.gtlcore.gtlcore.api.machine.trait.AECraft;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.NotifiableMAHandlerTrait;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IMECraftIOPart extends ICraftingProvider {

    boolean pushPattern(IPatternDetails details, long multiply);

    void init(@NotNull List<IItemTransfer> transfers);

    @NotNull
    NotifiableMAHandlerTrait getNotifiableMAHandlerTrait();
}
