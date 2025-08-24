package org.gtlcore.gtlcore.integration.ae2.widget;

import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEPatternViewSlotWidget;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AEPatternViewExtendSlotWidget extends AEPatternViewSlotWidget {

    public AEPatternViewExtendSlotWidget(IItemTransfer inventory, int slotIndex, int xPosition, int yPosition) {
        super(inventory, slotIndex, xPosition, yPosition);
    }

    @Nullable
    private Runnable onMiddleClick; // 接收槽位 index（或你想传的任何信息）

    public AEPatternViewExtendSlotWidget setOnMiddleClick(@Nullable Runnable runnable) {
        this.onMiddleClick = runnable;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (slotReference != null && isMouseOverElement(mouseX, mouseY) && gui != null) {
            if (button == 2 && onMiddleClick != null) {
                writeClientAction(10, writer -> writer.writeBoolean(true));
                onMiddleClick.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);
        if (id == 10) {
            if (onMiddleClick != null) {
                onMiddleClick.run();
            }
        }
    }
}
