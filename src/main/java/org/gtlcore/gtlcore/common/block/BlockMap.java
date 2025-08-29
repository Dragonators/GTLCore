package org.gtlcore.gtlcore.common.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;

import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;

public interface BlockMap {

    Object2ObjectOpenHashMap<String, Block[]> tierBlockMap = new Object2ObjectOpenHashMap<>(4);

    Int2ObjectOpenHashMap<Supplier<?>> scMap = new Int2ObjectOpenHashMap<>(4);
    Int2ObjectOpenHashMap<Supplier<?>> sepmMap = new Int2ObjectOpenHashMap<>(8);
    Int2ObjectOpenHashMap<Supplier<?>> calMap = new Int2ObjectOpenHashMap<>(16);
    Int2ObjectOpenHashMap<Supplier<?>> coilMap = new Int2ObjectOpenHashMap<>(24);

    static void init() {
        GTCEuAPI.HEATING_COILS.forEach((c, b) -> coilMap.put(c.getTier(), b));
    }

    static void build() {
        var tier = new ObjectArrayList<>(scMap.int2ObjectEntrySet());
        tier.sort(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey));
        tierBlockMap.put("sc", tier.stream().map(Map.Entry::getValue).map(Supplier::get).toArray(Block[]::new));
        tier = new ObjectArrayList<>(sepmMap.int2ObjectEntrySet());
        tier.sort(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey));
        tierBlockMap.put("sepm", tier.stream().map(Map.Entry::getValue).map(Supplier::get).toArray(Block[]::new));
        tier = new ObjectArrayList<>(calMap.int2ObjectEntrySet());
        tier.sort(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey));
        tierBlockMap.put("cal", tier.stream().map(Map.Entry::getValue).map(Supplier::get).toArray(Block[]::new));
        tier = new ObjectArrayList<>(coilMap.int2ObjectEntrySet());
        tier.sort(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey));
        tierBlockMap.put("coil", tier.stream().map(Map.Entry::getValue).map(Supplier::get).toArray(Block[]::new));
    }
}
