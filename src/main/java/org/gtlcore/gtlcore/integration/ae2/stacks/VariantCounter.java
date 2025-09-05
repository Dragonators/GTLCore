package org.gtlcore.gtlcore.integration.ae2.stacks;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class VariantCounter implements Iterable<Object2LongMap.Entry<AEKey>> {

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract long getOrMin(AEKey key);

    public abstract long get(AEKey key);

    public abstract long remove(AEKey key);

    public abstract Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey key, FuzzyMode var2);

    abstract AEKey2LongMap getRecords();

    public abstract void reset();

    public abstract void clear();

    public abstract void removeZeros();

    public abstract VariantCounter copy();

    public void add(AEKey key, long amount) {
        this.getRecords().addTo(key, amount);
    }

    public void set(AEKey key, long amount) {
        this.getRecords().put(key, amount);
    }

    public void addAll(VariantCounter other) {
        for (var it = Object2LongMaps.fastIterator(other.getRecords()); it.hasNext();) {
            var entry = it.next();
            this.add(entry.getKey(), entry.getLongValue());
        }
    }

    public void removeAll(VariantCounter other) {
        for (var it = Object2LongMaps.fastIterator(other.getRecords()); it.hasNext();) {
            var entry = it.next();
            this.add(entry.getKey(), -entry.getLongValue());
        }
    }

    public void invert() {
        for (var it = Object2LongMaps.fastIterator(this.getRecords()); it.hasNext();) {
            var entry = it.next();
            entry.setValue(-entry.getLongValue());
        }
    }

    public static class UnorderedVariantMap extends VariantCounter {

        private AEKey2LongMap records = new AEKey2LongMap.OpenHashMap();

        @Override
        public long getOrMin(AEKey key) {
            return this.records.getOrDefault(key, Long.MIN_VALUE);
        }

        @Override
        public long get(AEKey key) {
            return this.records.getLong(key);
        }

        @Override
        public long remove(AEKey key) {
            return 0;
        }

        @Override
        public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey filter, FuzzyMode fuzzy) {
            return records.object2LongEntrySet();
        }

        @Override
        public int size() {
            return records.size();
        }

        @Override
        public boolean isEmpty() {
            return records.isEmpty();
        }

        @Override
        AEKey2LongMap getRecords() {
            return records;
        }

        @Override
        public void reset() {
            records.replaceAll((key, value) -> 0L);
        }

        @Override
        public void clear() {
            records.clear();
        }

        @Override
        public void removeZeros() {
            for (var it = Object2LongMaps.fastIterator(records); it.hasNext();) {
                var entry = it.next();
                if (entry.getLongValue() == 0L) it.remove();
            }
        }

        @Override
        public VariantCounter copy() {
            var result = new UnorderedVariantMap();
            result.records.putAll(records);
            return result;
        }

        @Override
        public @NotNull Iterator<Object2LongMap.Entry<AEKey>> iterator() {
            return records.iterator();
        }
    }

    public static class FuzzyVariantMap extends VariantCounter {

        private AEKey2LongMap.AVLTreeMap records = FuzzySearch.createMap2Long();

        @Override
        public long getOrMin(AEKey key) {
            return this.records.getOrDefault(key, Long.MIN_VALUE);
        }

        @Override
        public long get(AEKey key) {
            return records.getLong(key);
        }

        @Override
        public long remove(AEKey key) {
            return records.removeLong(key);
        }

        @Override
        public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey key, FuzzyMode fuzzy) {
            return FuzzySearch.findFuzzy((Object2LongSortedMap<AEKey>) records, key, fuzzy).object2LongEntrySet();
        }

        @Override
        public int size() {
            return records.size();
        }

        @Override
        public boolean isEmpty() {
            return records.isEmpty();
        }

        @Override
        AEKey2LongMap getRecords() {
            return this.records;
        }

        @Override
        public void reset() {
            records.replaceAll((key, value) -> 0L);
        }

        @Override
        public void clear() {
            records.clear();
        }

        @Override
        public void removeZeros() {
            for (var it = Object2LongMaps.fastIterator(records); it.hasNext();) {
                var entry = it.next();
                if (entry.getLongValue() == 0L) it.remove();
            }
        }

        @Override
        public VariantCounter copy() {
            var result = new FuzzyVariantMap();
            result.records.putAll(records);
            return result;
        }

        @Override
        public @NotNull Iterator<Object2LongMap.Entry<AEKey>> iterator() {
            return records.iterator();
        }
    }
}
