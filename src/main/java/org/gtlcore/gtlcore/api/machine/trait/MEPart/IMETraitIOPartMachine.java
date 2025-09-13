package org.gtlcore.gtlcore.api.machine.trait.MEPart;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;

public interface IMETraitIOPartMachine extends IMEIOPartMachine {

    Iterable<IMERecipeHandlerTrait<?, ?>> getMERecipeHandlerTraits();

    default boolean hasFilter() {
        return false;
    }
}
