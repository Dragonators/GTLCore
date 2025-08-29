package org.gtlcore.gtlcore.api.gui;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;

import net.minecraft.world.level.block.Block;

import java.util.*;

public class BlockMapSelectorWidget extends SelectorWidget {

    public BlockMapSelectorWidget(int x, int y, int width, int height, List<Block> blocks) {
        super(x, y, width, height, blocks.isEmpty() ? List.of() :
                blocks.stream().map(b -> b.getName().getString()).toList(), -1);
        this.setBackground(GuiTextures.BACKGROUND_INVERSE);
        this.setButtonBackground(GuiTextures.BUTTON);
        this.textTexture.setRollSpeed(.25f);
        if (!this.candidates.isEmpty()) this.popUp.setSizeWidth(this.candidates.stream().mapToInt(String::length).max().orElse(0));
        if (blocks.isEmpty()) this.setVisible(false);
    }

    public int getIndex(String s) {
        return this.candidates.indexOf(s);
    }

    public void setIndex(int index, List<Block> blocks) {
        this.setBlocks(blocks);
        if (index < 0 || index >= this.candidates.size()) return;
        this.setValue(this.candidates.get(index));
        this.setVisible(true);
    }

    public void setBlocks(List<Block> blocks) {
        boolean isEmpty = blocks.isEmpty();
        this.setCandidates(isEmpty ? List.of() : blocks.stream().map(b -> b.getName().getString()).toList());
    }

    public void isShow() {
        this.setVisible(true);
        this.setShow(true);
    }
}
