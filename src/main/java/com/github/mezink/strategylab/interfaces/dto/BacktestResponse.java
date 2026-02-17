package com.github.mezink.strategylab.interfaces.dto;

import java.util.List;

/**
 * Response DTO wrapping multiple backtest results.
 */
public record BacktestResponse(
        List<BacktestResultDto> results
) {
}
