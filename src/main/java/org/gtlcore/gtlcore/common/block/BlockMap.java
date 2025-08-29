package org.gtlcore.gtlcore.common.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;

import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;
import java.util.function.Supplier;

public interface BlockMap {

    Object2ObjectOpenHashMap<String, Block[]> tierBlockMap = new Object2ObjectOpenHashMap<>(4);

    Int2ObjectOpenHashMap<Supplier<?>> scMap = new Int2ObjectOpenHashMap<>(4);
    Int2ObjectOpenHashMap<Supplier<?>> sepmMap = new Int2ObjectOpenHashMap<>(8);
    Int2ObjectOpenHashMap<Supplier<?>> calMap = new Int2ObjectOpenHashMap<>(16);
    Int2ObjectOpenHashMap<Supplier<?>> coilMap = new Int2ObjectOpenHashMap<>(24);

    static void init() {}

    static void build() {
        IntArrayList list = new IntArrayList(24);
        GTCEuAPI.HEATING_COILS.forEach((c, b) -> list.add(c.getCoilTemperature()));
        List<Integer> integers = list.stream().sorted().filter(Objects::nonNull).toList();
        GTCEuAPI.HEATING_COILS.forEach((c, b) -> coilMap.put(integers.indexOf(c.getCoilTemperature()), b));
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
