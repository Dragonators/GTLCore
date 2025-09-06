package org.gtlcore.gtlcore.api.machine;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

import static org.gtlcore.gtlcore.utils.Registries.getItemStack;

public class MEExtendedOutputFancyConfigurator implements IFancyUIProvider {

    protected final Supplier<Widget> widgetSupplier;

    public MEExtendedOutputFancyConfigurator(Supplier<Widget> widgetSupplier) {
        this.widgetSupplier = widgetSupplier;
    }

    @Override
    public Widget createMainPage(FancyMachineUIWidget fancyMachineUIWidget) {
        return widgetSupplier.get();
    }

    @Override
    public IGuiTexture getTabIcon() {
        return new ItemStackTexture(getItemStack("gtceu:echoite_vajra"));
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtlcore.gui.filter_config.title");
    }

    @Override
    public List<Component> getTabTooltips() {
        return List.of(Component.literal("更改物品/流体过滤设置与优先级"));
    }
}
