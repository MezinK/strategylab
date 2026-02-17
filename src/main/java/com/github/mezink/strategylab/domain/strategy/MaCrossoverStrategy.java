package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.engine.SmaCalculator;
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
 * Moving Average Crossover strategy.
 * <p>
 * When SMA(short) > SMA(long): fully invested.
 * When SMA(short) <= SMA(long): fully in cash.
 * Trades only on signal changes.
 */
public class MaCrossoverStrategy implements Strategy {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final MaCrossoverConfig strategyConfig;

    public MaCrossoverStrategy(MaCrossoverConfig strategyConfig) {
        this.strategyConfig = strategyConfig;
    }

    @Override
    public StrategyId id() {
        return StrategyId.MA_CROSSOVER;
    }

    @Override
    public StrategyConfig config() {
        return strategyConfig;
    }

    @Override
    public StrategyExecution execute(TimeSeries series, BigDecimal initialCapital) {
        int shortWindow = strategyConfig.shortWindow();
        int longWindow = strategyConfig.longWindow();

        List<Candle> candles = series.candles();
        List<BigDecimal> closePrices = candles.stream().map(Candle::close).toList();

        List<BigDecimal> shortSma = SmaCalculator.compute(closePrices, shortWindow);
        List<BigDecimal> longSma = SmaCalculator.compute(closePrices, longWindow);

        List<EquityPoint> curve = new ArrayList<>();
        List<Trade> trades = new ArrayList<>();

        BigDecimal cash = initialCapital;
        BigDecimal shares = BigDecimal.ZERO;
        boolean invested = false;

        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            BigDecimal shortVal = shortSma.get(i);
            BigDecimal longVal = longSma.get(i);

            if (shortVal != null && longVal != null) {
                boolean shouldBeInvested = shortVal.compareTo(longVal) > 0;

                if (shouldBeInvested && !invested) {
                    shares = cash.divide(candle.close(), MC);
                    trades.add(new Trade(candle.date(), TradeAction.BUY, shares, candle.close(),
                            "SMA(%d) crossed above SMA(%d)".formatted(shortWindow, longWindow)));
                    cash = BigDecimal.ZERO;
                    invested = true;
                } else if (!shouldBeInvested && invested) {
                    cash = shares.multiply(candle.close());
                    trades.add(new Trade(candle.date(), TradeAction.SELL, shares, candle.close(),
                            "SMA(%d) crossed below SMA(%d)".formatted(shortWindow, longWindow)));
                    shares = BigDecimal.ZERO;
                    invested = false;
                }
            }

            BigDecimal portfolioValue = invested
                    ? shares.multiply(candle.close())
                    : cash;
            curve.add(new EquityPoint(candle.date(), portfolioValue.setScale(2, RoundingMode.HALF_UP)));
        }

        return new StrategyExecution(curve, trades);
    }
}
