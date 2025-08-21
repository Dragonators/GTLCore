package org.gtlcore.gtlcore.mixin.gtm.api.recipe;

import org.gtlcore.gtlcore.api.recipe.IAdditionalRecipeIterator;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeIterator;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.Predicate;

import static org.gtlcore.gtlcore.api.recipe.IRecipeIterator.diveIngredientTreeFindRecipeCollection;

@Mixin(RecipeIterator.class)
public class RecipeIteratorMixin implements IAdditionalRecipeIterator {

    @Shadow(remap = false)
    int index;

    @Shadow(remap = false)
    List<List<AbstractMapIngredient>> ingredients;
    @Shadow(remap = false)
    @NotNull
    GTRecipeType recipeMap;

    @Shadow(remap = false)
    @NotNull
    Predicate<GTRecipe> canHandle;

    @Unique
    private List<GTRecipe> gtlcore$additionalRecipes = null;

    @Unique
    private Iterator<GTRecipe> gtlcore$presentIndexGTRecipesIterator = null;

    @Unique
    private Iterator<GTRecipe> gtlcore$additionalIterator = null;

    @Unique
    private boolean gtlcore$useOriginal = true;

    @Unique
    @Setter
    private boolean useDiveIngredientTreeFind = false;

    @Unique
    private GTRecipe gtlcore$singleRecipe = null;

    /**
     * @author Dragons
     * @reason 支持双重迭代器逻辑，优化单配方模式性能
     */
    @Overwrite(remap = false)
    public boolean hasNext() {
        if (gtlcore$useOriginal) {
            // 非dive模式：检查是否有单个配方缓存
            if (!useDiveIngredientTreeFind && gtlcore$singleRecipe != null) {
                return true;
            }

            // dive模式：如果当前迭代器还有元素，直接返回true
            if (useDiveIngredientTreeFind && gtlcore$presentIndexGTRecipesIterator != null && gtlcore$presentIndexGTRecipesIterator.hasNext()) {
                return true;
            }

            // 尝试查找下一个有效的ingredient集合或单个配方
            if (gtlcore$findNextRecipeSet()) {
                return true;
            }

            // 原始配方已遍历完，切换到额外配方
            gtlcore$useOriginal = false;
        }

        // 检查额外配方
        return gtlcore$additionalIterator != null && gtlcore$additionalIterator.hasNext();
    }

    /**
     * @author Dragons
     * @reason 使其可以找全每个HandlePart对应List<AbstractMapIngredient>中的所有配方，优化单配方模式性能
     *         不会返回null
     */
    @Overwrite(remap = false)
    @NotNull
    public GTRecipe next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (gtlcore$useOriginal) {
            // 非dive模式：优先使用单个配方缓存
            if (!useDiveIngredientTreeFind) {
                GTRecipe recipe = gtlcore$singleRecipe;
                gtlcore$singleRecipe = null; // 消费后设为null
                return recipe;
            }

            // dive模式：使用迭代器
            return gtlcore$presentIndexGTRecipesIterator.next();
        } else {
            return gtlcore$additionalIterator.next();
        }
    }

    /**
     * @author Dragons
     * @reason 重置所有状态
     */
    @Overwrite(remap = false)
    public void reset() {
        this.index = 0;
        this.gtlcore$singleRecipe = null;
        this.gtlcore$useOriginal = true;
        this.gtlcore$presentIndexGTRecipesIterator = null;

        if (gtlcore$additionalRecipes != null) {
            this.gtlcore$additionalIterator = gtlcore$additionalRecipes.iterator();
        } else {
            this.gtlcore$additionalIterator = null;
        }
    }

    @Override
    public void setAdditionalRecipes(@NotNull List<@NotNull GTRecipe> additionalRecipes) {
        if (!additionalRecipes.isEmpty()) {
            this.gtlcore$additionalRecipes = additionalRecipes;
            this.gtlcore$additionalIterator = additionalRecipes.iterator();
            this.gtlcore$useOriginal = true;
        }
    }

    @Override
    @Nullable
    public List<GTRecipe> getAdditionalRecipes() {
        return this.gtlcore$additionalRecipes;
    }

    @Unique
    private boolean gtlcore$findNextRecipeSet() {
        if (ingredients == null) return false;

        if (useDiveIngredientTreeFind) {
            // 使用diveIngredientTreeFind模式，收集所有配方
            ObjectOpenHashSet<GTRecipe> recipeSet = new ObjectOpenHashSet<>();
            while (this.index < this.ingredients.size()) {
                diveIngredientTreeFindRecipeCollection(ingredients.get(this.index),
                        recipeMap.getLookup().getLookup(),
                        canHandle, recipeSet);
                this.index++;

                recipeSet.remove(null); // 移除null元素
                if (!recipeSet.isEmpty()) {
                    gtlcore$presentIndexGTRecipesIterator = recipeSet.iterator();
                    return true;
                }
            }
        } else {
            // 非dive模式：直接缓存单个配方，不使用迭代器
            while (this.index < this.ingredients.size()) {
                GTRecipe recipe = this.recipeMap.getLookup().recurseIngredientTreeFindRecipe(
                        this.ingredients,
                        this.recipeMap.getLookup().getLookup(),
                        this.canHandle,
                        this.index,
                        0,
                        1L << this.index);

                this.index++;

                if (recipe != null) {
                    gtlcore$singleRecipe = recipe;
                    return true;
                }
            }
        }

        gtlcore$presentIndexGTRecipesIterator = null;
        return false;
    }
}
