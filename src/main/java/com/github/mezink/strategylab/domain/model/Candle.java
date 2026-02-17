package com.github.mezink.strategylab.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single daily price candle for an instrument.
 */
public record Candle(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
    public Candle {
        if (date == null) throw new IllegalArgumentException("date must not be null");
        if (close == null) throw new IllegalArgumentException("close must not be null");
    }
}
