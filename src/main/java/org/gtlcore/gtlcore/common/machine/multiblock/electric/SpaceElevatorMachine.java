package org.gtlcore.gtlcore.common.machine.multiblock.electric;

import org.gtlcore.gtlcore.api.machine.multiblock.ISpaceElevatorModule;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import earth.terrarium.adastra.common.menus.base.PlanetsMenuProvider;
import earth.terrarium.botarium.common.menu.MenuHooks;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SpaceElevatorMachine extends TierCasingMachine implements IMachineLife {

    public SpaceElevatorMachine(IMachineBlockEntity holder) {
        super(holder, "SEPMTier");
    }

    private final Set<ISpaceElevatorModule> proxyModules = new ReferenceOpenHashSet<>();

    private int mam = 0;

    public void addModule(ISpaceElevatorModule module) {
        this.proxyModules.add(module);
    }

    public void removeModule(ISpaceElevatorModule module) {
        this.proxyModules.remove(module);
    }

    protected @NotNull Set<ISpaceElevatorModule> getModules() {
        return Collections.unmodifiableSet(this.proxyModules);
    }

    protected void safeClearModules() {
        for (ISpaceElevatorModule module : this.getModules()) {
            module.removeFromElevator(this);
        }
        this.proxyModules.clear();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        safeClearModules();
    }

    @Override
    public void onMachineRemoved() {
        safeClearModules();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        safeClearModules();
        scanForModules();
    }

    private void scanForModules() {
        final Level level = getLevel();
        final BlockPos powerCore = getPowerCore(getPos(), level);
        if (powerCore != null) {
            BlockPos[] modulePositions = new BlockPos[] {
                    powerCore.offset(8, 2, 3),
                    powerCore.offset(8, 2, -3),
                    powerCore.offset(-8, 2, 3),
                    powerCore.offset(-8, 2, -3),
                    powerCore.offset(3, 2, 8),
                    powerCore.offset(-3, 2, 8),
                    powerCore.offset(3, 2, -8),
                    powerCore.offset(-3, 2, -8)
            };

            for (BlockPos pos : modulePositions) {
                MetaMachine machine = MetaMachine.getMachine(Objects.requireNonNull(level), pos);
                if (machine instanceof ISpaceElevatorModule module && module.isFormed()) {
                    module.connectToElevator(this);
                }
            }
        }
    }

    private BlockPos getPowerCore(BlockPos pos, Level level) {
        BlockPos[] coordinates = new BlockPos[] { pos.offset(3, -2, 0),
                pos.offset(-3, -2, 0),
                pos.offset(0, -2, 3),
                pos.offset(0, -2, -3) };
        for (BlockPos blockPos : coordinates) {
            if (Objects.equals(level.kjs$getBlock(blockPos).getId(), "gtlcore:power_core")) {
                return blockPos;
            }
        }
        return null;
    }

    private int getMAM() {
        if (getOffsetTimer() % 20 == 0) {
            mam = (int) proxyModules.stream()
                    .filter(ISpaceElevatorModule::isFormed)
                    .count();
        }
        return mam;
    }

    @Override
    public boolean onWorking() {
        boolean value = super.onWorking();
        if (getOffsetTimer() % 20 == 0) {
            if (getRecipeLogic().getProgress() > 240) {
                RecipeResult.of(this, RecipeResult.SUCCESS);
                getRecipeLogic().setProgress(120);
            }
        }
        return value;
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!this.isFormed) return;
        textList.add(Component.translatable("gtceu.machine.module", getMAM()));
        textList.add(
                ComponentPanelWidget.withButton(Component.translatable("gtceu.machine.space_elevator.set_out"),
                        "set_out"));
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        if (componentData.equals("set_out") && getRecipeLogic().isWorking()) {
            final BlockPos pos = getPos();
            List<ServerPlayer> entities = Objects.requireNonNull(getLevel()).getEntitiesOfClass(ServerPlayer.class, new AABB(pos.getX() - 2,
                    pos.getY() - 2,
                    pos.getZ() - 2,
                    pos.getX() + 2,
                    pos.getY() + 2,
                    pos.getZ() + 2));
            for (ServerPlayer pr : entities) {
                if (pr != null) {
                    pr.addTag("spaceelevatorst");
                    MenuHooks.openMenu(pr, new PlanetsMenuProvider());
                }
            }
        }
    }
}
