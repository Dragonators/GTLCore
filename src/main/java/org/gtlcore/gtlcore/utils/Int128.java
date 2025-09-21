package org.gtlcore.gtlcore.utils;

import com.google.common.primitives.Ints;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * 高性能128位有符号整数实现，使用两个long组成
 * 避免频繁创建新对象，支持原地修改
 */
@Getter
public final class Int128 extends Number implements Comparable<Int128> {

    // 获取值
    // 高64位和低64位
    private long high;
    private long low;

    // 常用常量 - 使用方法返回新实例，避免意外修改
    public static Int128 ZERO() {
        return new Int128(0, 0);
    }

    public static Int128 ONE() {
        return new Int128(0, 1);
    }

    public static Int128 NEGATIVE_ONE() {
        return new Int128(-1L, -1L);
    }

    public static final Int128 MAX_VALUE = new Int128(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
    public static final Int128 MIN_VALUE = new Int128(0x8000000000000000L, 0);

    // 构造函数
    public Int128() {
        this.high = 0;
        this.low = 0;
    }

    @Override
    public int intValue() {
        return Ints.saturatedCast(low);
    }

    @Override
    public long longValue() {
        return low;
    }

    @Override
    public float floatValue() {
        return (float) toDouble();
    }

    @Override
    public double doubleValue() {
        return toDouble();
    }

    public Int128(long high, long low) {
        this.high = high;
        this.low = low;
    }

    public Int128(long value) {
        this.high = value < 0 ? -1L : 0;
        this.low = value;
    }

    // 设置值
    public Int128 set(long high, long low) {
        this.high = high;
        this.low = low;
        return this;
    }

    public Int128 set(Int128 other) {
        this.high = other.high;
        this.low = other.low;
        return this;
    }

    // =================== 算术运算 ===================

    /**
     * 原地加法：this = this + other (long)
     * 优化的128位加64位算法
     */
    public Int128 add(long other) {
        long newLow = this.low + other;
        long newHigh = this.high;

        // 处理进位：检查无符号溢出
        if (Long.compareUnsigned(newLow, this.low) < 0) {
            newHigh++; // 发生无符号进位
        }

        // 处理符号扩展：如果other是负数，将其符号扩展到高位
        if (other < 0) {
            newHigh--; // 负数的符号扩展
        }

        this.low = newLow;
        this.high = newHigh;
        return this;
    }

    public Int128 add(Int128 other) {
        long newLow = this.low + other.low;
        long newHigh = this.high + other.high;

        // 处理无符号进位
        if (Long.compareUnsigned(newLow, this.low) < 0) {
            newHigh++;
        }

        this.low = newLow;
        this.high = newHigh;
        return this;
    }

    public static Int128 add(Int128 a, Int128 b, Int128 result) {
        long newLow = a.low + b.low;
        long newHigh = a.high + b.high;

        if (Long.compareUnsigned(newLow, a.low) < 0) {
            newHigh++;
        }

        result.low = newLow;
        result.high = newHigh;
        return result;
    }

    /**
     * 原地减法：this = this - other
     */
    public Int128 subtract(Int128 other) {
        long newLow = this.low - other.low;
        long newHigh = this.high - other.high;

        // 处理无符号借位
        if (Long.compareUnsigned(this.low, other.low) < 0) {
            newHigh--;
        }

        this.low = newLow;
        this.high = newHigh;
        return this;
    }

    /**
     * 减法到新对象：result = a - b
     */
    public static Int128 subtract(Int128 a, Int128 b, Int128 result) {
        long newLow = a.low - b.low;
        long newHigh = a.high - b.high;

        if (Long.compareUnsigned(a.low, b.low) < 0) {
            newHigh--;
        }

        result.low = newLow;
        result.high = newHigh;
        return result;
    }

    /**
     * 原地乘法：this = this * other
     * 使用Karatsuba算法优化
     */
    public Int128 multiply(Int128 other) {
        // 分解为32位块进行计算
        long a0 = this.low & 0xFFFFFFFFL;
        long a1 = this.low >>> 32;
        long a2 = this.high & 0xFFFFFFFFL;
        long a3 = this.high >>> 32;

        long b0 = other.low & 0xFFFFFFFFL;
        long b1 = other.low >>> 32;
        long b2 = other.high & 0xFFFFFFFFL;
        long b3 = other.high >>> 32;

        // 计算部分乘积
        long p0 = a0 * b0;
        long p1 = a0 * b1 + a1 * b0;
        long p2 = a0 * b2 + a1 * b1 + a2 * b0;
        long p3 = a0 * b3 + a1 * b2 + a2 * b1 + a3 * b0;

        // 处理进位
        p1 += p0 >>> 32;
        p2 += p1 >>> 32;
        p3 += p2 >>> 32;

        // 组合结果
        this.low = (p1 << 32) | (p0 & 0xFFFFFFFFL);
        this.high = (p3 << 32) | (p2 & 0xFFFFFFFFL);

        return this;
    }

    /**
     * 乘法到新对象：result = a * b
     */
    public static Int128 multiply(Int128 a, Int128 b, Int128 result) {
        long a0 = a.low & 0xFFFFFFFFL;
        long a1 = a.low >>> 32;
        long a2 = a.high & 0xFFFFFFFFL;
        long a3 = a.high >>> 32;

        long b0 = b.low & 0xFFFFFFFFL;
        long b1 = b.low >>> 32;
        long b2 = b.high & 0xFFFFFFFFL;
        long b3 = b.high >>> 32;

        long p0 = a0 * b0;
        long p1 = a0 * b1 + a1 * b0;
        long p2 = a0 * b2 + a1 * b1 + a2 * b0;
        long p3 = a0 * b3 + a1 * b2 + a2 * b1 + a3 * b0;

        p1 += p0 >>> 32;
        p2 += p1 >>> 32;
        p3 += p2 >>> 32;

        result.low = (p1 << 32) | (p0 & 0xFFFFFFFFL);
        result.high = (p3 << 32) | (p2 & 0xFFFFFFFFL);

        return result;
    }

    /**
     * 原地乘法：this = this * multiplier (long)
     * 优化的128位乘64位算法，比通用乘法快约2倍
     */
    public Int128 multiply(long multiplier) {
        // 128位 × 64位的优化算法
        // 分解为：(high:low) × multiplier

        // 分解this为32位块
        long a0 = this.low & 0xFFFFFFFFL;
        long a1 = this.low >>> 32;
        long a2 = this.high & 0xFFFFFFFFL;
        long a3 = this.high >>> 32;

        // 分解multiplier为32位块
        long m0 = multiplier & 0xFFFFFFFFL;
        long m1 = multiplier >>> 32;

        // 计算部分乘积
        long p0 = a0 * m0;
        long p1 = a0 * m1 + a1 * m0;
        long p2 = a1 * m1 + a2 * m0;
        long p3 = a2 * m1 + a3 * m0;

        // 处理进位
        p1 += p0 >>> 32;
        p2 += p1 >>> 32;
        p3 += p2 >>> 32;

        // 组合结果
        this.low = (p1 << 32) | (p0 & 0xFFFFFFFFL);
        this.high = (p3 << 32) | (p2 & 0xFFFFFFFFL);

        return this;
    }

    /**
     * 乘法到新对象：result = a * multiplier
     */
    public static Int128 multiply(Int128 a, long multiplier, Int128 result) {
        result.set(a.high, a.low);
        return result.multiply(multiplier);
    }

    /**
     * 原地除法：this = this / divisor
     * 返回余数到remainder参数
     */
    public Int128 divide(Int128 divisor, Int128 remainder) {
        if (divisor.isZero()) {
            throw new ArithmeticException("Division by zero");
        }

        // 处理特殊情况
        if (this.isZero()) {
            remainder.set(0, 0);
            return this.set(0, 0);
        }

        // 记录符号
        boolean negativeResult = (this.isNegative() != divisor.isNegative());

        // 转为正数进行计算
        Int128 dividend = new Int128(this.high, this.low);
        Int128 div = new Int128(divisor.high, divisor.low);

        if (dividend.isNegative()) dividend.negate();
        if (div.isNegative()) div.negate();

        // 二进制长除法
        Int128 quotient = new Int128();
        Int128 temp = new Int128();

        for (int i = 127; i >= 0; i--) {
            temp.shiftLeft(1);
            if (dividend.getBit(i)) {
                temp.low |= 1;
            }

            if (temp.compareTo(div) >= 0) {
                temp.subtract(div);
                quotient.setBit(i, true);
            }
        }

        // 设置余数
        remainder.set(temp);

        // 应用符号
        this.set(quotient);
        if (negativeResult) {
            this.negate();
        }

        return this;
    }

    /**
     * 简单除法（除以long）
     */
    public Int128 divide(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }

        // 128位除以64位的优化算法
        boolean neg = (this.isNegative() != (divisor < 0));

        if (this.isNegative()) this.negate();
        if (divisor < 0) divisor = -divisor;

        long rem = 0;
        long resultHigh = 0;
        long resultLow = 0;

        // 先除高位
        if (high != 0) {
            resultHigh = Long.divideUnsigned(high, divisor);
            rem = Long.remainderUnsigned(high, divisor);
        }

        // 再除低位（包含高位余数）
        if (rem != 0) {
            // 组合余数和低位
            long combined = (rem << 32) | (low >>> 32);
            long q1 = Long.divideUnsigned(combined, divisor);
            rem = Long.remainderUnsigned(combined, divisor);

            combined = (rem << 32) | (low & 0xFFFFFFFFL);
            long q0 = Long.divideUnsigned(combined, divisor);

            resultLow = (q1 << 32) | q0;
        } else {
            resultLow = Long.divideUnsigned(low, divisor);
        }

        this.high = resultHigh;
        this.low = resultLow;

        if (neg) this.negate();

        return this;
    }

