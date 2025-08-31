package org.gtlcore.gtlcore.api.pattern.util;

import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;

import net.minecraft.core.BlockPos;

public interface IMultiblockStateGet {

    void cleanState();

    boolean updateState(BlockPos posIn, TraceabilityPredicate predicate);

    boolean isError();

    void setErrorNormal(PatternError error);

    void setErrorFlip(PatternError error);

    PatternError getErrorNormal();

    PatternError getErrorFlip();
}
