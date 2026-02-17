package com.github.mezink.strategylab.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for a single backtest configuration.
 */
public record BacktestRequestItem(
        String symbol,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialCapital,
        String strategyId,
        Map<String, String> strategyParams
) {
}
