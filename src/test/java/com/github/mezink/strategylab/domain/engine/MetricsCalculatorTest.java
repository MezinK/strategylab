package com.github.mezink.strategylab.domain.engine;

import com.github.mezink.strategylab.domain.model.BacktestMetrics;
import com.github.mezink.strategylab.domain.model.EquityPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCalculatorTest {

    @Test
    void cagrForDoublingInOneYear() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(10000)),
                new EquityPoint(LocalDate.of(2021, 1, 1), bd(20000))
        );
        BigDecimal cagr = MetricsCalculator.computeCAGR(curve);
        // Should be close to 1.0 (100%)
        assertTrue(cagr.doubleValue() > 0.99 && cagr.doubleValue() < 1.01,
                "CAGR for doubling in 1 year should be ~100%, got: " + cagr);
    }

    @Test
    void cagrForFlatReturn() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(10000)),
                new EquityPoint(LocalDate.of(2021, 1, 1), bd(10000))
        );
        BigDecimal cagr = MetricsCalculator.computeCAGR(curve);
        assertEquals(0, bd(0).compareTo(cagr), "CAGR for flat return should be 0");
    }

    @Test
    void maxDrawdownWithClearPeakToTrough() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(100)),
                new EquityPoint(LocalDate.of(2020, 2, 1), bd(200)),  // peak
                new EquityPoint(LocalDate.of(2020, 3, 1), bd(100)),  // 50% drawdown
                new EquityPoint(LocalDate.of(2020, 4, 1), bd(150))
        );
        BigDecimal dd = MetricsCalculator.computeMaxDrawdown(curve);
        assertEquals(0, bd(0.5).compareTo(dd), "Max drawdown should be 50%, got: " + dd);
    }

    @Test
    void maxDrawdownAlwaysRising() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(100)),
                new EquityPoint(LocalDate.of(2020, 2, 1), bd(200)),
                new EquityPoint(LocalDate.of(2020, 3, 1), bd(300))
        );
        BigDecimal dd = MetricsCalculator.computeMaxDrawdown(curve);
        assertEquals(0, bd(0).compareTo(dd), "Drawdown for always-rising should be 0");
    }

    @Test
    void volatilityIsNonNegative() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(100)),
                new EquityPoint(LocalDate.of(2020, 1, 2), bd(102)),
                new EquityPoint(LocalDate.of(2020, 1, 3), bd(98)),
                new EquityPoint(LocalDate.of(2020, 1, 4), bd(105)),
                new EquityPoint(LocalDate.of(2020, 1, 5), bd(101))
        );
        BigDecimal vol = MetricsCalculator.annualizedVolatilityFrom(
                MetricsCalculator.computeDailyReturns(curve));
        assertTrue(vol.doubleValue() >= 0, "Volatility should be non-negative");
        assertTrue(vol.doubleValue() > 0, "Volatility for varying prices should be positive");
    }

    @Test
    void sharpeIsZeroWhenReturnsAreConstant() {
        // When all daily returns are the same, stddev = 0, so Sharpe should be 0.
        // Use powers of 2 so that each return is exactly 1.0 (100%) in floating point.
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(100)),
                new EquityPoint(LocalDate.of(2020, 1, 2), bd(200)),
                new EquityPoint(LocalDate.of(2020, 1, 3), bd(400)),
                new EquityPoint(LocalDate.of(2020, 1, 4), bd(800))
        );
        List<Double> returns = MetricsCalculator.computeDailyReturns(curve);
        BigDecimal sharpe = MetricsCalculator.sharpeFrom(returns);
        assertEquals(0, bd(0).compareTo(sharpe),
                "Sharpe should be 0 when all returns are identical (zero vol), got: " + sharpe);
    }

    @Test
    void sharpeUsesArithmeticMeanReturn() {
        // Build a curve with known daily returns that produce different CAGR vs arithmetic mean
        List<EquityPoint> curve = new ArrayList<>();
        LocalDate date = LocalDate.of(2020, 1, 1);
        // Volatile path: up 10%, down 10%, up 10%, down 10%, ...
        double value = 10000;
        curve.add(new EquityPoint(date, bd(value)));
        for (int i = 1; i <= 252; i++) {
            value = (i % 2 == 1) ? value * 1.02 : value * 0.98;
            curve.add(new EquityPoint(date.plusDays(i), bd(value)));
        }

        List<Double> returns = MetricsCalculator.computeDailyReturns(curve);
        BigDecimal newSharpe = MetricsCalculator.sharpeFrom(returns);

        // Old method: CAGR / volatility (geometric / arithmetic mix)
        BigDecimal cagr = MetricsCalculator.computeCAGR(curve);
        BigDecimal vol = MetricsCalculator.annualizedVolatilityFrom(returns);
        BigDecimal oldSharpe = (vol.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO
                : cagr.divide(vol, new java.math.MathContext(16, java.math.RoundingMode.HALF_UP))
                .setScale(6, java.math.RoundingMode.HALF_UP);

        // The new Sharpe (arithmetic mean / vol) should differ from old (CAGR / vol)
        assertNotEquals(0, newSharpe.compareTo(oldSharpe),
                "New Sharpe (arithmetic) should differ from old Sharpe (CAGR-based). " +
                        "New: " + newSharpe + ", Old: " + oldSharpe);
    }

    @Test
    void fullMetricsComputation() {
        List<EquityPoint> curve = new ArrayList<>();
        LocalDate date = LocalDate.of(2020, 1, 1);
        // Generate a simple uptrend
        for (int i = 0; i < 252; i++) {
            double value = 10000 * (1 + 0.0004 * i); // ~10% annual
            curve.add(new EquityPoint(date.plusDays(i), bd(value)));
        }

        BacktestMetrics metrics = MetricsCalculator.compute(curve, bd(10000), 1);

        assertNotNull(metrics.finalValue());
        assertNotNull(metrics.totalReturnPct());
        assertNotNull(metrics.cagr());
        assertNotNull(metrics.maxDrawdown());
        assertNotNull(metrics.annualizedVolatility());
        assertNotNull(metrics.sharpeRatio());
        assertEquals(1, metrics.numberOfTrades());
        assertEquals(0, bd(10000).compareTo(metrics.totalContributions()));
    }

    @Test
    void totalReturnPctComputedCorrectly() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(10000)),
                new EquityPoint(LocalDate.of(2021, 1, 1), bd(12000))
        );
        // With DCA contributions totaling $15000, return = (12000-15000)/15000 = -20%
        BacktestMetrics metrics = MetricsCalculator.compute(curve, bd(15000), 5);
        assertEquals(-0.2, metrics.totalReturnPct().doubleValue(), 0.001);
    }

    @Test
    void totalReturnPctPositiveForProfit() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(10000)),
                new EquityPoint(LocalDate.of(2021, 1, 1), bd(12000))
        );
        BacktestMetrics metrics = MetricsCalculator.compute(curve, bd(10000), 1);
        assertEquals(0.2, metrics.totalReturnPct().doubleValue(), 0.001);
    }

    @Test
    void throwsForTooFewPoints() {
        List<EquityPoint> curve = List.of(
                new EquityPoint(LocalDate.of(2020, 1, 1), bd(100))
        );
        BigDecimal capital = bd(100);
        assertThrows(IllegalArgumentException.class,
                () -> MetricsCalculator.compute(curve, capital, 0));
    }

    private static BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }
}
