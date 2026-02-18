package com.github.mezink.strategylab.domain.model;

import java.math.BigDecimal;

/**
 * Computed performance metrics for a single backtest run.
 */
public record BacktestMetrics(
        BigDecimal finalValue,
        BigDecimal totalContributions,
        BigDecimal netReturnPct,
        BigDecimal cagr,
        BigDecimal maxDrawdown,
        BigDecimal annualizedVolatility,
        BigDecimal sharpeRatio,
        int numberOfTrades
) {
}
