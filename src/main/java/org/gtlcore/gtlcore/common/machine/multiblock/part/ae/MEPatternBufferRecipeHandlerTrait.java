package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;
import org.gtlcore.gtlcore.integration.ae2.AEUtils;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MEPatternBufferRecipeHandlerTrait extends MachineTrait {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEPatternBufferPartMachine.class);

    @Getter
    protected final MEItemInputHandler meItemHandler;

    @Getter
    protected final MEFluidHandler meFluidHandler;

    private final MEPatternBufferPartMachine.PendingRefundData pendingRefundData;

    public MEPatternBufferRecipeHandlerTrait(MEPatternBufferPartMachine ioBuffer, MEPatternBufferPartMachine.PendingRefundData pendingRefundData, IO io) {
        super(ioBuffer);
        meItemHandler = new MEItemInputHandler(ioBuffer, io);
        meFluidHandler = new MEFluidHandler(ioBuffer, io);
        this.pendingRefundData = pendingRefundData;
    }

    @Override
    public MEPatternBufferPartMachine getMachine() {
        return (MEPatternBufferPartMachine) super.getMachine();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {}

    public List<IMERecipeHandlerTrait<? extends Predicate<?>, ?>> getMERecipeHandlers() {
        return List.of(meItemHandler, meFluidHandler);
    }

    public Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<? extends Predicate<?>, ?>> getMERecipeHandlerMap() {
        Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<? extends Predicate<?>, ?>> map = new Reference2ObjectArrayMap<>();
        map.put(ItemRecipeCapability.CAP, meItemHandler);
        map.put(FluidRecipeCapability.CAP, meFluidHandler);
        return map;
    }

    private boolean handleItemInner(GTRecipe recipe, Object2LongMap<Ingredient> left, int circuit, boolean simulate, int trySlot) {
        var internalSlot = getMachine().getInternalInventory()[trySlot];
        if (internalSlot.isActive(ItemRecipeCapability.CAP)) {
            if (simulate) {
                if (!internalSlot.testCatalystItemInternal(recipe)) return false;
            }
            return internalSlot.handleItemInternal(left, circuit, simulate);
        } else return false;
    }

    private boolean handleFluidInner(GTRecipe recipe, Object2LongMap<FluidIngredient> left, boolean simulate, int trySlot) {
        var internalSlot = getMachine().getInternalInventory()[trySlot];
        if (internalSlot.isActive(FluidRecipeCapability.CAP)) {
            if (simulate) {
                if (!internalSlot.testCatalystFluidInternal(recipe)) return false;
            }
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
                .filter(i -> slots[i].isActive(recipeCapability) && !machine.recipeCacheMap.containsKey(i))
                .boxed()
                .collect(Collectors.toList());
    }

    public class MEItemInputHandler extends NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> {

        @Getter
        private final IO io;

        @Getter
        @Setter
        private Object2LongMap<Ingredient> preparedMEHandleContents = new Object2LongOpenHashMap<>();

        @Setter
        private int preparedCircuitConfig = -1;

        public MEItemInputHandler(MEPatternBufferPartMachine machine, IO io) {
            super(machine);
            this.io = io;
        }

        public MEPatternBufferPartMachine getMachine() {
            return (MEPatternBufferPartMachine) this.machine;
        }

        @Override
        public RecipeCapability<Ingredient> getCapability() {
            return ItemRecipeCapability.CAP;
        }

        @Override
        public List<Integer> getActiveSlots() {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveSlots(getMachine().getInternalInventory(), ItemRecipeCapability.CAP);
        }

        private List<Integer> getActiveAndUnCachedSlots() {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveAndUnCachedSlots(getMachine().getInternalInventory(), ItemRecipeCapability.CAP);
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveAndUnCachedSlotsLimitContentsMap() {
            var map = new Int2ObjectArrayMap<List<Object>>();
            var machine = getMachine();
            var shared = machine.getSharedCatalystInventory().getContents();
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
            return handleItemInner(recipe, left, preparedCircuitConfig, simulate, trySlot);
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<Ingredient> left, boolean simulate) {
            if (simulate) {
                // 处理总成配置的电路
                getMachine().getSharedCircuitInventory().handleRecipeInner(IO.IN, recipe, left, null, true);

                // 处理共享库存
                getMachine().getSharedCatalystInventory().handleRecipeInner(IO.IN, recipe, left, null, true);

                // simulate时left会包含Circuit
                setPreparedMEHandleContents(AEUtils.ingredientsMapWithOutCircuit(left, this::setPreparedCircuitConfig));
            } else {
                setPreparedMEHandleContents(AEUtils.ingredientsMap(left));
            }
        }

        @Override
        public List<Ingredient> meHandleRecipeOutputInner(List<Ingredient> left, boolean simulate) {
            if (simulate) return List.of();
            for (Ingredient ingredient : left) {
                if (ingredient instanceof IntProviderIngredient intProvider) {
                    intProvider.setItemStacks(null);
                    intProvider.setSampledCount(null);
                }

                ItemStack[] items = ingredient.getItems();
                if (items.length != 0) {
                    ItemStack output = items[0];
                    if (!output.isEmpty()) {
                        pendingRefundData.addTo(AEItemKey.of(output), output.getCount());
                    }
                }
            }
            return List.of();
        }
    }

    public class MEFluidHandler extends NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> {

        @Getter
        private final IO io;

        @Getter
        @Setter
        private Object2LongMap<FluidIngredient> preparedMEHandleContents = new Object2LongOpenHashMap<>();

        public MEFluidHandler(MEPatternBufferPartMachine machine, IO io) {
            super(machine);
            this.io = io;
        }

        public MEPatternBufferPartMachine getMachine() {
            return (MEPatternBufferPartMachine) this.machine;
        }

        @Override
        public RecipeCapability<FluidIngredient> getCapability() {
            return FluidRecipeCapability.CAP;
        }

        @Override
        public List<Integer> getActiveSlots() {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveSlots(getMachine().getInternalInventory(), FluidRecipeCapability.CAP);
        }

        private List<Integer> getActiveAndUnCachedSlots() {
            return MEPatternBufferRecipeHandlerTrait.this.getActiveAndUnCachedSlots(getMachine().getInternalInventory(), FluidRecipeCapability.CAP);
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveAndUnCachedSlotsLimitContentsMap() {
            var map = new Int2ObjectArrayMap<List<Object>>();
            var machine = getMachine();
            var shared = machine.getSharedCatalystTank().getContents();
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
            return handleFluidInner(recipe, left, simulate, trySlot);
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<FluidIngredient> left, boolean simulate) {
            if (simulate) {
                getMachine().getSharedCatalystTank().handleRecipeInner(IO.IN, recipe, left, null, true);
            }
            setPreparedMEHandleContents(AEUtils.fluidIngredientsMap(left));
        }

        @Override
        public List<FluidIngredient> meHandleRecipeOutputInner(List<FluidIngredient> left, boolean simulate) {
            if (simulate) return List.of();;
            for (FluidIngredient fluidIngredient : left) {
                if (!fluidIngredient.isEmpty()) {
                    FluidStack[] fluids = fluidIngredient.getStacks();
                    if (fluids.length != 0) {
                        FluidStack output = fluids[0];
                        pendingRefundData.addTo(AEFluidKey.of(output.getFluid()), output.getAmount());
                    }
                }
            }
            return List.of();
        }
    }
}
