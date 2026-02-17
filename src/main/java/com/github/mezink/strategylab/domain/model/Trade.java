package com.github.mezink.strategylab.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single trade executed during a backtest.
 */
public record Trade(
        LocalDate date,
        TradeAction action,
        BigDecimal quantity,
        BigDecimal price,
        String reason
) {
}
