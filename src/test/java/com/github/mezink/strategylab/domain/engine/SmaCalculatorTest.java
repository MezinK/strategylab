package com.github.mezink.strategylab.domain.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SmaCalculatorTest {

    @Test
    void computeSmaWithExactWindowSize() {
        List<BigDecimal> prices = List.of(bd(10), bd(20), bd(30));
        List<BigDecimal> sma = SmaCalculator.compute(prices, 3);

        assertEquals(3, sma.size());
        assertNull(sma.get(0));
        assertNull(sma.get(1));
        assertEquals(0, bd(20).compareTo(sma.get(2)), "SMA(3) of [10,20,30] = 20");
    }

    @Test
    void computeSmaRollingWindow() {
        // Prices: 1, 2, 3, 4, 5
        // SMA(3): null, null, 2, 3, 4
        List<BigDecimal> prices = List.of(bd(1), bd(2), bd(3), bd(4), bd(5));
        List<BigDecimal> sma = SmaCalculator.compute(prices, 3);

        assertEquals(5, sma.size());
        assertNull(sma.get(0));
        assertNull(sma.get(1));
        assertEquals(0, bd(2).compareTo(sma.get(2)));
        assertEquals(0, bd(3).compareTo(sma.get(3)));
        assertEquals(0, bd(4).compareTo(sma.get(4)));
    }

    @Test
    void computeSmaWindowOfOne() {
        List<BigDecimal> prices = List.of(bd(5), bd(10), bd(15));
        List<BigDecimal> sma = SmaCalculator.compute(prices, 1);

        assertEquals(0, bd(5).compareTo(sma.get(0)));
        assertEquals(0, bd(10).compareTo(sma.get(1)));
        assertEquals(0, bd(15).compareTo(sma.get(2)));
    }

    @Test
    void throwsForWindowLargerThanData() {
        List<BigDecimal> prices = List.of(bd(1), bd(2));
        assertThrows(IllegalArgumentException.class, () -> SmaCalculator.compute(prices, 5));
    }

    @Test
    void throwsForZeroWindow() {
        List<BigDecimal> prices = List.of(bd(1));
        assertThrows(IllegalArgumentException.class, () -> SmaCalculator.compute(prices, 0));
    }

    private static BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }
}
