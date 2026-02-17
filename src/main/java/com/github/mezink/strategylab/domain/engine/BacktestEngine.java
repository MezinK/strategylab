package com.github.mezink.strategylab.domain.engine;

import com.github.mezink.strategylab.domain.model.BacktestConfig;
import com.github.mezink.strategylab.domain.model.BacktestMetrics;
import com.github.mezink.strategylab.domain.model.BacktestResult;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.strategy.Strategy;
import com.github.mezink.strategylab.domain.strategy.StrategyExecution;

import java.math.BigDecimal;

/**
 * Core backtest engine. Runs a strategy on a time series and produces a BacktestResult.
 * This class has NO dependency on Spring or infrastructure.
 */
public class BacktestEngine {

    /**
     * Run a single backtest given price data and configuration.
     * The strategy is obtained from the config itself.
     */
    public BacktestResult run(TimeSeries series, BacktestConfig config) {
        Strategy strategy = config.strategy();
        StrategyExecution execution = strategy.execute(series, config.initialCapital());

        BigDecimal totalContributions = strategy.config()
                .totalContributions(config.initialCapital(), execution.trades().size());

        BacktestMetrics metrics = MetricsCalculator.compute(
                execution.equityCurve(),
                totalContributions,
                execution.trades().size()
        );

        return new BacktestResult(
                strategy.id(),
                config.symbol(),
                execution.equityCurve(),
                execution.trades(),
                metrics
        );
    }
}
