package com.github.mezink.strategylab.domain.engine;

import com.github.mezink.strategylab.domain.model.BacktestMetrics;
import com.github.mezink.strategylab.domain.model.EquityPoint;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes performance metrics from an equity curve.
 * All math uses BigDecimal for clarity; precision is prototype-grade.
 */
public final class MetricsCalculator {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private MetricsCalculator() {
    }

    public static BacktestMetrics compute(
            List<EquityPoint> equityCurve,
            BigDecimal totalContributions,
            int numberOfTrades
    ) {
        if (equityCurve == null || equityCurve.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 equity points to compute metrics");
        }

        BigDecimal finalValue = equityCurve.getLast().portfolioValue();
        BigDecimal cagr = computeCAGR(equityCurve);
        BigDecimal maxDrawdown = computeMaxDrawdown(equityCurve);
        BigDecimal volatility = computeAnnualizedVolatility(equityCurve);
        BigDecimal sharpe = computeSharpe(cagr, volatility);

        return new BacktestMetrics(
                finalValue,
                totalContributions,
                cagr,
                maxDrawdown,
                volatility,
                sharpe,
                numberOfTrades
        );
    }

    /**
     * CAGR = (finalValue / initialValue)^(1/years) - 1
     */
    static BigDecimal computeCAGR(List<EquityPoint> curve) {
        BigDecimal initial = curve.getFirst().portfolioValue();
        BigDecimal last = curve.getLast().portfolioValue();
        if (initial.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        long days = ChronoUnit.DAYS.between(curve.getFirst().date(), curve.getLast().date());
        if (days <= 0) return BigDecimal.ZERO;

        double years = days / 365.25;
        double ratio = last.doubleValue() / initial.doubleValue();
        if (ratio <= 0) return BigDecimal.ZERO;

        double cagr = Math.pow(ratio, 1.0 / years) - 1.0;
        return BigDecimal.valueOf(cagr).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Max drawdown = max peak-to-trough decline.
     * Returned as a positive fraction (e.g., 0.25 = 25% drawdown).
     */
    static BigDecimal computeMaxDrawdown(List<EquityPoint> curve) {
        BigDecimal peak = curve.getFirst().portfolioValue();
        BigDecimal maxDd = BigDecimal.ZERO;

        for (EquityPoint point : curve) {
            BigDecimal value = point.portfolioValue();
            if (value.compareTo(peak) > 0) {
                peak = value;
            }
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dd = peak.subtract(value).divide(peak, MC);
                if (dd.compareTo(maxDd) > 0) {
                    maxDd = dd;
                }
            }
        }
        return maxDd.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Annualized volatility = stddev(daily returns) * sqrt(252).
     */
    static BigDecimal computeAnnualizedVolatility(List<EquityPoint> curve) {
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < curve.size(); i++) {
            double prev = curve.get(i - 1).portfolioValue().doubleValue();
            double curr = curve.get(i).portfolioValue().doubleValue();
            if (prev > 0) {
                dailyReturns.add((curr - prev) / prev);
            }
        }
        if (dailyReturns.isEmpty()) return BigDecimal.ZERO;

        double mean = dailyReturns.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = dailyReturns.stream()
                .mapToDouble(d -> (d - mean) * (d - mean))
                .average()
                .orElse(0.0);
        double stddev = Math.sqrt(variance);
        double annualized = stddev * Math.sqrt(252);

        return BigDecimal.valueOf(annualized).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Sharpe ratio (rf = 0) = CAGR / annualized volatility.
     */
    static BigDecimal computeSharpe(BigDecimal cagr, BigDecimal volatility) {
        if (volatility.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return cagr.divide(volatility, MC).setScale(6, RoundingMode.HALF_UP);
    }
}
