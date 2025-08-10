package org.gtlcore.gtlcore.api.machine.trait;

import org.gtlcore.gtlcore.api.capability.IMERecipeHandler;

import java.util.List;

public interface IMEPatternPartMachine {

    List<IMERecipeHandler<?>> getMERecipeHandlers();
}
