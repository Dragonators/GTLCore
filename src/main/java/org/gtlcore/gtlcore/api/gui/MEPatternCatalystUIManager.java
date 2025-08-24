package org.gtlcore.gtlcore.api.gui;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.FluidTransferList;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class MEPatternCatalystUIManager {

    static final int SLOT_SIZE = 18;
    static final int PAD_OUT = 8;
    static final int PAD_IN = 4;
    static final int MAX_COLS = 9;
    private static final int[] OPTIMAL_COLUMNS = new int[MAX_COLS + 1];

    static {
        for (int slots = 1; slots <= MAX_COLS; slots++) {
            OPTIMAL_COLUMNS[slots] = calculateOptimalColumnsInternal(slots);
        }
    }

    private int lastIndex = -1;

    /** 右侧停靠容器（外层 UI 需要将其 add 到主界面上） */
    @Getter
    private final WidgetGroup dockRoot;

    private final IItemTransfer[] itemTransfers;

    private final FluidTransferList[] fluidTankTransfers;

    public MEPatternCatalystUIManager(int dockX, IItemTransfer[] itemTransfers, FluidTransferList[] fluidTankTransfers) {
        // 初始高度给个最小值，后面会根据内容 resize
        this.dockRoot = new WidgetGroup(dockX, 0, 16, 16);
        this.dockRoot.setVisible(false).setActive(false);
        this.itemTransfers = itemTransfers;
        this.fluidTankTransfers = fluidTankTransfers;
    }

    /**
     * 为指定 pattern 槽位切换显示/隐藏：
     * - 如果点击同一slot执行开关
     * - 否则即时创建新的催化剂 UI 并显示它。
     */
    public void toggleFor(int index) {
        if (dockRoot == null) return;

        if (lastIndex == index) {
            dockRoot.setVisible(!dockRoot.isVisible()).setActive(!dockRoot.isActive());
            return;
        }
        show(index, itemTransfers[index], fluidTankTransfers[index]);
    }

    private void show(int index, IItemTransfer itemInventory, FluidTransferList tanks) {
        dockRoot.clearAllWidgets();

        final int itemSlots = (itemInventory != null) ? itemInventory.getSlots() : 0;
        final int fluidTanks = (tanks != null) ? tanks.transfers.length : 0;

        int currentY = 0;
        int maxWidth = 0;

        if (itemSlots > 0) {
            Widget itemContainer = createInventoryContainer(itemInventory, itemSlots);
            itemContainer.setSelfPosition(0, currentY);
            dockRoot.addWidget(itemContainer);
            currentY += itemContainer.getSize().height;
            maxWidth = Math.max(maxWidth, itemContainer.getSize().width);
        }

        if (fluidTanks > 0) {
            Widget fluidContainer = createFluidContainer(tanks, fluidTanks);
            fluidContainer.setSelfPosition(0, currentY);
            dockRoot.addWidget(fluidContainer);
            currentY += fluidContainer.getSize().height;
            maxWidth = Math.max(maxWidth, fluidContainer.getSize().width);
        }

        if (maxWidth <= 0) maxWidth = 16;
        if (currentY <= 0) currentY = 16;

        dockRoot.setSize(maxWidth, currentY);
        dockRoot.setVisible(true).setActive(true);
        lastIndex = index;
    }

    private static @NotNull Widget createInventoryContainer(IItemTransfer inventory, int slots) {
        final int cols = calculateOptimalColumns(slots);
        final int rows = (slots + cols - 1) / cols;
        final int containerW = PAD_IN * 2 + cols * SLOT_SIZE;
        final int containerH = PAD_IN * 2 + rows * SLOT_SIZE;
        final int groupW = PAD_OUT * 2 + containerW;
        final int groupH = PAD_OUT * 2 + containerH;

        WidgetGroup group = new WidgetGroup(0, 0, groupW, groupH);
        WidgetGroup container = new WidgetGroup(PAD_OUT, PAD_OUT + 4, containerW, containerH);

        int index = 0;
        for (int y = 0; y < rows && index < slots; ++y) {
            for (int x = 0; x < cols && index < slots; ++x) {
                int sx = PAD_IN + x * SLOT_SIZE;
                int sy = PAD_IN + y * SLOT_SIZE;
                container.addWidget(createSlotWidget(inventory, index++, sx, sy));
            }
        }

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }

    private static @NotNull Widget createFluidContainer(FluidTransferList tanks, int slots) {
        final int cols = calculateOptimalColumns(slots);
        final int rows = (slots + cols - 1) / cols;
        final int containerW = PAD_IN * 2 + cols * SLOT_SIZE;
        final int containerH = PAD_IN * 2 + rows * SLOT_SIZE;
        final int groupW = PAD_OUT * 2 + containerW;
        final int groupH = PAD_OUT * 2 + containerH;

        WidgetGroup group = new WidgetGroup(0, 0, groupW, groupH);
        WidgetGroup container = new WidgetGroup(PAD_OUT, PAD_OUT, containerW, containerH);

        int index = 0;
        for (int y = 0; y < rows && index < slots; ++y) {
            for (int x = 0; x < cols && index < slots; ++x) {
                int sx = PAD_IN + x * SLOT_SIZE;
                int sy = PAD_IN + y * SLOT_SIZE;
                container.addWidget(createTankWidget(tanks.transfers[index], sx, sy));
                index++;
            }
        }

        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }

    private static int calculateOptimalColumns(int slots) {
        if (slots <= 0) return 1;
        if (slots <= MAX_COLS) return OPTIMAL_COLUMNS[slots];
        return MAX_COLS;
    }

    private static int calculateOptimalColumnsInternal(int slots) {
        if (slots <= 0) return 1;
        int bestCols = 1;
        double bestRatio = Double.MAX_VALUE;
        for (int cols = 1; cols <= Math.min(slots, MAX_COLS); cols++) {
            int rows = (slots + cols - 1) / cols;
            double ratio = Math.abs((double) cols / rows - 1.0); // 越接近 1 越“方”
            if (ratio < bestRatio) {
                bestRatio = ratio;
                bestCols = cols;
            }
        }
        return bestCols;
    }

    private static @NotNull Widget createSlotWidget(IItemTransfer inventory, int slotIndex, int sx, int sy) {
        return new SlotWidget(inventory, slotIndex, sx, sy, true, true)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setIngredientIO(IngredientIO.INPUT);
    }

    private static @NotNull Widget createTankWidget(IFluidTransfer storage, int sx, int sy) {
        return new TankWidget(storage, 0, sx, sy, true, true)
                .setBackground(GuiTextures.FLUID_SLOT);
    }
}
