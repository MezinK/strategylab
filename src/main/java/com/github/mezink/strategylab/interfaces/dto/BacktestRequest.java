package com.github.mezink.strategylab.interfaces.dto;

import java.util.List;

/**
 * Request DTO wrapping multiple backtest configurations for comparison mode.
 */
public record BacktestRequest(
        List<BacktestRequestItem> backtests
) {
}
