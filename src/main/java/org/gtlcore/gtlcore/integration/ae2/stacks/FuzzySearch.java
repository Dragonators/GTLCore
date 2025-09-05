package org.gtlcore.gtlcore.integration.ae2.stacks;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

final class FuzzySearch {

    @VisibleForTesting
    static final KeyComparator COMPARATOR = new KeyComparator();

    private FuzzySearch() {}

    public static <K extends AEKey, V> Object2ObjectSortedMap<K, V> createMap() {
        return new Object2ObjectAVLTreeMap<>(COMPARATOR);
    }

    public static AEKey2LongMap.AVLTreeMap createMap2Long() {
        return new AEKey2LongMap.AVLTreeMap(COMPARATOR);
    }

    @SuppressWarnings({ "unchecked" })
    public static <T extends SortedMap<K, V>, K, V> T findFuzzy(T map, AEKey key, FuzzyMode fuzzy) {
        var lowerBound = makeLowerBound(key, fuzzy);
        var upperBound = makeUpperBound(key, fuzzy);
        Preconditions.checkState(lowerBound.itemDamage > upperBound.itemDamage);

        return (T) map.subMap((K) lowerBound, (K) upperBound);
    }

    @VisibleForTesting
    record FuzzyBound(int itemDamage) {}

    private static class KeyComparator implements Comparator<Object> {

        @Override
        public int compare(Object a, Object b) {
            FuzzyBound boundA = null;
            AEKey stackA = null;
            int fuzzyOrderB;
            if (a instanceof FuzzyBound) {
                boundA = (FuzzyBound) a;
                fuzzyOrderB = boundA.itemDamage;
            } else {
                stackA = (AEKey) a;
                fuzzyOrderB = stackA.getFuzzySearchValue();
            }
            FuzzyBound boundB = null;
            AEKey stackB = null;
            int fuzzyOrderA;
            if (b instanceof FuzzyBound) {
                boundB = (FuzzyBound) b;
                fuzzyOrderA = boundB.itemDamage;
            } else {
                stackB = (AEKey) b;
                fuzzyOrderA = stackB.getFuzzySearchValue();
            }

            if (boundA != null || boundB != null) {
                return Integer.compare(fuzzyOrderA, fuzzyOrderB);
            }

            if (stackA.equals(stackB)) {
                return 0;
            }

            final var fuzzyOrder = Integer.compare(fuzzyOrderA, fuzzyOrderB);
            if (fuzzyOrder != 0) {
                return fuzzyOrder;
            }

            return Long.compare(stackA.hashCode(), stackB.hashCode());
        }
    }

    private static final int MIN_DAMAGE_VALUE = -1;

    static FuzzyBound makeLowerBound(AEKey key, FuzzyMode fuzzy) {
        var maxValue = key.getFuzzySearchMaxValue();
        Preconditions.checkState(maxValue > 0, "Cannot use fuzzy search on keys that don't have a fuzzy max value: %s",
                key);

        int damage;
        if (fuzzy == FuzzyMode.IGNORE_ALL) {
            damage = maxValue;
        } else {
            var breakpoint = fuzzy.calculateBreakPoint(maxValue);
            damage = key.getFuzzySearchValue() <= breakpoint ? breakpoint : maxValue;
        }

        return new FuzzyBound(damage);
    }

    static FuzzyBound makeUpperBound(AEKey key, FuzzyMode fuzzy) {
        var maxValue = key.getFuzzySearchMaxValue();
        Preconditions.checkState(maxValue > 0, "Cannot use fuzzy search on keys that don't have a fuzzy max value: %s",
                key);

        int damage;
        if (fuzzy == FuzzyMode.IGNORE_ALL) {
            damage = MIN_DAMAGE_VALUE;
        } else {
            final var breakpoint = fuzzy.calculateBreakPoint(maxValue);
            damage = key.getFuzzySearchValue() <= breakpoint ? MIN_DAMAGE_VALUE : breakpoint;
        }

        return new FuzzyBound(damage);
    }
}
