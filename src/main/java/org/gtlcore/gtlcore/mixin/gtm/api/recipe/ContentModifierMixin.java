package org.gtlcore.gtlcore.mixin.gtm.api.recipe;

import org.gtlcore.gtlcore.api.recipe.IAdvancedContentModifier;

import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;

import org.spongepowered.asm.mixin.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@Mixin(ContentModifier.class)
public class ContentModifierMixin implements IAdvancedContentModifier {

    @Unique
    private int numerator = -1;

    @Unique
    private int denominator = -1;

    @Unique
    private boolean useFraction;

    @Shadow(remap = false)
    @Final
    private double multiplier;

    @Shadow(remap = false)
    @Final
    private double addition;

    /**
     * @author Dragons
     * @reason QFT空转等问题
     */
    @Overwrite(remap = false)
    public Number apply(Number number) {
        if (number instanceof Long l) {
            if (useFraction) return l * numerator / denominator;
            return number.doubleValue() * this.multiplier + this.addition;
        } else if (number instanceof BigDecimal decimal) {
            return decimal.multiply(BigDecimal.valueOf(this.multiplier)).add(BigDecimal.valueOf(this.addition));
        } else if (number instanceof BigInteger bigInteger) {
            return bigInteger.multiply(BigInteger.valueOf((long) this.multiplier)).add(BigInteger.valueOf((long) this.addition));
        } else {
            return number.doubleValue() * this.multiplier + this.addition;
        }
    }

    @Override
    @Unique
    public void setDivision(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
        this.useFraction = true;
    }
}
