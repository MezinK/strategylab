package com.github.mezink.strategylab.domain.strategy;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Configuration for the Moving Average Crossover strategy.
 *
 * @param shortWindow short SMA window in trading days
 * @param longWindow  long SMA window in trading days
 */
public record MaCrossoverConfig(
        int shortWindow,
        int longWindow
) implements StrategyConfig {

    public MaCrossoverConfig {
        if (shortWindow <= 0) {
            throw new IllegalArgumentException("shortWindow must be a positive integer");
        }
        if (longWindow <= 0) {
            throw new IllegalArgumentException("longWindow must be a positive integer");
        }
        if (shortWindow >= longWindow) {
            throw new IllegalArgumentException("shortWindow must be less than longWindow");
        }
    }

    /**
     * Parse from raw request parameters. Throws on missing or invalid values.
     */
    public static MaCrossoverConfig fromParams(Map<String, String> params) {
        String shortStr = params.get("shortWindow");
        if (shortStr == null || shortStr.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: shortWindow");
        }

        String longStr = params.get("longWindow");
        if (longStr == null || longStr.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: longWindow");
        }

        int shortWin;
        try {
            shortWin = Integer.parseInt(shortStr);
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("shortWindow must be a valid integer: " + shortStr);
        }

        int longWin;
        try {
            longWin = Integer.parseInt(longStr);
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("longWindow must be a valid integer: " + longStr);
        }

        return new MaCrossoverConfig(shortWin, longWin);
    }

    @Override
    public BigDecimal totalContributions(BigDecimal initialCapital, int tradeCount) {
        return initialCapital;
    }
}
