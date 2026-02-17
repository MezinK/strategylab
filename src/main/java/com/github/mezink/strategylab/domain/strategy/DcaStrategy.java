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

/**
 * Dollar Cost Averaging strategy: invest a fixed amount at regular intervals.
 * Fractional shares allowed.
 */
public class DcaStrategy implements Strategy {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final DcaConfig strategyConfig;

    public DcaStrategy(DcaConfig strategyConfig) {
        this.strategyConfig = strategyConfig;
    }

    @Override
    public StrategyId id() {
        return StrategyId.DCA;
    }

    @Override
    public StrategyConfig config() {
        return strategyConfig;
    }

    @Override
    public StrategyExecution execute(TimeSeries series, BigDecimal initialCapital) {
        BigDecimal contributionAmount = strategyConfig.contributionAmount();
        int frequencyDays = strategyConfig.frequencyDays();

        List<Candle> candles = series.candles();
        List<EquityPoint> curve = new ArrayList<>();
        List<Trade> trades = new ArrayList<>();

        BigDecimal cash = initialCapital;
        BigDecimal shares = BigDecimal.ZERO;
        int daysSinceLastContribution = frequencyDays;

        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);

            if (i == 0) {
                BigDecimal sharesToBuy = cash.divide(candle.close(), MC);
                shares = shares.add(sharesToBuy);
                trades.add(new Trade(candle.date(), TradeAction.BUY, sharesToBuy, candle.close(),
                        "Initial investment of " + cash.toPlainString()));
                cash = BigDecimal.ZERO;
                daysSinceLastContribution = 0;
            } else {
                daysSinceLastContribution++;
                if (daysSinceLastContribution >= frequencyDays) {
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
}
