package org.gtlcore.gtlcore.api.recipe.ingredient;

import org.gtlcore.gtlcore.mixin.gtm.recipe.Ingredient.IntProviderIngredientAccessor;

import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;

import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class LongIngredient extends SizedIngredient {

    @Getter
    @Setter
    protected long actualAmount;
    private int hashCode = 0;

    protected LongIngredient(Ingredient inner, long actualAmount) {
        super(inner, Ints.saturatedCast(actualAmount));
        this.actualAmount = actualAmount;
    }

    protected LongIngredient(@NotNull TagKey<Item> tag, long actualAmount) {
        super(tag, Ints.saturatedCast(actualAmount));
        this.actualAmount = actualAmount;
    }

    protected LongIngredient(ItemStack itemStack, long actualAmount) {
        super(itemStack);
        this.actualAmount = actualAmount;
    }

    public static LongIngredient create(Ingredient inner, long amount) {
        return new LongIngredient(inner, amount);
    }

    public static LongIngredient create(Ingredient inner) {
        return new LongIngredient(inner, 1);
    }

    public static Ingredient copy(Ingredient ingredient) {
        if (ingredient instanceof LongIngredient longIngredient) {
            return LongIngredient.create(longIngredient.inner, longIngredient.actualAmount);
        } else if (ingredient instanceof SizedIngredient sizedIngredient) {
            if (sizedIngredient.getInner() instanceof IntProviderIngredient intProviderIngredient) {
                return copy(intProviderIngredient);
            }
            return LongIngredient.create(sizedIngredient.getInner(), sizedIngredient.getAmount());
        } else if (ingredient instanceof IntCircuitIngredient circuit) {
            return circuit.copy();
        } else if (ingredient instanceof IntProviderIngredient intProviderIngredient) {
            IntProviderIngredient copied = new IntProviderIngredient(intProviderIngredient.getInner(), intProviderIngredient.getCountProvider());
            final var accessor = (IntProviderIngredientAccessor) intProviderIngredient;
            if (accessor.getItemStack() != null) {
                copied.setItemStacks(Arrays.stream(accessor.getItemStack()).map(ItemStack::copy)
                        .toArray(ItemStack[]::new));
            }
            if (accessor.getSampledCount() != -1) {
                copied.setSampledCount(accessor.getSampledCount());
            }
            return copied;
        } else {
            return create(ingredient);
        }
    }

    @Override
    @NotNull
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return SERIALIZER;
    }

    public static SizedIngredient fromJson(JsonObject json) {
        return SERIALIZER.parse(json);
    }

    @Override
    public ItemStack @NotNull [] getItems() {
        if (getInner() instanceof IntProviderIngredient intProviderIngredient) {
            return intProviderIngredient.getItems();
        }
        if (itemStacks == null) {
            var items = new ObjectArrayList<ItemStack>(inner.getItems().length);
            for (ItemStack item : this.inner.getItems()) {
                items.add(item.copyWithCount(amount));
            }
            itemStacks = items.toArray(new ItemStack[0]);
        }
        return itemStacks;
    }

    @Override
    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = Objects.hash(this.actualAmount, Objects.hashCode(this.inner));
        }
        return this.hashCode;
    }

    public static final IIngredientSerializer<LongIngredient> SERIALIZER = new IIngredientSerializer<>() {

        @Override
        public @NotNull LongIngredient parse(FriendlyByteBuf buffer) {
            long amount = buffer.readVarLong();
            return new LongIngredient(Ingredient.fromNetwork(buffer), amount);
        }

        @Override
        public @NotNull LongIngredient parse(JsonObject json) {
            long amount = json.get("count").getAsLong();
            Ingredient inner = Ingredient.fromJson(json.get("ingredient"));
            return new LongIngredient(inner, amount);
        }

        @Override
        public void write(FriendlyByteBuf buffer, LongIngredient ingredient) {
            buffer.writeVarLong(ingredient.getActualAmount());
            ingredient.inner.toNetwork(buffer);
        }
    };
}
