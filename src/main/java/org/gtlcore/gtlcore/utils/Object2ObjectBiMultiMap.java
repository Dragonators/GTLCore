package org.gtlcore.gtlcore.utils;

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Object2ObjectBiMultiMap<K, V> {

    private final Object2ObjectMap<K, ObjectSet<@NotNull V>> keyToValues = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<V, ObjectSet<@NotNull K>> valueToKeys = new Object2ObjectOpenHashMap<>();

    public void put(@NotNull K key, @NotNull V value) {
        keyToValues.computeIfAbsent(key, k -> new ObjectArraySet<>()).add(value);
        valueToKeys.computeIfAbsent(value, v -> new ObjectArraySet<>()).add(key);
    }

    public ObjectSet<V> getValues(K key) {
        return keyToValues.getOrDefault(key, ObjectSets.emptySet());
    }

    public ObjectSet<K> getKeys(V value) {
        return valueToKeys.getOrDefault(value, ObjectSets.emptySet());
    }

    public void remove(K key, V value) {
        Optional.ofNullable(keyToValues.get(key)).ifPresent(set -> {
            set.remove(value);
            if (set.isEmpty()) keyToValues.remove(key);
        });
        Optional.ofNullable(valueToKeys.get(value)).ifPresent(set -> {
            set.remove(key);
            if (set.isEmpty()) valueToKeys.remove(value);
        });
    }

    public void removeByKey(K key) {
        ObjectSet<V> values = keyToValues.remove(key);
        if (values != null) {
            for (V v : values) {
                ObjectSet<K> ks = valueToKeys.get(v);
                if (ks != null) {
                    ks.remove(key);
                    if (ks.isEmpty()) valueToKeys.remove(v);
                }
            }
        }
    }

    public void removeByValue(V value) {
        ObjectSet<K> keys = valueToKeys.remove(value);
        if (keys != null) {
            for (K k : keys) {
                ObjectSet<V> vs = keyToValues.get(k);
                if (vs != null) {
                    vs.remove(value);
                    if (vs.isEmpty()) keyToValues.remove(k);
                }
            }
        }
    }

    public ObjectSet<K> keySet() {
        return ObjectSets.unmodifiable(keyToValues.keySet());
    }

    public ObjectSet<V> valueSet() {
        return ObjectSets.unmodifiable(valueToKeys.keySet());
    }

    public void clear() {
        keyToValues.clear();
        valueToKeys.clear();
    }

    public int size() {
        return keyToValues.values().stream().mapToInt(ObjectSet::size).sum();
    }

    public void forEach(Consumer<Map.Entry<K, V>> action) {
        for (var entry : keyToValues.entrySet()) {
            K key = entry.getKey();
            for (V value : entry.getValue()) {
                action.accept(Map.entry(key, value));
            }
        }
    }
}
