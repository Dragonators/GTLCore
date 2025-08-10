package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.capability.IMERecipeHandler;

import com.gregtechceu.gtceu.api.capability.recipe.IO;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;

public interface IMERecipeHandlerTrait<T> extends IMERecipeHandler<T> {

    default IO getHandlerIO() {
        return IO.IN;
    }

    ISubscription addChangedListener(Runnable var1);
}
