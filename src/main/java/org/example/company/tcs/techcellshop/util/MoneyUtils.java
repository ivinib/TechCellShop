package org.example.company.tcs.techcellshop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyUtils() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
        return normalize(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public static BigDecimal subtractFloorZero(BigDecimal amount, BigDecimal discount) {
        BigDecimal result = normalize(amount).subtract(normalize(discount));
        return result.compareTo(BigDecimal.ZERO) < 0 ? zero() : normalize(result);
    }
}