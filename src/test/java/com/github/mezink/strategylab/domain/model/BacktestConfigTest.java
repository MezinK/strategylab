package com.github.mezink.strategylab.domain.model;

import com.github.mezink.strategylab.domain.strategy.BuyAndHoldStrategy;
import com.github.mezink.strategylab.domain.strategy.config.BuyAndHoldConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BacktestConfigTest {

    @Test
    void rejectsStartDateAfterEndDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new BacktestConfig(
                        "SPY",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2023, 1, 1),
                        BigDecimal.valueOf(10000),
                        new BuyAndHoldStrategy(new BuyAndHoldConfig())
                ));
    }

    @Test
    void rejectsStartDateEqualsEndDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new BacktestConfig(
                        "SPY",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 1, 1),
                        BigDecimal.valueOf(10000),
                        new BuyAndHoldStrategy(new BuyAndHoldConfig())
                ));
    }

    @Test
    void acceptsValidConfig() {
        assertDoesNotThrow(() ->
                new BacktestConfig(
                        "SPY",
                        LocalDate.of(2023, 1, 1),
                        LocalDate.of(2024, 1, 1),
                        BigDecimal.valueOf(10000),
                        new BuyAndHoldStrategy(new BuyAndHoldConfig())
                ));
    }
}
