package org.gtlcore.gtlcore.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IFilteredHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author Dragonators
 *
 *         ME样板总成配方处理接口
 *         支持基于slot的配方处理和内容管理
 *
 * @param <T> Ingredient/FluidIngredient
 */
public interface IMERecipeHandler<T extends Predicate<S>, S> extends IFilteredHandler<T> {

    /**
     * @return ItemCAP/FluidCAP
     */
    RecipeCapability<T> getCapability();

    // ==================== Active Slots Management ====================

    /**
     * 获取激活的slot列表
     * 意味着该slot拥有样板且样板包含CAP
     *
     * @param cap 配方能力类型
     * @return 激活的slot index[]
     */
    List<Integer> getActiveSlots(RecipeCapability<?> cap);

    // ==================== Content Management ====================

    /**
     * 获取所有激活且未缓存GTRecipe的slot -> 与handler对应内容的映射
     * 查询GTRecipe使用，所有Content缩限到单个元素与Int.MAX上限
     * Object类型说明:
     * - Ingredient -> ItemStack
     * - FluidIngredient -> FluidStack
     *
     * @return slot到限制内容列表的映射
     */
    Int2ObjectMap<List<Object>> getActiveAndUnCachedSlotsLimitContentsMap();

    /**
     * 获取指定slot中所有与handler对应内容 -> amount的映射
     * 计算并行使用，amount不缩限
     *
     * @param slots 要查询的slot列表
     * @return 内容到数量的映射
     */
    Object2LongMap<S> getCustomSlotsStackMap(List<Integer> slots);

    /**
     * 获取单个slot中所有与handler对应内容 -> amount的映射
     *
     * @param slot 要查询的slot
     * @return 内容到数量的映射
     */
    default Object2LongMap<S> getSingleSlotStackMap(int slot) {
        return getCustomSlotsStackMap(Collections.singletonList(slot));
    }

    default T copyContent(Object content) {
        return getCapability().copyInner((T) content);
    }

    // ==================== Recipe Handling ====================

    /**
     * 尝试使用指定slot处理配方
     * 注意: 在调用此方法前必须先调用initMEHandleContents为ME样板总成的Handler初始化待处理Map
     *
     * @param recipe   要处理的配方
     * @param simulate 是否模拟
     * @param trySlot  尝试使用的slot
     * @return 处理是否成功
     */
    default boolean meHandleRecipe(GTRecipe recipe, boolean simulate, int trySlot) {
        return meHandleRecipeInner(recipe, getPreparedMEHandleContents(), simulate, trySlot);
    }

    /**
     * ME配方处理的内部实现
     * 由具体实现类提供处理逻辑
     *
     * @param recipe           配方
     * @param preparedContents 准备好的内容映射
     * @param simulate         是否模拟
     * @param trySlot          尝试使用的slot
     * @return 处理是否成功
     */
    boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<T> preparedContents, boolean simulate, int trySlot);

    // ==================== Content Preparation ====================

    /**
     * 初始化ME处理内容
     * 将配方待处理内容List<?>转换为ME样板总成处理器内部处理的Map形式
     * 同时预处理催化剂/电路相关内容，在后续Map中只存在实际消耗的物品
     *
     * @param recipe       配方
     * @param leftContents 配方待处理内容
     * @param simulate     是否模拟
     */
    default void initMEHandleContents(GTRecipe recipe, List<?> leftContents, boolean simulate) {
        if (leftContents.isEmpty()) return;

        List<T> typedContents = new ObjectArrayList<>(leftContents.size());
        for (Object leftObj : leftContents) {
            typedContents.add(this.copyContent(leftObj));
        }

        prepareMEHandleContents(recipe, typedContents, simulate);
    }

    /**
     * 准备ME处理内容, 由具体的ME样板总成实现
     *
     * @param recipe   配方
     * @param contents 类型化的内容列表
     * @param simulate 是否模拟
     */
    void prepareMEHandleContents(GTRecipe recipe, List<T> contents, boolean simulate);

    /**
     * 获取准备好的ME处理内容
     *
     * @return 准备好的内容映射
     */
    Object2LongMap<T> getPreparedMEHandleContents();
}
