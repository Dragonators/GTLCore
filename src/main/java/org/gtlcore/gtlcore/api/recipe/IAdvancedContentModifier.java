package org.gtlcore.gtlcore.api.recipe;

import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;

public interface IAdvancedContentModifier {

    void setDivision(int numerator, int denominator);

    static ContentModifier preciseDivision(int numerator, int denominator) {
        var modifier = new ContentModifier(0, 0);
        ((IAdvancedContentModifier) modifier).setDivision(numerator, denominator);
        return modifier;
    }
}
