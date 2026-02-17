package com.github.mezink.strategylab.interfaces.dto;

import com.github.mezink.strategylab.domain.model.BacktestConfig;
import com.github.mezink.strategylab.domain.strategy.Strategy;
import com.github.mezink.strategylab.domain.strategy.StrategyId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for a single backtest configuration.
 * Handles its own conversion to the domain model via {@link #toDomainConfig()}.
 */
public record BacktestRequestItem(
        String symbol,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialCapital,
        String strategyId,
        Map<String, String> strategyParams
) {

    /**
     * Convert this wire-format DTO into a domain {@link BacktestConfig}.
     * Parses the strategy identifier, creates a configured strategy instance,
     * and assembles the domain config.
     *
     * @return a fully validated domain config
     * @throws IllegalArgumentException if strategyId is unknown or params are invalid
     */
    public BacktestConfig toDomainConfig() {
        StrategyId id = StrategyId.valueOf(strategyId);
        Strategy strategy = id.createStrategy(strategyParams != null ? strategyParams : Map.of());
        return new BacktestConfig(symbol, startDate, endDate, initialCapital, strategy);
    }
}
