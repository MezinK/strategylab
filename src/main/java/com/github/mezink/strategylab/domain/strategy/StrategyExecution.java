package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.EquityPoint;
import com.github.mezink.strategylab.domain.model.Trade;

import java.util.List;

/**
 * The output of a single strategy execution (before metric computation).
 */
public record StrategyExecution(
        List<EquityPoint> equityCurve,
        List<Trade> trades
) {
}
