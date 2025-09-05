package org.gtlcore.gtlcore.integration.ae2.stacks;

import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

interface AEKey2LongMap extends Object2LongMap<AEKey> {

    long addTo(AEKey k, long incur);

    Iterator<Entry<AEKey>> iterator();

    final class OpenHashMap extends Object2LongOpenHashMap<AEKey> implements AEKey2LongMap {

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
