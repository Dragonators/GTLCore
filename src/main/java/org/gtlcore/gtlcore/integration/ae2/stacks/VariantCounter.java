package org.gtlcore.gtlcore.integration.ae2.stacks;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

public abstract class VariantCounter implements Iterable<Object2LongMap.Entry<AEKey>> {

    private boolean dropZeros;

    public boolean isDropZeros() {
        return dropZeros;
    }

    public void setDropZeros(boolean dropZeros) {
        this.dropZeros = dropZeros;
    }

    public int size() {
        if (!dropZeros) return getRecords().size();
        var size = 0;
        for (var value : getRecords().values()) if (value != 0) size++;
        return size;
    }

    public boolean isEmpty() {
        if (!dropZeros) return getRecords().isEmpty();
        for (var value : getRecords().values()) if (value != 0) return false;
        return true;
    }

    public long get(AEKey key) {
        return this.getRecords().getOrDefault(key, 0);
    }

    public long remove(AEKey key) {
        return getRecords().removeLong(key);
    }

    public abstract Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey key, FuzzyMode var2);

    abstract AEKey2LongMap getRecords();

    public void reset() {
        if (dropZeros) getRecords().clear();
        else getRecords().replaceAll((key, value) -> 0L);
    }

    public void clear() {
        getRecords().clear();
    }

    public void removeZeros() {
        for (var it = Object2LongMaps.fastIterator(getRecords()); it.hasNext();) {
            var entry = it.next();
            if (entry.getLongValue() == 0L) it.remove();
        }
    }

    public abstract VariantCounter copy();

    public void add(AEKey key, long amount) {
        this.getRecords().addTo(key, amount);
    }

    public void set(AEKey key, long amount) {
        if (dropZeros && amount == 0) {
            getRecords().removeLong(key);
        } else {
            getRecords().put(key, amount);
        }
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

    @Override
    public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
        return Object2LongMaps.fastIterator(getRecords());
    }

    public static class UnorderedVariantMap extends VariantCounter {

        private final AEKey2LongMap records = new AEKey2LongMap.OpenHashMap();

        @Override
        public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey filter, FuzzyMode fuzzy) {
            return records.object2LongEntrySet();
        }

        @Override
        AEKey2LongMap getRecords() {
            return records;
        }

        @Override
        public VariantCounter copy() {
            var result = new UnorderedVariantMap();
            result.records.putAll(records);
            return result;
        }
    }

    public static class FuzzyVariantMap extends VariantCounter {

        private final AEKey2LongMap.AVLTreeMap records = FuzzySearch.createMap2Long();

        @Override
        public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey key, FuzzyMode fuzzy) {
            return FuzzySearch.findFuzzy((Object2LongSortedMap<AEKey>) records, key, fuzzy).object2LongEntrySet();
        }

        @Override
        AEKey2LongMap getRecords() {
            return this.records;
        }

        @Override
        public VariantCounter copy() {
            var result = new FuzzyVariantMap();
            result.records.putAll(records);
            return result;
        }
    }
}
