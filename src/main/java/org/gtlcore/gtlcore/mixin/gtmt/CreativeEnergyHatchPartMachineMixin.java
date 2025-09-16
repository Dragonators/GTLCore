package org.gtlcore.gtlcore.mixin.gtmt;

import org.gtlcore.gtlcore.integration.gtmt.InfinityEnergyContainer;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;

import net.minecraft.world.entity.player.Player;

import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.Arrays;

@Mixin(CreativeEnergyHatchPartMachine.class)
public abstract class CreativeEnergyHatchPartMachineMixin extends TieredIOPartMachine {

    @Shadow(remap = false)
    @Final
    public NotifiableEnergyContainer energyContainer;

    @Shadow(remap = false)
    private long voltage;

    @Shadow(remap = false)
    private Long maxEnergy;

    @Shadow(remap = false)
    private int setTier;

    @Shadow(remap = false)
    private int amps;

    @Unique
    protected TickableSubscription gTLCore$energySubs;
    @Nullable
    @Unique
    protected ISubscription gTLCore$energyListener;

    public CreativeEnergyHatchPartMachineMixin(IMachineBlockEntity holder, int tier, IO io) {
        super(holder, tier, io);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        gTLCore$energyListener = energyContainer.addChangedListener(this::gTLCore$InfinityEnergySubscription);
        gTLCore$InfinityEnergySubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (gTLCore$energyListener != null) {
            gTLCore$energyListener.unsubscribe();
            gTLCore$energyListener = null;
        }
    }

    /**
     * @author liansishen
     * @reason 修复创造激光
     */
    @Overwrite(remap = false)
    protected NotifiableEnergyContainer createEnergyContainer() {
        NotifiableEnergyContainer container;
        this.voltage = GTValues.VEX[setTier];
        this.maxEnergy = this.voltage * 16L * this.amps;
        container = new InfinityEnergyContainer(this, this.maxEnergy, this.voltage, this.amps, 0L, 0L);
        return container;
    }

    /**
     * @author liansishen
     * @reason 修复创造激光
     */
    @Overwrite(remap = false)
    protected void addEnergy() {
        if (energyContainer.getEnergyStored() < this.maxEnergy) {
            energyContainer.setEnergyStored(this.maxEnergy);
        }
    }

    /**
     * @author liansishen
     * @reason 修复创造激光
     */
    @Overwrite(remap = false)
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(176, 136, this, entityPlayer)
                .background(GuiTextures.BACKGROUND)
                .widget(new LabelWidget(7, 32, "gtceu.creative.energy.voltage"))
                .widget(new TextFieldWidget(9, 47, 152, 16, () -> String.valueOf(voltage),
                        value -> {
                            gTLCore$setVoltage(Long.parseLong(value));
                            setTier = GTUtil.getTierByVoltage(this.voltage);
                        }).setNumbersOnly(8L, Long.MAX_VALUE))
                .widget(new LabelWidget(7, 74, "gtceu.creative.energy.amperage"))
                .widget(new ButtonWidget(7, 87, 20, 20,
                        new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("-")),
                        cd -> gTLCore$setAmps(--amps == -1 ? 0 : amps)))
                .widget(new TextFieldWidget(31, 89, 114, 16, () -> String.valueOf(amps),
                        value -> gTLCore$setAmps(Integer.parseInt(value))).setNumbersOnly(1, 67108864))
                .widget(new ButtonWidget(149, 87, 20, 20,
                        new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("+")),
                        cd -> {
                            if (amps < Integer.MAX_VALUE) {
                                gTLCore$setAmps(++amps);
                            }
                        }))

                .widget(new SelectorWidget(7, 7, 50, 20, Arrays.stream(GTValues.VNF).toList(), -1)
                        .setOnChanged(tier -> {
                            setTier = ArrayUtils.indexOf(GTValues.VNF, tier);
                            gTLCore$setVoltage(GTValues.VEX[setTier]);
                        })
                        .setSupplier(() -> GTValues.VNF[setTier])
                        .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                        .setBackground(ColorPattern.BLACK.rectTexture())
                        .setValue(GTValues.VNF[setTier]));
    }

    @Unique
    private void gTLCore$setVoltage(long voltage) {
        this.voltage = voltage;
        this.maxEnergy = this.voltage * 16L * this.amps;
        gTLCore$updateEnergyContainer();
    }

    @Unique
    private void gTLCore$setAmps(int amps) {
        this.amps = amps;
        this.maxEnergy = this.voltage * 16L * this.amps;
        gTLCore$updateEnergyContainer();
    }

    @Unique
    private void gTLCore$updateEnergyContainer() {
        this.energyContainer.resetBasicInfo(this.maxEnergy, this.voltage, this.amps, 0, 0);
        this.energyContainer.setEnergyStored(this.maxEnergy);
        if (!this.getControllers().isEmpty()) this.getControllers().get(0).onPartUnload();
    }

    @Unique
    protected void gTLCore$InfinityEnergySubscription() {
        gTLCore$energySubs = subscribeServerTick(gTLCore$energySubs, this::addEnergy);
    }
}
