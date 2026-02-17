package com.github.mezink.strategylab.domain.strategy.config;

import java.math.BigDecimal;

/**
 * Configuration for the Buy & Hold strategy.
 * This strategy takes no additional parameters.
 */
public record BuyAndHoldConfig() implements StrategyConfig {

    @Override
    public BigDecimal totalContributions(BigDecimal initialCapital, int tradeCount) {
        return initialCapital;
    }
}
