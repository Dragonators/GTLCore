package org.gtlcore.gtlcore.mixin.ae2.integration;

import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jei.ItemIngredientConverter;
import com.tterrag.registrate.util.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Objects;

import static com.gregtechceu.gtceu.common.data.GTItems.*;

@Mixin(ItemIngredientConverter.class)
public class ItemIngredientConverterMixin {

    @Inject(method = "getStackFromIngredient(Lnet/minecraft/world/item/ItemStack;)Lappeng/api/stacks/GenericStack;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    public void getStackFromIngredient(ItemStack itemStack, CallbackInfoReturnable<GenericStack> cir) {
        if (!itemStack.isEmpty()) {
            var item = itemStack.getItem();
            boolean b1 = Arrays.stream(SHAPE_MOLDS).map(RegistryEntry::get).anyMatch((i) -> i.equals(item));
            boolean b2 = Arrays.stream(SHAPE_EXTRUDERS).filter(Objects::nonNull).map(RegistryEntry::get).anyMatch((i) -> i.equals(item));
            boolean b3 = TOOL_DATA_STICK.is(item) || TOOL_DATA_ORB.is(item) || TOOL_DATA_MODULE.is(item);
            if (b1 || b2 || b3) cir.setReturnValue(null);
        }
    }
}
