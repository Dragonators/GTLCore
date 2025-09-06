package org.gtlcore.gtlcore.integration.ae2.stacks;

import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

interface AEKey2LongMap extends Object2LongMap<AEKey> {

    long addTo(AEKey key, long incur);

    Iterator<Entry<AEKey>> iterator();

    final class OpenHashMap extends Object2LongOpenHashMap<AEKey> implements AEKey2LongMap {

        @Override
        public long addTo(AEKey key, long incur) {
            long oldValue = getLong(key);

            if (incur == 0) return oldValue;
            long newValue = oldValue + incur;

            if (((oldValue ^ newValue) & (incur ^ newValue)) < 0) {
                newValue = incur > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
            }

            put(key, newValue);
            return oldValue;
        }

        @Override
        public Iterator<Entry<AEKey>> iterator() {
            return this.object2LongEntrySet().fastIterator();
        }
    }

    final class AVLTreeMap extends Object2LongAVLTreeMap<AEKey> implements AEKey2LongMap {

        public AVLTreeMap(Comparator<? super AEKey> c) {
            super(c);
        }

        @Override
        public Iterator<Entry<AEKey>> iterator() {
            return this.object2LongEntrySet().iterator();
        }
    }
}
