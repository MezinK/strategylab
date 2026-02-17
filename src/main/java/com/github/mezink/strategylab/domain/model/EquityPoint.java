package com.github.mezink.strategylab.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single point on the equity curve (portfolio value at a given date).
 */
public record EquityPoint(
        LocalDate date,
        BigDecimal portfolioValue
) {
}
