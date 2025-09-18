package org.gtlcore.gtlcore.mixin.gtm.api.misc;

import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EnergyContainerList.class)
public abstract class EnergyContainerListMixin {

    /**
     * @author Dragons
     * @reason Performance
     */
    @Overwrite(remap = false)
    private static boolean hasPrimeFactorGreaterThanTwo(long l) {
        return l > 0 && (l & (l - 1)) != 0;
    }
}
