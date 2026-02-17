package com.github.mezink.strategylab.interfaces.dto;

import com.github.mezink.strategylab.domain.model.BacktestMetrics;
import com.github.mezink.strategylab.domain.model.BacktestResult;
import com.github.mezink.strategylab.domain.model.EquityPoint;
import com.github.mezink.strategylab.domain.model.Trade;

import java.util.List;

/**
 * Response DTO for a single backtest result.
 */
public record BacktestResultDto(
        String strategyId,
        String symbol,
        List<EquityPoint> equityCurve,
        List<Trade> trades,
        BacktestMetrics metrics
) {
    public static BacktestResultDto from(BacktestResult result) {
        return new BacktestResultDto(
                result.strategyId(),
                result.symbol(),
                result.equityCurve(),
                result.trades(),
                result.metrics()
        );
    }
}
