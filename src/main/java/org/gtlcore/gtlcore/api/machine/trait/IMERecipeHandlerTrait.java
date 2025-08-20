package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.capability.IMERecipeHandler;

import com.gregtechceu.gtceu.api.capability.recipe.IO;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;

import java.util.function.Predicate;

/**
 * @author Dragonators
 * @param <T>
 */
public interface IMERecipeHandlerTrait<T extends Predicate<S>, S> extends IMERecipeHandler<T, S> {

    default IO getHandlerIO() {
        return IO.IN;
    }

    ISubscription addChangedListener(Runnable var1);
}
