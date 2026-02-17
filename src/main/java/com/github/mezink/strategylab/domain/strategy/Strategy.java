package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.TimeSeries;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strategy pattern interface. Each hardcoded strategy implements this.
 * <p>
 * The strategy receives the price series and configuration and produces
 * an equity curve and trade list. Metrics are computed externally.
 */
public interface Strategy {

    /**
     * Unique identifier for this strategy (e.g., "dca", "buy_and_hold", "ma_crossover").
     */
    String id();

    /**
     * Human-readable name.
     */
    String displayName();

    /**
     * Description of what this strategy does.
     */
    String description();

    /**
     * Returns the list of parameters this strategy accepts,
     * so the UI knows what to render.
     */
    List<StrategyParameterDescriptor> parameterDescriptors();

    /**
     * Execute the strategy on the given time series.
     *
     * @param series         daily price data
     * @param initialCapital starting cash
     * @param params         strategy-specific parameters (e.g., contributionAmount, frequency)
     * @return the result of the execution (equity curve + trades)
     */
    StrategyExecution execute(TimeSeries series, BigDecimal initialCapital, Map<String, String> params);
}
