package org.gtlcore.gtlcore.common.machine.multiblock.part.ae;

import org.gtlcore.gtlcore.api.machine.trait.IMEPatternPartMachine;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.trait.GridNodeHolder;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;

import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.helpers.patternprovider.PatternContainer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public abstract class MEPatternIOPartMachine extends MultiblockPartMachine implements ICraftingProvider, PatternContainer, IMachineLife, IGridConnectedMachine, IMEPatternPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MEPatternIOPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    private final boolean canHandleOutput;

    @Persisted
    @Getter
    protected final GridNodeHolder nodeHolder;
    @Getter
    @Setter
    @DescSynced
    protected boolean isOnline;
    protected final IActionSource actionSource;

    public MEPatternIOPartMachine(IMachineBlockEntity holder, boolean canHandleOutput) {
        super(holder);
        this.canHandleOutput = canHandleOutput;
        this.nodeHolder = createNodeHolder();
        this.actionSource = IActionSource.ofMachine(nodeHolder.getMainNode()::getNode);
    }

    @Nullable
    public IFancyUIProvider.@Nullable PageGroupingData getPageGroupingData() {
        return canHandleOutput ?
                new IFancyUIProvider.PageGroupingData("gtceu.multiblock.page_switcher.io.both", 3) :
                new IFancyUIProvider.PageGroupingData("gtceu.multiblock.page_switcher.io.import", 1);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedGridNode getMainNode() {
        return nodeHolder.getMainNode();
    }

    protected GridNodeHolder createNodeHolder() {
        return new GridNodeHolder(this);
    }

    @Override
    public void onRotated(@NotNull Direction oldFacing, @NotNull Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        this.getMainNode().setExposedOnSides(EnumSet.of(newFacing));
    }

    @Override
    public boolean canHandleOutput() {
        return canHandleOutput;
    }
}
