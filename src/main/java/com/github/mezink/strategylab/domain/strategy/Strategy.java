package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.TimeSeries;

import java.math.BigDecimal;

/**
 * Strategy pattern interface. Each hardcoded strategy implements this.
 * <p>
 * Strategies are created per-request with their configuration. The strategy
 * receives the price series and initial capital and produces an equity curve
 * and trade list. Metrics are computed externally by the engine.
 * <p>
 * Static metadata (displayName, description, parameterDescriptors) lives on
 * {@link StrategyId} to avoid duplication.
 */
public interface Strategy {

    /**
     * Unique identifier for this strategy.
     */
    StrategyId id();

    /**
     * The typed configuration for this strategy instance.
     */
    StrategyConfig config();

    /**
     * Execute the strategy on the given time series.
     *
     * @param series         daily price data
     * @param initialCapital starting cash
     * @return the result of the execution (equity curve + trades)
     */
    StrategyExecution execute(TimeSeries series, BigDecimal initialCapital);
}
