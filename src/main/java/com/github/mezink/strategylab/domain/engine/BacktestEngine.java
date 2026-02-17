package com.github.mezink.strategylab.domain.engine;

import com.github.mezink.strategylab.domain.model.BacktestConfig;
import com.github.mezink.strategylab.domain.model.BacktestMetrics;
import com.github.mezink.strategylab.domain.model.BacktestResult;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.model.Trade;
import com.github.mezink.strategylab.domain.strategy.Strategy;
import com.github.mezink.strategylab.domain.strategy.StrategyExecution;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core backtest engine. Runs a strategy on a time series and produces a BacktestResult.
 * This class has NO dependency on Spring or infrastructure.
 */
public class BacktestEngine {

    private static final String DCA_STRATEGY_ID = "dca";

    /**
     * Run a single backtest given a strategy, price data, and configuration.
     */
    public BacktestResult run(Strategy strategy, TimeSeries series, BacktestConfig config) {
        StrategyExecution execution = strategy.execute(series, config.initialCapital(), config.strategyParams());

        BigDecimal totalContributions = computeTotalContributions(config, execution.trades());

        BacktestMetrics metrics = MetricsCalculator.compute(
                execution.equityCurve(),
                totalContributions,
                execution.trades().size()
        );

        return new BacktestResult(
                config.strategyId(),
                config.symbol(),
                execution.equityCurve(),
                execution.trades(),
                metrics
        );
    }

    /**
     * For DCA, total contributions = initial capital + sum of DCA contributions.
     * For other strategies, total contributions = initial capital.
     */
    private BigDecimal computeTotalContributions(BacktestConfig config, List<Trade> trades) {
        if (DCA_STRATEGY_ID.equals(config.strategyId())) {
            BigDecimal contributionAmount = new BigDecimal(
                    config.strategyParams().getOrDefault("contributionAmount", "500"));
            // First trade is initial capital, rest are DCA contributions
            int dcaContributions = Math.max(0, trades.size() - 1);
            return config.initialCapital().add(
                    contributionAmount.multiply(BigDecimal.valueOf(dcaContributions)));
        }
        return config.initialCapital();
    }
}
