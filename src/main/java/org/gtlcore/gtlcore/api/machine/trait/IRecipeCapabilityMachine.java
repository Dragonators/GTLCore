package org.gtlcore.gtlcore.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 部分代码参考自GTO
 * &#064;line <a href="https://github.com/GregTech-Odyssey/GTOCore">...</a>
 */

public interface IRecipeCapabilityMachine {

    @NotNull
    Map<IO, List<RecipeHandlePart>> getCapabilities();

    @NotNull
    Map<IO, Map<RecipeCapability<?>, List<IRecipeHandler<?>>>> getCapabilitiesFlat();

    @NotNull
    default List<IRecipeHandler<?>> getCapabilitiesFlat(IO io, RecipeCapability<?> cap) {
        return getCapabilitiesFlat()
                .getOrDefault(io, Collections.emptyMap())
                .getOrDefault(cap, Collections.emptyList());
    }

    default void addHandlerList(RecipeHandlePart handler) {
        if (handler == RecipeHandlePart.NO_DATA) return;
        IO io = handler.getHandlerIO();
        getCapabilities().computeIfAbsent(io, i -> new ArrayList<>()).add(handler);
        var entrySet = handler.getHandlerMap().entrySet();
        var inner = getCapabilitiesFlat().computeIfAbsent(io, i -> new Reference2ObjectOpenHashMap<>(entrySet.size()));
        for (var entry : entrySet) {
            var entryList = entry.getValue();
            inner.computeIfAbsent(entry.getKey(), c -> new ArrayList<>(entryList.size())).addAll(entryList);
        }
    }

    // region ME

    List<MERecipeHandlePart> getMERecipeHandleParts();

    List<MERecipeHandlePart> getMERecipeOutputHandleParts();

    void setMERecipeHandleMap(MERecipeHandlePart hatch, GTRecipe recipe, int slot);

    // endregion

    List<RecipeHandlePart> getRecipeHandleParts();

    Map<GTRecipe, IRecipeHandlePart> getRecipeHandleMap();

    void setRecipeHandleMap(RecipeHandlePart hatch, GTRecipe recipe);

    void upDate();

    boolean isDistinct();

    void setDistinct(boolean isDistinct);

    default boolean isMEOutPutBus() {
        return false;
    }

    default boolean isMEOutPutHatch() {
        return false;
    }

    default boolean isMEOutPutDual() {
        return false;
    }

    default boolean isRecipeOutput(GTRecipe recipe) {
        return false;
    }

    default IParallelHatch getParallelHatch() {
        return null;
    }

    static void attachConfigurators(ConfiguratorPanel configuratorPanel, WorkableElectricMultiblockMachine machine) {
        if (machine instanceof IRecipeCapabilityMachine distinctMachine) {
            configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                    GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture(0, 0.5, 1, 0.5),
                    GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture(0, 0, 1, 0.5),
                    distinctMachine::isDistinct, (clickData, pressed) -> {
                        distinctMachine.setDistinct(pressed);
                        distinctMachine.upDate();
                    })
                    .setTooltipsSupplier(pressed -> List.of(Component.translatable("gtceu.multiblock.universal.distinct.all").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                            .append(Component.translatable(pressed ? "gtceu.multiblock.universal.distinct.yes" : "gtceu.multiblock.universal.distinct.no")))));
        }
    }
}
