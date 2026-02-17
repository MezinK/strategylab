package com.github.mezink.strategylab.domain.strategy;

import java.math.BigDecimal;

/**
 * Sealed interface for strategy-specific configuration.
 * Each strategy defines a concrete record with typed, validated fields.
 * <p>
 * The {@link #totalContributions(BigDecimal, int)} method lets the engine compute
 * metrics without needing to know which strategy was used.
 */
public sealed interface StrategyConfig permits BuyAndHoldConfig, DcaConfig, MaCrossoverConfig {

    /**
     * Compute total capital contributed over the backtest period.
     * For strategies without additional contributions, this equals the initial capital.
     *
     * @param initialCapital the starting capital
     * @param tradeCount     the number of trades executed
     * @return the total contributions
     */
    BigDecimal totalContributions(BigDecimal initialCapital, int tradeCount);
}
