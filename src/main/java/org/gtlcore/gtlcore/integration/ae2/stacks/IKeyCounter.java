package org.gtlcore.gtlcore.integration.ae2.stacks;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;

/**
 * 代码参考自gto
 * &#064;line <a href="https://github.com/GregTech-Odyssey/GTOCore">...</a>
 */

public interface IKeyCounter {

    Object2ObjectOpenHashMap<Object, VariantCounter> getLists();

    VariantCounter getVariantCounter();

    static IKeyCounter of(KeyCounter keyCounter) {
        return (IKeyCounter) (Object) keyCounter;
    }

    class Entry implements Object2LongMap.Entry<AEKey> {

        private final long value;
        @Getter
        private final AEKey key;

        public Entry(long value, AEKey key) {
            this.value = value;
            this.key = key;
        }

        public long getLongValue() {
            return this.value;
        }

        public long setValue(long value) {
            return 0L;
        }
    }
}
