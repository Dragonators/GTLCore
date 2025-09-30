package org.gtlcore.gtlcore.data.recipe;

import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static org.gtlcore.gtlcore.GTLCore.id;
import static org.gtlcore.gtlcore.common.data.GTLItems.MIRACLE_CRYSTAL;
import static org.gtlcore.gtlcore.common.data.GTLItems.ULTIMATE_TEA;
import static org.gtlcore.gtlcore.common.data.GTLMaterials.*;
import static org.gtlcore.gtlcore.common.data.GTLRecipeTypes.DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES;
import static org.gtlcore.gtlcore.utils.Registries.getItemStack;

public class DimensionallyMixerRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES.recipeBuilder(id("ultimate_tea"))
                .inputItems(getItemStack("kubejs:heartofthesmogus", 64))
                .inputItems(MIRACLE_CRYSTAL, 64)
                .inputItems(MIRACLE_CRYSTAL, 64)
                .inputItems(MIRACLE_CRYSTAL, 64)
                .inputItems(MIRACLE_CRYSTAL, 64)
                .inputItems(MIRACLE_CRYSTAL, 64)
                .inputFluids(SpaceTime.getFluid(10000000000L))
                .inputFluids(Eternity.getFluid(10000000000L))
                .inputFluids(Miracle.getFluid(10000000000L))
                .inputFluids(MagnetohydrodynamicallyConstrainedStarMatter.getFluid(10000000000L))
                .inputFluids(Shirabon.getFluid(10000000000L))
                .inputFluids(PrimordialMatter.getFluid(10000000000L))
                .outputItems(ULTIMATE_TEA)
                .EUt(V[MAX])
                .duration(1000000)
                .cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                .save(provider);
    }
}
