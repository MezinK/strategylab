package com.github.mezink.strategylab.domain.model;

import com.github.mezink.strategylab.domain.strategy.Strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Configuration for a single backtest run.
 * The Strategy carries both its identifier and typed configuration.
 */
public record BacktestConfig(
        String symbol,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialCapital,
        Strategy strategy
) {
    public BacktestConfig {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol required");
        if (startDate == null) throw new IllegalArgumentException("startDate required");
        if (endDate == null) throw new IllegalArgumentException("endDate required");
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("initialCapital must be positive");
        }
        if (strategy == null) throw new IllegalArgumentException("strategy required");
    }
}
