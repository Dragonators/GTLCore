package org.gtlcore.gtlcore.mixin.gtm.registry;

import org.gtlcore.gtlcore.api.recipe.IAdditionalRecipeIterator;
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.mixin.gtm.api.recipe.GTRecipeLookupAccessor;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeIterator;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.*;

import java.util.*;
import java.util.function.*;

@Mixin(GTRecipeType.class)
public class GTRecipeTypeMixin {

    @Shadow(remap = false)
    private GTRecipeBuilder recipeBuilder;

    @Shadow(remap = false)
    @Final
    public ResourceLocation registryName;

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public GTRecipeType onRecipeBuild(BiConsumer<GTRecipeBuilder, Consumer<FinishedRecipe>> onBuild) {
        if (Objects.equals(registryName, GTCEu.id("cutter"))) {
            recipeBuilder.onSave((recipeBuilder, provider) -> {
                if (recipeBuilder.input.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList()).isEmpty() &&
                        recipeBuilder.tickInput.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList())
                                .isEmpty()) {
                    if (recipeBuilder.EUt() < 524288) {
                        recipeBuilder
                                .copy(new ResourceLocation(recipeBuilder.id.toString() + "_water"))
                                .inputFluids(GTMaterials.Water.getFluid((int) Math.max(4,
                                        Math.min(1000, recipeBuilder.duration * recipeBuilder.EUt() / 320))))
                                .duration(recipeBuilder.duration * 2)
                                .save(provider);
                        recipeBuilder
                                .copy(new ResourceLocation(recipeBuilder.id.toString() + "_distilled_water"))
                                .inputFluids(GTMaterials.DistilledWater.getFluid((int) Math.max(3,
                                        Math.min(750, recipeBuilder.duration * recipeBuilder.EUt() / 426))))
                                .duration((int) (recipeBuilder.duration * 1.5))
                                .save(provider);
                        recipeBuilder
                                .copy(new ResourceLocation(recipeBuilder.id.toString() + "_8_water"))
                                .inputFluids(GTLMaterials.GradePurifiedWater8.getFluid((int) Math.max(1,
                                        Math.min(500, recipeBuilder.duration * recipeBuilder.EUt() / 720))))
                                .duration((int) (recipeBuilder.duration * 0.8))
                                .save(provider);
                        recipeBuilder
                                .copy(new ResourceLocation(recipeBuilder.id.toString() + "_16_water"))
                                .inputFluids(GTLMaterials.GradePurifiedWater16.getFluid((int) Math.max(1,
                                        Math.min(250, recipeBuilder.duration * recipeBuilder.EUt() / 960))))
                                .duration((int) (recipeBuilder.duration * 0.5))
                                .save(provider);
                        recipeBuilder
                                .inputFluids(GTMaterials.Lubricant.getFluid((int) Math.max(1,
                                        Math.min(250, recipeBuilder.duration * recipeBuilder.EUt() / 1280))))
                                .duration(Math.max(1, recipeBuilder.duration));
                    } else if (recipeBuilder.EUt() < GTValues.VA[GTValues.UEV]) {
                        recipeBuilder
                                .copy(new ResourceLocation(recipeBuilder.id.toString() + "_16_water"))
                                .inputFluids(GTLMaterials.GradePurifiedWater16.getFluid((int) Math.max(1,
                                        Math.min(500, recipeBuilder.duration * recipeBuilder.EUt() / 640))))
                                .duration((int) (recipeBuilder.duration * 0.5))
                                .save(provider);
                        recipeBuilder
                                .inputFluids(GTLMaterials.GradePurifiedWater8.getFluid((int) Math.max(1,
                                        Math.min(1000, recipeBuilder.duration * recipeBuilder.EUt() / 320))))
                                .duration(Math.max(1, recipeBuilder.duration));
                    } else {
                        recipeBuilder
                                .inputFluids(GTLMaterials.GradePurifiedWater16.getFluid((int) Math.max(1,
                                        Math.min(1000, recipeBuilder.duration * recipeBuilder.EUt() / 320))))
                                .duration(Math.max(1, recipeBuilder.duration));
                    }
                }
            });
            return recipeBuilder.recipeType;
        }
        if (Objects.equals(registryName, GTCEu.id("circuit_assembler"))) {
            recipeBuilder.onSave((recipeBuilder, provider) -> {
                if (recipeBuilder.input.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList()).isEmpty() &&
                        recipeBuilder.tickInput.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList())
                                .isEmpty()) {
                    if (recipeBuilder.EUt() < 160000) {
                        recipeBuilder.copy(new ResourceLocation(recipeBuilder.id.toString() + "_soldering_alloy"))
                                .inputFluids(GTMaterials.SolderingAlloy
                                        .getFluid(Math.max(1, 72 * recipeBuilder.getSolderMultiplier())))
                                .save(provider);
                        recipeBuilder.inputFluids(
                                GTMaterials.Tin.getFluid(Math.max(1, 144 * recipeBuilder.getSolderMultiplier())));
                    } else {
                        int am = GTUtil.getFloorTierByVoltage(recipeBuilder.EUt()) - 6;
                        recipeBuilder.copy(new ResourceLocation(recipeBuilder.id.toString() + "_soldering_alloy"))
                                .inputFluids(GTLMaterials.SuperMutatedLivingSolder
                                        .getFluid(Math.max(1, 72 * am)))
                                .save(provider);
                        recipeBuilder.inputFluids(
                                GTLMaterials.MutatedLivingSolder.getFluid(Math.max(1, 144 * am)));
                    }
                }
            });
            return recipeBuilder.recipeType;
        }
        recipeBuilder.onSave(onBuild);
        return recipeBuilder.recipeType;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public Iterator<GTRecipe> searchRecipe(IRecipeCapabilityHolder holder) {
        if (!holder.hasProxies()) {
            return null;
        } else {
            RecipeIterator iterator = this.getLookup().getRecipeIterator(holder,
                    (recipex) -> RecipeRunnerHelper.matchRecipe(holder, recipex) && recipex.matchTickRecipe(holder).isSuccess());

            if (((IAdditionalRecipeIterator) iterator).hasAdditionalRecipes()) {
                ((IAdditionalRecipeIterator) iterator).setAdditionalRecipesCanHandle(recipe -> recipe.recipeType == ((GTRecipeLookupAccessor) this.getLookup()).getRecipeType());
            }

            GTRecipe recipe = null;
            if (iterator.hasNext()) {
                recipe = iterator.next();
            }

            if (recipe != null) {
                iterator.reset();
                return Collections.singleton(recipe).iterator();
            } else {
                for (GTRecipeType.ICustomRecipeLogic logic : this.customRecipeLogicRunners) {
                    recipe = logic.createCustomRecipe(holder);
                    if (recipe != null) {
                        return Collections.singleton(recipe).iterator();
                    }
                }
                return Collections.emptyIterator();
            }
        }
    }

    public GTRecipeTypeMixin(List<GTRecipeType.ICustomRecipeLogic> customRecipeLogicRunners) {
        this.customRecipeLogicRunners = customRecipeLogicRunners;
    }

    @Mutable
    @Final
    @Shadow(remap = false)
    private final List<GTRecipeType.ICustomRecipeLogic> customRecipeLogicRunners;

    @Shadow(remap = false)
    public GTRecipeLookup getLookup() {
        return null;
    }
}
