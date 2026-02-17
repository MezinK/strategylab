package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.model.EquityPoint;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.model.Trade;
import com.github.mezink.strategylab.domain.model.TradeAction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dollar Cost Averaging strategy: invest a fixed amount at regular intervals.
 * Fractional shares allowed.
 */
public class DcaStrategy implements Strategy {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final int INITIAL_TRADE_COUNT = 1;

    @Override
    public String id() {
        return "dca";
    }

    @Override
    public String displayName() {
        return "Dollar Cost Averaging (DCA)";
    }

    @Override
    public String description() {
        return "Buy a fixed dollar amount every N trading days. No selling. Fractional shares allowed.";
    }

    @Override
    public List<StrategyParameterDescriptor> parameterDescriptors() {
        return List.of(
                new StrategyParameterDescriptor(
                        "contributionAmount",
                        "Dollar amount to invest each period",
                        "number",
                        "500"
                ),
                new StrategyParameterDescriptor(
                        "frequencyDays",
                        "Number of trading days between contributions (e.g., 5 = weekly, 21 = monthly)",
                        "integer",
                        "21"
                )
        );
    }

    @Override
    public StrategyExecution execute(TimeSeries series, BigDecimal initialCapital, Map<String, String> params) {
        BigDecimal contributionAmount = new BigDecimal(params.getOrDefault("contributionAmount", "500"));
        int frequencyDays = Integer.parseInt(params.getOrDefault("frequencyDays", "21"));

        List<Candle> candles = series.candles();
        List<EquityPoint> curve = new ArrayList<>();
        List<Trade> trades = new ArrayList<>();

        BigDecimal cash = initialCapital;
        BigDecimal shares = BigDecimal.ZERO;
        int daysSinceLastContribution = frequencyDays; // so first day triggers a buy

        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);

            // On the very first day, invest initial capital as a DCA contribution
            if (i == 0) {
                // Buy with initial capital
                BigDecimal sharesToBuy = cash.divide(candle.close(), MC);
                shares = shares.add(sharesToBuy);
                trades.add(new Trade(candle.date(), TradeAction.BUY, sharesToBuy, candle.close(),
                        "Initial investment of " + cash.toPlainString()));
                cash = BigDecimal.ZERO;
                daysSinceLastContribution = 0;
            } else {
                daysSinceLastContribution++;
                if (daysSinceLastContribution >= frequencyDays) {
                    // Add contribution and buy
                    cash = cash.add(contributionAmount);
                    BigDecimal sharesToBuy = cash.divide(candle.close(), MC);
                    shares = shares.add(sharesToBuy);
                    trades.add(new Trade(candle.date(), TradeAction.BUY, sharesToBuy, candle.close(),
                            "DCA contribution of " + contributionAmount.toPlainString()));
                    cash = BigDecimal.ZERO;
                    daysSinceLastContribution = 0;
                }
            }

            BigDecimal portfolioValue = shares.multiply(candle.close()).add(cash);
            curve.add(new EquityPoint(candle.date(), portfolioValue.setScale(2, RoundingMode.HALF_UP)));
        }

        return new StrategyExecution(curve, trades);
    }

    /**
     * Returns the total contributions (initial capital + all DCA contributions).
     * Useful for metrics.
     */
    public static BigDecimal computeTotalContributions(BigDecimal initialCapital, List<Trade> trades, BigDecimal contributionAmount) {
        // First trade is initial capital; subsequent trades are DCA contributions
        if (trades.size() <= INITIAL_TRADE_COUNT) return initialCapital;
        return initialCapital.add(contributionAmount.multiply(BigDecimal.valueOf((long) trades.size() - INITIAL_TRADE_COUNT)));
    }
}
