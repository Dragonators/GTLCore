package org.gtlcore.gtlcore.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.google.common.math.LongMath;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class NumberUtils {

    public static final String[] UNITS = { "", "K", "M", "G", "T", "P", "E", "Z", "Y", "B", "N", "D" };

    public static final int[] NEAREST = { 1, 2, 4, 4, 4, 8, 8, 8, 8, 8, 8, 16, 16, 16, 16, 16 };

    public static final BigInteger BIG_INTEGER_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    public static String formatLong(long number) {
        DecimalFormat df = new DecimalFormat("#.##");
        double temp = number;
        int unitIndex = 0;
        while (temp >= 1000 && unitIndex < UNITS.length - 1) {
            temp /= 1000;
            unitIndex++;
        }
        return df.format(temp) + UNITS[unitIndex];
    }

    public static String formatDouble(double number) {
        DecimalFormat df = new DecimalFormat("#.##");
        double temp = number;
        int unitIndex = 0;
        while (temp >= 1000 && unitIndex < UNITS.length - 1) {
            temp /= 1000;
            unitIndex++;
        }
        return df.format(temp) + UNITS[unitIndex];
    }

    public static MutableComponent numberText(double number) {
        return Component.literal(formatDouble(number));
    }

    public static MutableComponent numberText(long number) {
        return Component.literal(formatLong(number));
    }

    public static long getLongValue(BigInteger bigInt) {
        return bigInt.compareTo(BIG_INTEGER_MAX_LONG) > 0 ? Long.MAX_VALUE : bigInt.longValue();
    }

    public static int getFakeVoltageTier(long voltage) {
        long a = voltage;
        int b = 0;
        while (a / 4L >= 8L) {
            b++;
            a /= 4L;
        }
        return b;
    }

    public static long getVoltageFromFakeTier(int tier) {
        return LongMath.pow(4L, tier + 1) * 2;
    }

    public static int nearestPow2Lookup(int x) {
        if (x < 1) return 1;
        if (x > 16) return 16;
        return NEAREST[x - 1];
    }

    // actualConsumeParallel > 1
    public static int getAdditionalTier(double durationFactor, double actualConsumeParallel) {
        // 移除边界检查以提升性能
        // if (!(durationFactor > 0.0 && durationFactor < 1.0)) {
        // throw new IllegalArgumentException("require 0 < durationFactor < 1");
        // }

        return (int) Math.ceil(Math.log(actualConsumeParallel) / (-Math.log(durationFactor)));
    }

    // ========================================
    // Energy
    // ========================================

    /**
     * 计算最大的 x，使 B * 4^x <= I * V（I,V,B 为非负 long；x 为自然数）。
     *
     * 约定/边界：
     * - 若 B <= 0：抛出 IllegalArgumentException（基础功率应为正）。
     * - 若 I < 0 或 V < 0：抛出 IllegalArgumentException（电流/电压应为非负）。
     * - 若 I == 0 或 V == 0：总功率为 0；若 B>0，则不存在满足条件的 x，返回 -1。
     *
     * @return 最大 x；若不存在（例如 I==0 或 V==0），返回 -1。
     */
    public static int maxExponentOf4(long I, long V, long B) {
        if (B <= 0) {
            throw new IllegalArgumentException("Base power B must be > 0");
        }
        if (I < 0 || V < 0) {
            throw new IllegalArgumentException("I and V must be non-negative");
        }
        if (I == 0 || V == 0) {
            return -1; // 总功率=0，且 B>0，不可能满足
        }

        // 左侧：P = I * V，表示为 128 位无符号 (hi, lo)
        final long pHi = Math.multiplyHigh(I, V);
        final long pLo = I * V; // 低 64 位，溢出无碍

        if (pHi == 0 && pLo == 0) {
            return -1; // 理论上不可能到这，I/V 已检查
        }

        // 右侧：R = B << (2x)
        // 用 bitLength 粗估一个候选 x0，然后做 1~2 次 128 位比较校正
        final int bitsP = bitLenUnsigned128(pHi, pLo);
        final int bitsB = bitLenUnsigned64(B);

        // 估计：B * 2^(2x) <= P => 2x <= bitsP - bitsB （近似）
        int x = Math.max(0, (bitsP - bitsB) / 2);
        x = Math.min(x, 63); // 因为最多移位 2x=126

        // 校正（最多 ±1）
        if (compareRightShiftedGreaterThanP(B, x, pHi, pLo)) {
            x--; // 候选偏大
        } else {
            // 看看还能否再加 1
            if (x < 63 && !compareRightShiftedGreaterThanP(B, x + 1, pHi, pLo)) {
                x++;
            }
        }

        // 若 x 变成负，说明连 x=0 都不满足（但这只有 P < B 时才会发生）
        return (x >= 0) ? x : -1;
    }

    /** 判断 B << (2*xx) 是否 > P (pHi,pLo) —— 若是，说明 xx 太大了。 */
    private static boolean compareRightShiftedGreaterThanP(long B, int xx, long pHi, long pLo) {
        final int shift = xx << 1; // 2x
        long[] r = shiftLeft128(B, shift);
        return compare128Unsigned(r[0], r[1], pHi, pLo) > 0;
    }

    /** (hi, lo) 无符号 128 位比较：a 与 b 比较，返回负/零/正。 */
    private static int compare128Unsigned(long aHi, long aLo, long bHi, long bLo) {
        int hiCmp = Long.compareUnsigned(aHi, bHi);
        if (hiCmp != 0) return hiCmp;
        return Long.compareUnsigned(aLo, bLo);
    }

    /** 计算无符号 128 位 (hi, lo) 的位长（bitLength），0 返回 0。 */
    private static int bitLenUnsigned128(long hi, long lo) {
        if (hi != 0) return 64 + (64 - Long.numberOfLeadingZeros(hi));
        if (lo != 0) return (64 - Long.numberOfLeadingZeros(lo));
        return 0;
    }

    /** 计算无符号 64 位 x 的位长（bitLength），0 返回 0。 */
    private static int bitLenUnsigned64(long x) {
        return x == 0 ? 0 : 64 - Long.numberOfLeadingZeros(x);
    }

    /**
     * 把 (b << shift) 表示为 128 位无符号 (hi, lo)，其中 0 <= shift <= 126。
     * 返回数组 [hi, lo]。
     */
    private static long[] shiftLeft128(long b, int shift) {
        if (shift == 0) return new long[] { 0L, b };

        if (shift < 64) {
            long hi = b >>> (64 - shift); // 无符号右移生成高位
            long lo = b << shift;
            return new long[] { hi, lo };
        } else {
            int s = shift - 64;           // 0..62（因为 shift 最大 126）
            long hi = (s == 0) ? b : (b << s);
            long lo = 0L;
            return new long[] { hi, lo };
        }
    }

    public static long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
            // If a and b have different signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }

    public static long saturatedMultiply(long a, long b) {
        int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a) + Long.numberOfLeadingZeros(b) + Long.numberOfLeadingZeros(~b);
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        // the return value if we will overflow (which we calculate by overflowing a long :) )
        long limit = Long.MAX_VALUE + ((a ^ b) >>> (Long.SIZE - 1));
        if (leadingZeros < Long.SIZE | (a < 0 & b == Long.MIN_VALUE)) {
            // overflow
            return limit;
        }
        long result = a * b;
        if (a == 0 || result / a == b) {
            return result;
        }
        return limit;
    }
}
