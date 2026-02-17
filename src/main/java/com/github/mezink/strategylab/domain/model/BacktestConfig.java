package com.github.mezink.strategylab.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Configuration for a single backtest run.
 */
public record BacktestConfig(
        String symbol,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialCapital,
        String strategyId,
        java.util.Map<String, String> strategyParams
) {
    public BacktestConfig {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol required");
        if (startDate == null) throw new IllegalArgumentException("startDate required");
        if (endDate == null) throw new IllegalArgumentException("endDate required");
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("initialCapital must be positive");
        }
        if (strategyId == null || strategyId.isBlank()) throw new IllegalArgumentException("strategyId required");
        if (strategyParams == null) {
            strategyParams = java.util.Map.of();
        } else {
            strategyParams = java.util.Map.copyOf(strategyParams);
        }
    }
}
