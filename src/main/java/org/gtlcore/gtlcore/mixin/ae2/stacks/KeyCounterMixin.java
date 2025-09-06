package org.gtlcore.gtlcore.mixin.ae2.stacks;

import org.gtlcore.gtlcore.integration.ae2.stacks.*;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.*;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.*;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(KeyCounter.class)
public class KeyCounterMixin implements IKeyCounter {

    @Unique
    private static final Object OBJECT = new Object();
    @Unique
    private VariantCounter variantCounter;
    @Unique
    private Object2ObjectOpenHashMap<Object, VariantCounter> lists = new Object2ObjectOpenHashMap<>();

    @Unique
    private VariantCounter getSubIndex(AEKey key) {
        if (key.getFuzzySearchMaxValue() > 0) {
            return this.getLists().computeIfAbsent(key.getPrimaryKey(), (k) -> new VariantCounter.FuzzyVariantMap());
        } else {
            if (this.getLists().isEmpty() || this.variantCounter == null) {
                this.variantCounter = new VariantCounter.UnorderedVariantMap();
                this.getLists().put(OBJECT, this.variantCounter);
            }
            return this.variantCounter;
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey key, FuzzyMode fuzzy) {
        if (this.getLists() == null) {
            return Collections.emptyList();
        } else {
            if (this.variantCounter != null) {
                return this.variantCounter.findFuzzy(key, fuzzy);
            } else {
                var subIndex = this.getLists().get(key.getPrimaryKey());
                if (subIndex != null) return subIndex.findFuzzy(key, fuzzy);
            }
            return Collections.emptyList();
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void removeZeros() {
        if (this.getLists() == null) return;
        for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            var variantList = entry.getValue();
            variantList.removeZeros();
            if (variantList.isEmpty()) it.remove();
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void addAll(KeyCounter other) {
        var list = IKeyCounter.of(other).getLists();
        if (list == null) return;
        for (var it = list.object2ObjectEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            var ourSubIndex = getLists().get(entry.getKey());
            if (ourSubIndex == null) {
                var value = entry.getValue().copy();
                if (entry.getKey() == OBJECT) this.variantCounter = value;
                getLists().put(entry.getKey(), value);
            } else ourSubIndex.addAll(entry.getValue());
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void removeAll(KeyCounter other) {
        var list = IKeyCounter.of(other).getLists();
        if (list == null) return;
        for (var it = list.object2ObjectEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            var ourSubIndex = getLists().get(entry.getKey());
            if (ourSubIndex == null) {
                var copied = entry.getValue().copy();
                copied.invert();
                if (entry.getKey() == OBJECT) this.variantCounter = copied;
                getLists().put(entry.getKey(), copied);
            } else ourSubIndex.removeAll(entry.getValue());
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void add(AEKey key, long amount) {
        this.getSubIndex(key).add(key, amount);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void remove(AEKey key, long amount) {
        this.add(key, -amount);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public long remove(AEKey key) {
        var subIndex = getSubIndex(key);
        var ret = subIndex.remove(key);
        if (subIndex.isEmpty()) lists.remove(key.getPrimaryKey());
        return ret;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void set(AEKey key, long amount) {
        this.getSubIndex(key).set(key, amount);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public long get(AEKey key) {
        if (this.getLists() == null) return 0L;
        var subIndex = key.getFuzzySearchMaxValue() > 0 ?
                this.getLists().get(key.getPrimaryKey()) : this.variantCounter;
        return subIndex == null ? 0L : subIndex.get(key);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void reset() {
        if (this.getLists() != null) {
            for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
                it.next().getValue().reset();
            }
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void clear() {
        if (this.getLists() != null) {
            for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
                it.next().getValue().clear();
            }
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public boolean isEmpty() {
        if (this.getLists() != null) {
            for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                if (!entry.getValue().isEmpty()) return false;
            }
        }
        return true;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public int size() {
        if (this.getLists() == null) return 0;
        int tot = 0;
        for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext(); tot += it.next().getValue().size()) {}
        return tot;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
        return this.getLists() == null ? Collections.emptyListIterator() :
                Iterators.concat(Iterators.transform(this.getLists().values().iterator(), VariantCounter::iterator));
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public AEKey getFirstKey() {
        var e = this.getFirstEntry();
        return e != null ? e.getKey() : null;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public <T extends AEKey> T getFirstKey(Class<T> keyClass) {
        var e = this.getFirstEntry(keyClass);
        return e != null ? keyClass.cast(e.getKey()) : null;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public Object2LongMap.Entry<AEKey> getFirstEntry() {
        if (this.getLists() != null) {
            for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                var iter = entry.getValue().iterator();
                if (iter.hasNext()) return iter.next();
            }
        }
        return null;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public <T extends AEKey> Object2LongMap.Entry<AEKey> getFirstEntry(Class<T> keyClass) {
        if (this.getLists() != null) {
            for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                for (var iter : entry.getValue()) {
                    if (keyClass.isInstance(iter.getKey())) return iter;
                }
            }
        }
        return null;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public Set<AEKey> keySet() {
        if (this.getLists() == null) return Collections.emptySet();
        else {
            var keys = new ObjectOpenHashSet<AEKey>(this.size());
            for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                for (var iter : entry.getValue()) keys.add(iter.getKey());
            }
            return keys;
        }
    }

    @Override
    public Object2ObjectOpenHashMap<Object, VariantCounter> getLists() {
        return this.lists;
    }

    @Override
    public VariantCounter getVariantCounter() {
        return this.variantCounter;
    }

    @Override
    public void removeEmptySubmaps() {
        for (var it = this.getLists().object2ObjectEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            if (entry.getValue().isEmpty()) it.remove();
        }
    }
}
