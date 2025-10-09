package org.gtlcore.gtlcore.utils;

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisjointSetMap<T, K> {

    private final Object2ReferenceMap<T, ObjectSortedSet<@NotNull K>> map = new Object2ReferenceOpenHashMap<>();
    private final Object2ObjectMap<K, T> valueToKey = new Object2ObjectOpenHashMap<>();

    public boolean put(@NotNull T key, @NotNull K value) {
        T existingKey = valueToKey.get(value);
        if (existingKey != null) {
            if (!existingKey.equals(key)) return false;
        } else {
            valueToKey.put(value, key);
        }

        map.computeIfAbsent(key, ignore -> new ObjectLinkedOpenHashSet<>()).add(value);
        return true;
    }

    public void forcePut(@NotNull T key, @NotNull K value) {
        T conflictKey = valueToKey.get(value);
        if (conflictKey != null) {
            if (!conflictKey.equals(key)) {
                ObjectSet<K> conflictSet = map.get(conflictKey);
                if (conflictSet != null) {
                    conflictSet.remove(value);
                }
                valueToKey.put(value, key);
            }
        } else {
            valueToKey.put(value, key);
        }

        map.computeIfAbsent(key, ignore -> new ObjectLinkedOpenHashSet<>()).add(value);
    }

    public void removeKey(T key) {
        ObjectSet<K> values = map.remove(key);
        if (values != null) {
            values.forEach(valueToKey::remove);
        }
    }

    public void removeValue(K value) {
        if (valueToKey.containsKey(value)) {
            T obj = valueToKey.get(value);
            ObjectSet<K> set = map.get(obj);
            set.remove(value);
            if (set.isEmpty()) map.remove(obj);
            valueToKey.remove(value);
        }
    }

    // Never modify by get
    public @NotNull ObjectSet<@NotNull K> get(T key) {
        return map.containsKey(key) ? ObjectSets.unmodifiable(map.get(key)) : ObjectSet.of();
    }

    @NotNull
    public ObjectSet<@NotNull K> getFirstOrEmpty(T key) {
        return map.containsKey(key) ? map.get(key).isEmpty() ? ObjectSet.of() : ObjectSet.of(map.get(key).iterator().next()) : ObjectSet.of();
    }

    public @Nullable T getKeyForValue(K value) {
        return valueToKey.get(value);
    }

    public ObjectSet<@NotNull T> keySet() {
        return ObjectSets.unmodifiable(map.keySet());
    }

    public void clear() {
        map.clear();
        valueToKey.clear();
    }
}