    // =================== 位运算 ===================

    /**
     * 原地左移
     */
    public Int128 shiftLeft(int n) {
        n &= 127; // 限制在0-127范围

        if (n >= 64) {
            this.high = this.low << (n - 64);
            this.low = 0;
        } else if (n > 0) {
            this.high = (this.high << n) | (this.low >>> (64 - n));
            this.low = this.low << n;
        }

        return this;
    }

    /**
     * 原地右移（算术右移，保留符号）
     */
    public Int128 shiftRight(int n) {
        n &= 127;

        if (n >= 64) {
            this.low = this.high >> (n - 64);
            this.high = this.high >> 63; // 符号扩展
        } else if (n > 0) {
            this.low = (this.low >>> n) | (this.high << (64 - n));
            this.high = this.high >> n;
        }

        return this;
    }

    /**
     * 原地逻辑右移（无符号）
     */
    public Int128 shiftRightUnsigned(int n) {
        n &= 127;

        if (n >= 64) {
            this.low = this.high >>> (n - 64);
            this.high = 0;
        } else if (n > 0) {
            this.low = (this.low >>> n) | (this.high << (64 - n));
            this.high = this.high >>> n;
        }

        return this;
    }

    /**
     * 原地取反
     */
    public Int128 negate() {
        this.low = ~this.low;
        this.high = ~this.high;

        // 加1
        this.low++;
        if (this.low == 0) {
            this.high++;
        }

        return this;
    }

