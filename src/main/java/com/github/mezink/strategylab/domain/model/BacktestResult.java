package com.github.mezink.strategylab.domain.model;

import java.util.List;

/**
 * The full output of running a single backtest: equity curve, trades, and computed metrics.
 */
public record BacktestResult(
        String strategyId,
        String symbol,
        List<EquityPoint> equityCurve,
        List<Trade> trades,
        BacktestMetrics metrics
) {
}
