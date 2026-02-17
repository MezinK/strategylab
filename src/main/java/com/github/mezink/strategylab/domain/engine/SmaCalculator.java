package com.github.mezink.strategylab.domain.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Moving Average calculator.
 * Operates on a list of BigDecimal close prices.
 */
public final class SmaCalculator {

    private SmaCalculator() {
    }

    /**
     * Compute the SMA series for the given window.
     * Returns a list of the same size as {@code prices};
     * indices 0..window-2 are null (not enough data).
     *
     * @param closePrices ordered close prices
     * @param window      the SMA window length
     * @return list of SMA values (null where insufficient data)
     */
    public static List<BigDecimal> compute(List<BigDecimal> closePrices, int window) {
        if (window <= 0) throw new IllegalArgumentException("window must be positive");
        if (closePrices.size() < window) {
            throw new IllegalArgumentException(
                    "Need at least %d prices for SMA(%d), got %d".formatted(window, window, closePrices.size()));
        }

        List<BigDecimal> result = new ArrayList<>(closePrices.size());
        BigDecimal sum = BigDecimal.ZERO;

        for (int i = 0; i < closePrices.size(); i++) {
            sum = sum.add(closePrices.get(i));
            if (i < window - 1) {
                result.add(null);
            } else {
                if (i >= window) {
                    sum = sum.subtract(closePrices.get(i - window));
                }
                result.add(sum.divide(BigDecimal.valueOf(window), 6, RoundingMode.HALF_UP));
            }
        }
        return result;
    }
}
