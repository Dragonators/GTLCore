package org.gtlcore.gtlcore.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.google.common.math.LongMath;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class NumberUtils {

    private static final String[] UNITS = { "", "K", "M", "G", "T", "P", "E", "Z", "Y", "B", "N", "D" };

    private static final int[] NEAREST = { 1, 2, 4, 4, 4, 8, 8, 8, 8, 8, 8, 16, 16, 16, 16, 16 };

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
}
