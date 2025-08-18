package org.gtlcore.gtlcore.integration.jade;

import org.gtlcore.gtlcore.integration.jade.provider.MEPatternBufferProvider;
import org.gtlcore.gtlcore.integration.jade.provider.MEPatternBufferProxyProvider;
import org.gtlcore.gtlcore.integration.jade.provider.TickTimeProvider;
import org.gtlcore.gtlcore.integration.jade.provider.WirelessOpticalDataHatchProvide;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * @author EasterFG on 2024/10/27
 */
@WailaPlugin
public class GTLJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new WirelessOpticalDataHatchProvide(), BlockEntity.class);
        registration.registerBlockDataProvider(new TickTimeProvider(), MetaMachineBlockEntity.class);
        registration.registerBlockDataProvider(new MEPatternBufferProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new MEPatternBufferProxyProvider(), BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new WirelessOpticalDataHatchProvide(), Block.class);
        registration.registerBlockComponent(new TickTimeProvider(), MetaMachineBlock.class);
        registration.registerBlockComponent(new MEPatternBufferProvider(), Block.class);
        registration.registerBlockComponent(new MEPatternBufferProxyProvider(), Block.class);
    }
}
