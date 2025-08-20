package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MEPatternBufferRecipeHandlerTrait extends MachineTrait {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEPatternBufferPartMachine.class);
    protected List<Runnable> listeners = new ArrayList<>();

    @Getter
    protected final MEItemInputHandler itemInputHandler;

    @Getter
    protected final MEFluidInputHandler fluidInputHandler;

    public MEPatternBufferRecipeHandlerTrait(MEPatternBufferPartMachine ioBuffer) {
        super(ioBuffer);
        itemInputHandler = new MEItemInputHandler(ioBuffer);
        fluidInputHandler = new MEFluidInputHandler(ioBuffer);
    }

    @Override
    public MEPatternBufferPartMachine getMachine() {
        return (MEPatternBufferPartMachine) super.getMachine();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public void onChanged() {
        listeners.forEach(Runnable::run);
    }

    public List<IMERecipeHandlerTrait<? extends Predicate<?>, ?>> getMERecipeHandlers() {
        return List.of(itemInputHandler, fluidInputHandler);
    }

    public Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<? extends Predicate<?>, ?>> getMERecipeHandlerMap() {
        Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<? extends Predicate<?>, ?>> map = new Reference2ObjectArrayMap<>();
        map.put(ItemRecipeCapability.CAP, itemInputHandler);
        map.put(FluidRecipeCapability.CAP, fluidInputHandler);
        return map;
    }

    public boolean handleItemInner(Object2LongMap<Ingredient> left, boolean simulate, int trySlot) {
        var internalSlot = getMachine().getInternalInventory()[trySlot];
        if (internalSlot.isActive(ItemRecipeCapability.CAP)) {
            return internalSlot.handleItemInternal(left, simulate);
        } else return false;
    }

    public boolean handleFluidInner(Object2LongMap<FluidIngredient> left, boolean simulate, int trySlot) {
        var internalSlot = getMachine().getInternalInventory()[trySlot];
        if (internalSlot.isActive(FluidRecipeCapability.CAP)) {
            return internalSlot.handleFluidInternal(left, simulate);
        } else return false;
    }

    private List<Integer> getActiveSlots(MEPatternBufferPartMachine.InternalSlot[] slots, RecipeCapability<?> recipeCapability) {
        return IntStream.range(0, slots.length)
                .filter(i -> slots[i].isActive(recipeCapability))
                .boxed()
                .collect(Collectors.toList());
    }

    private List<Integer> getActiveAndUnCachedSlots(MEPatternBufferPartMachine.InternalSlot[] slots, RecipeCapability<?> recipeCapability) {
        var machine = getMachine();
        return IntStream.range(0, slots.length)
                .filter(i -> slots[i].isActive(recipeCapability) && !machine.gtRecipeCacheMap.containsKey(i))
                .boxed()
                .collect(Collectors.toList());
    }

    public class MEItemInputHandler extends NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> {

        @Getter
        @Setter
        private Object2LongMap<Ingredient> preparedMEHandleContents = new Object2LongOpenHashMap<>();

        public MEItemInputHandler(MEPatternBufferPartMachine machine) {
            super(machine);
        }

        public MEPatternBufferPartMachine getMachine() {
            return (MEPatternBufferPartMachine) this.machine;
        }

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public RecipeCapability<Ingredient> getCapability() {
            return ItemRecipeCapability.CAP;
        }

        @Override
        public Ingredient copyContent(Object content) {
            return super.copyContent(content);
        }

        @Override
        public List<Integer> getActiveSlots(RecipeCapability<?> recipeCapability) {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveSlots(getMachine().getInternalInventory(), recipeCapability);
        }

        private List<Integer> getActiveAndUnCachedSlots() {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveAndUnCachedSlots(getMachine().getInternalInventory(), ItemRecipeCapability.CAP);
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveAndUnCachedSlotsLimitContentsMap() {
            var map = new Int2ObjectArrayMap<List<Object>>();
            var machine = getMachine();
            var shared = machine.getShareInventory().getContents();
            for (int slot : getActiveAndUnCachedSlots()) {
                var inputs = machine.getInternalInventory()[slot].getLimitItemStackInput();

                // 通过统一方法获取该槽位的电路（可能来自样板或总成配置）
                ItemStack circuitForRecipe = machine.getCircuitForRecipe(slot);
                if (!circuitForRecipe.isEmpty()) {
                    inputs.add(circuitForRecipe);
                }

                inputs.addAll(shared);
                map.put(slot, inputs);
            }
            return map;
        }

        @Override
        public Object2LongMap<ItemStack> getCustomSlotsStackMap(List<Integer> list) {
            Object2LongOpenHashMap<ItemStack> map = new Object2LongOpenHashMap<>();
            for (int i : list) {
                var slot = getMachine().getInternalInventory()[i];
                for (var it = Object2LongMaps.fastIterator(slot.getItemStackInputMap()); it.hasNext();) {
                    var entry = it.next();
                    map.addTo(entry.getKey(), entry.getLongValue());
                }
            }
            return map;
        }

        @Override
        public boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<Ingredient> left, boolean simulate, int trySlot) {
            return handleItemInner(left, simulate, trySlot);
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<Ingredient> left, boolean simulate) {
            // 处理总成配置的电路
            getMachine().getMePatternCircuitInventory().handleRecipeInner(IO.IN, recipe, left, null, simulate);

            // 处理共享库存
            getMachine().getShareInventory().handleRecipeInner(IO.IN, recipe, left, null, simulate);

            // 电路需要每个Slot单独处理
            setPreparedMEHandleContents(ingredientsToAEKeyMap(left));
        }
    }

    public class MEFluidInputHandler extends NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> {

        @Getter
        @Setter
        private Object2LongMap<FluidIngredient> preparedMEHandleContents = new Object2LongOpenHashMap<>();

        public MEFluidInputHandler(MEPatternBufferPartMachine machine) {
            super(machine);
        }

        public MEPatternBufferPartMachine getMachine() {
            return (MEPatternBufferPartMachine) this.machine;
        }

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public RecipeCapability<FluidIngredient> getCapability() {
            return FluidRecipeCapability.CAP;
        }

        @Override
        public FluidIngredient copyContent(Object content) {
            return super.copyContent(content);
        }

        @Override
        public List<Integer> getActiveSlots(RecipeCapability<?> recipeCapability) {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveSlots(getMachine().getInternalInventory(), recipeCapability);
        }

        private List<Integer> getActiveAndUnCachedSlots() {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveAndUnCachedSlots(getMachine().getInternalInventory(), FluidRecipeCapability.CAP);
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveAndUnCachedSlotsLimitContentsMap() {
            var map = new Int2ObjectArrayMap<List<Object>>();
            var machine = getMachine();
            var shared = machine.getShareTank().getContents();
            for (int slot : getActiveAndUnCachedSlots()) {
                var inputs = machine.getInternalInventory()[slot].getLimitFluidStackInput();
                inputs.addAll(shared);
                map.put(slot, inputs);
            }
            return map;
        }

        @Override
        public Object2LongMap<FluidStack> getCustomSlotsStackMap(List<Integer> list) {
            Object2LongOpenHashMap<FluidStack> map = new Object2LongOpenHashMap<>();
            for (int i : list) {
                var slot = getMachine().getInternalInventory()[i];
                for (var it = Object2LongMaps.fastIterator(slot.getFluidStackInputMap()); it.hasNext();) {
                    var entry = it.next();
                    map.addTo(entry.getKey(), entry.getLongValue());
                }
            }
            return map;
        }

        @Override
        public boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<FluidIngredient> left, boolean simulate, int trySlot) {
            return handleFluidInner(left, simulate, trySlot);
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<FluidIngredient> left, boolean simulate) {
            getMachine().getShareTank().handleRecipeInner(IO.IN, recipe, left, null, simulate);
            setPreparedMEHandleContents(fluidIngredientsToAEKeyMap(left));
        }
    }

    // Utility Methods
    public static Pair<Object2LongOpenHashMap<Item>, Object2LongOpenHashMap<Fluid>> mergeInternalSlot(MEPatternBufferPartMachine.InternalSlot[] internalSlots) {
        Object2LongOpenHashMap<Item> items = new Object2LongOpenHashMap<>();
        Object2LongOpenHashMap<Fluid> fluids = new Object2LongOpenHashMap<>();
        for (var internalSlot : Arrays.stream(internalSlots).filter(MEPatternBufferPartMachine.InternalSlot::isActive).toList()) {
            for (var it = internalSlot.getItemInventory().object2LongEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                items.addTo(entry.getKey().getItem(), entry.getLongValue());
            }
            for (var it = internalSlot.getFluidInventory().object2LongEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                fluids.addTo(entry.getKey().getFluid(), entry.getLongValue());
            }
        }
        return new ImmutablePair<>(items, fluids);
    }

    private static Object2LongMap<Ingredient> ingredientsToAEKeyMap(List<Ingredient> ingredients) {
        var result = new Object2LongOpenHashMap<Ingredient>();
        for (Ingredient ingredient : ingredients) {
            var items = ingredient.getItems();
            if (items.length == 0 || items[0].isEmpty()) {
                continue;
            }
            if (GTItems.INTEGRATED_CIRCUIT.is(items[0].getItem())) {
                result.addTo(ingredient, 1);
                continue;
            }
            result.addTo(ingredient, items[0].getCount());
        }
        return result;
    }

    private static Object2LongMap<FluidIngredient> fluidIngredientsToAEKeyMap(List<FluidIngredient> ingredients) {
        var result = new Object2LongOpenHashMap<FluidIngredient>(ingredients.size());
        for (FluidIngredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            result.addTo(ingredient, ingredient.getAmount());
        }
        return result;
    }
}
