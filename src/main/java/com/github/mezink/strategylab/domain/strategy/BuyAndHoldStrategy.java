package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.strategy.config.BuyAndHoldConfig;
import com.github.mezink.strategylab.domain.strategy.config.StrategyConfig;
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
 * Buy & Hold strategy: invest all initial capital at the first available close,
 * then hold until the end of the series.
 */
public class BuyAndHoldStrategy implements Strategy {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final BuyAndHoldConfig strategyConfig;

    public BuyAndHoldStrategy(BuyAndHoldConfig strategyConfig) {
        this.strategyConfig = strategyConfig;
    }

    @Override
    public StrategyId id() {
        return StrategyId.BUY_AND_HOLD;
    }

    @Override
    public StrategyConfig config() {
        return strategyConfig;
    }

    @Override
    public StrategyExecution execute(TimeSeries series, BigDecimal initialCapital) {
        List<Candle> candles = series.candles();
        List<EquityPoint> curve = new ArrayList<>();
        List<Trade> trades = new ArrayList<>();

        Candle first = candles.getFirst();
        BigDecimal shares = initialCapital.divide(first.close(), MC);
        trades.add(new Trade(first.date(), TradeAction.BUY, shares, first.close(), "Initial buy — all capital"));

        for (Candle candle : candles) {
            BigDecimal value = shares.multiply(candle.close());
            curve.add(new EquityPoint(candle.date(), value.setScale(2, RoundingMode.HALF_UP)));
        }

        return new StrategyExecution(curve, trades);
    }
}
