package org.gtlcore.gtlcore.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;

public interface IMEIOPartMachine {

    IO getIO();

    void notifySelfIO();

    Iterable<IMERecipeHandlerTrait<?, ?>> getMERecipeHandlerTraits();

    default boolean hasFilter() {
        return false;
    }
}