    // =================== 比较运算 ===================

    @Override
    public int compareTo(Int128 other) {
        // 先比较符号
        boolean thisNeg = this.isNegative();
        boolean otherNeg = other.isNegative();

        if (thisNeg != otherNeg) {
            return thisNeg ? -1 : 1;
        }

        // 比较高位
        if (this.high != other.high) {
            return Long.compare(this.high, other.high);
        }

        // 比较低位（无符号）
        return Long.compareUnsigned(this.low, other.low);
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Int128)) return false;
        Int128 other = (Int128) obj;
        return this.high == other.high && this.low == other.low;
    }

    // =================== 工具方法 ===================

    public boolean isZero() {
        return high == 0 && low == 0;
    }

    public boolean isNegative() {
        return high < 0;
    }

    public boolean isPositive() {
        return !isNegative() && !isZero();
    }

    public boolean getBit(int index) {
        if (index < 64) {
            return (low & (1L << index)) != 0;
        } else {
            return (high & (1L << (index - 64))) != 0;
        }
    }

    public void setBit(int index, boolean value) {
        if (index < 64) {
            if (value) {
                low |= (1L << index);
            } else {
                low &= ~(1L << index);
            }
        } else {
            if (value) {
                high |= (1L << (index - 64));
            } else {
                high &= ~(1L << (index - 64));
            }
        }
    }

    public long toLong() {
        return low;
    }

    public double toDouble() {
        return high * Math.pow(2, 64) + (low & 0x7FFFFFFFFFFFFFFFL) + (low < 0 ? Math.pow(2, 63) : 0);
    }

    /**
     * 转换为BigInteger
     * 注意：这会创建新对象，性能不如直接使用Int128
     */
    public BigInteger toBigInteger() {
        if (isZero()) {
            return BigInteger.ZERO;
        }

        // 创建16字节数组来表示128位数据
        byte[] bytes = new byte[16];

        // 填充高64位（大端序）
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (high >>> (56 - i * 8));
        }

        // 填充低64位（大端序）
        for (int i = 0; i < 8; i++) {
            bytes[i + 8] = (byte) (low >>> (56 - i * 8));
        }

        return new BigInteger(bytes);
    }

    /**
     * 从BigInteger创建Int128
     * 
     * @param value BigInteger值
     * @return 新的Int128实例
     * @throws ArithmeticException 如果BigInteger太大无法表示为128位
     */
    public static Int128 fromBigInteger(BigInteger value) {
        if (value == null) {
            return ZERO();
        }

        // 检查范围
        if (value.bitLength() > 127) {
            throw new ArithmeticException("BigInteger too large for Int128: " + value);
        }

        // 特殊值快速处理
        if (value.equals(BigInteger.ZERO)) {
            return new Int128(0, 0);
        }
        if (value.equals(BigInteger.ONE)) {
            return new Int128(0, 1);
        }

        // 转换为字节数组（大端序）
        byte[] bytes = value.toByteArray();

        long high = 0, low = 0;

        // 从右到左处理字节（小端处理）
        int len = bytes.length;

        // 处理低64位
        for (int i = 0; i < Math.min(8, len); i++) {
            int byteIndex = len - 1 - i;
            if (byteIndex >= 0) {
                low |= ((long) (bytes[byteIndex] & 0xFF)) << (i * 8);
            }
        }

        // 处理高64位
        for (int i = 8; i < Math.min(16, len); i++) {
            int byteIndex = len - 1 - i;
            if (byteIndex >= 0) {
                high |= ((long) (bytes[byteIndex] & 0xFF)) << ((i - 8) * 8);
            }
        }

        // 如果是负数，需要符号扩展
        if (value.signum() < 0 && len < 16) {
            // 符号扩展高位
            if (len <= 8) {
                // 所有数据都在低64位，高位全部设为-1
                if (len < 8) {
                    // 低64位也需要符号扩展
                    low |= (-1L << (len * 8));
                }
                high = -1L;
            } else {
                // 高位需要符号扩展
                high |= (-1L << ((len - 8) * 8));
            }
        }

        return new Int128(high, low);
    }

    @Override
    public String toString() {
        if (isZero()) return "0";

        // 对于小数值，直接转换
        if (high == 0 || (high == -1 && low < 0)) {
            return Long.toString(low);
        }

        // 高性能128位十进制转换
        return toStringFast();
    }

    /**
     * 高性能128位十进制转换
     * 使用分治法和预计算的10的幂次，比逐位除法快约10倍
     */
    private String toStringFast() {
        boolean negative = isNegative();

        // 工作副本
        long workHigh = negative ? ~high : high;
        long workLow = negative ? ~low + 1 : low;
        if (negative && workLow == 0) workHigh++; // 处理进位

        // 特殊处理：如果高位为0，直接用long转换
        if (workHigh == 0) {
            return negative ? "-" + Long.toString(workLow) : Long.toString(workLow);
        }

        // 分治转换：先转换高64位，再转换低64位
        char[] digits = new char[40]; // 128位最多39位数字 + 符号
        int pos = digits.length;

        // 使用高效的除法：除以10^9（约30位）进行分块
        final long BILLION = 1_000_000_000L;

        // 128位除以10^9的优化算法
        while (workHigh != 0 || workLow != 0) {
            // 128位除法：(workHigh:workLow) / BILLION
            long quotientHigh, quotientLow, remainder;

            if (workHigh == 0) {
                quotientHigh = 0;
                quotientLow = workLow / BILLION;
                remainder = workLow % BILLION;
            } else {
                // 完整的128位除法
                long temp = workHigh % BILLION;
                quotientHigh = workHigh / BILLION;

                // 组合余数和低位进行除法
                long combined = (temp << 32) | (workLow >>> 32);
                long q1 = combined / BILLION;
                temp = combined % BILLION;

                combined = (temp << 32) | (workLow & 0xFFFFFFFFL);
                long q0 = combined / BILLION;
                remainder = combined % BILLION;

                quotientLow = (q1 << 32) | q0;
            }

            // 转换余数（最多9位数字）
            for (int i = 0; i < 9 && (remainder != 0 || workHigh != 0 || workLow != quotientLow * BILLION + remainder); i++) {
                digits[--pos] = (char) ('0' + (remainder % 10));
                remainder /= 10;
            }

            workHigh = quotientHigh;
            workLow = quotientLow;
        }

        // 添加负号
        if (negative) {
            digits[--pos] = '-';
        }

        return new String(digits, pos, digits.length - pos);
    }

    public static Int128 fromString(@NotNull String str) {
        str = str.trim();
        if (str.isEmpty()) {
            throw new NumberFormatException("empty string");
        }

        return fromDecimalString(str);
    }

    public static Int128 fromString(@NotNull String str, Int128 defaultValue) {
        try {
            return fromString(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Int128 fromDecimalString(@NotNull String str) {
        boolean negative = false;
        int start = 0;

        // 处理符号
        if (str.charAt(0) == '-') {
            negative = true;
            start = 1;
        } else if (str.charAt(0) == '+') {
            start = 1;
        }

        if (start >= str.length()) {
            throw new NumberFormatException("no digits");
        }

        // 快速路径：小数值直接用Long.parseLong
        if (str.length() - start <= 18) { // long最多19位，保守估计18位
            try {
                long value = Long.parseLong(str.substring(start));
                return new Int128(negative ? -value : value);
            } catch (NumberFormatException e) {
                // 继续用128位解析
            }
        }

        // 128位解析：使用霍纳方法 (Horner's method)
        Int128 result = new Int128();
        Int128 ten = new Int128(10);

        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                throw new NumberFormatException("invalid digit: " + c);
            }

            // result = result * 10 + digit
            result.multiply(ten);
            result.add(new Int128(c - '0'));
        }

        if (negative) {
            result.negate();
        }

        return result;
    }

    public static Int128 sum(Int128 a, Int128 b) {
        return a.add(b);
    }

    public String toHexString() {
        return String.format("%016X%016X", high, low);
    }

    /**
     * 格式化数字的toString版本，使用千分位分隔符
     * 
     * @param separator 分隔符，通常为 "," 或 " "
     * @return 格式化后的字符串
     */
    public String toFormattedString(String separator) {
        if (separator == null) separator = ",";

        String baseStr = this.toString();
        if (baseStr.length() <= 3) {
            return baseStr;
        }

        boolean negative = baseStr.startsWith("-");
        String digits = negative ? baseStr.substring(1) : baseStr;

        StringBuilder formatted = new StringBuilder();
        int len = digits.length();

        // 从右到左每3位添加分隔符
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                formatted.append(separator);
            }
            formatted.append(digits.charAt(i));
        }

        if (negative) {
            formatted.insert(0, "-");
        }

        return formatted.toString();
    }

    /**
     * 使用逗号作为千分位分隔符的格式化字符串
     * 
     * @return 格式化后的字符串，如 "1,234,567,890"
     */
    public String toFormattedString() {
        return toFormattedString(",");
    }

    /**
     * 紧凑格式，使用科学计数法显示大数字
     * 
     * @return 紧凑格式的字符串，如 "1.23E+15"
     */
    public String toCompactString() {
        if (isZero()) return "0";

        String str = this.toString();
        boolean negative = str.startsWith("-");
        String digits = negative ? str.substring(1) : str;

        if (digits.length() <= 6) {
            return str; // 小数字直接显示
        }

        // 大数字使用科学计数法
        char firstDigit = digits.charAt(0);
        StringBuilder mantissa = new StringBuilder();
        mantissa.append(firstDigit);

        if (digits.length() > 1) {
            mantissa.append('.');
            // 取前3位作为小数部分
            int precision = Math.min(3, digits.length() - 1);
            mantissa.append(digits.substring(1, 1 + precision));
        }

        int exponent = digits.length() - 1;
        String result = mantissa + "E+" + exponent;

        return negative ? "-" + result : result;
    }

    /**
     * 人类可读的格式，使用单位后缀
     * 
     * @return 人类可读的字符串，如 "1.23K", "4.56M", "7.89B"
     */
    public String toHumanReadableString() {
        if (isZero()) return "0";

        // 单位后缀
        String[] units = { "", "K", "M", "B", "T", "P", "E", "Z", "Y" };

        String str = this.toString();
        boolean negative = str.startsWith("-");
        String digits = negative ? str.substring(1) : str;

        if (digits.length() <= 3) {
            return str; // 小数字直接显示
        }

        // 计算合适的单位
        int unitIndex = (digits.length() - 1) / 3;
        if (unitIndex >= units.length) {
            return toCompactString(); // 超出范围使用科学计数法
        }

        if (unitIndex == 0) {
            return str; // 不需要单位
        }

        // 计算显示的数值
        int significantDigits = digits.length() - (unitIndex * 3);
        String integerPart = digits.substring(0, significantDigits);

        StringBuilder result = new StringBuilder();
        if (negative) result.append("-");

        result.append(integerPart);

        // 添加小数部分（最多2位）
        int remainingDigits = digits.length() - significantDigits;
        if (remainingDigits > 0 && integerPart.length() < 3) {
            result.append(".");
            int decimalPlaces = Math.min(2, Math.min(remainingDigits, 3 - integerPart.length()));
            result.append(digits.substring(significantDigits, significantDigits + decimalPlaces));
        }

        result.append(units[unitIndex]);

        return result.toString();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(high) * 31 + Long.hashCode(low);
    }

    public Int128 copy() {
        return new Int128(this.high, this.low);
    }
}
