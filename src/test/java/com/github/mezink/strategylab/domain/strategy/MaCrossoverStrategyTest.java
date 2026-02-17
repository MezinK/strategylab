package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.model.Trade;
import com.github.mezink.strategylab.domain.model.TradeAction;
import com.github.mezink.strategylab.domain.strategy.config.MaCrossoverConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaCrossoverStrategyTest {

    @Test
    void idAndMetadata() {
        MaCrossoverStrategy strategy = new MaCrossoverStrategy(new MaCrossoverConfig(5, 20));
        assertEquals(StrategyId.MA_CROSSOVER, strategy.id());
        assertFalse(StrategyId.MA_CROSSOVER.parameterDescriptors().isEmpty());
    }

    @Test
    void configIsAccessible() {
        MaCrossoverConfig config = new MaCrossoverConfig(5, 20);
        MaCrossoverStrategy strategy = new MaCrossoverStrategy(config);
        assertSame(config, strategy.config());
    }

    @Test
    void throwsIfShortWindowNotLessThanLong() {
        assertThrows(IllegalArgumentException.class, () ->
                new MaCrossoverConfig(20, 20));
    }

    @Test
    void producesTradesOnCrossover() {
        // Create a series that goes up (bullish) then down (bearish) then up again
        // This should produce at least buy and sell trades
        List<Double> prices = new ArrayList<>();
        // Start low, go high (bullish crossover)
        for (int i = 0; i < 30; i++) prices.add(50.0 + i * 2);
        // Go down sharply (bearish crossover)
        for (int i = 0; i < 30; i++) prices.add(108.0 - i * 3);
        // Go up again (bullish crossover)
        for (int i = 0; i < 30; i++) prices.add(20.0 + i * 2);

        TimeSeries series = createSeriesFromPrices(prices);
        MaCrossoverStrategy strategy = new MaCrossoverStrategy(new MaCrossoverConfig(5, 15));
        StrategyExecution result = strategy.execute(series, bd(10000));

        assertFalse(result.trades().isEmpty(), "Should have trades when SMA crossovers occur");

        // Should have both buys and sells
        boolean hasBuy = result.trades().stream().anyMatch(t -> t.action() == TradeAction.BUY);
        boolean hasSell = result.trades().stream().anyMatch(t -> t.action() == TradeAction.SELL);
        assertTrue(hasBuy, "Should have at least one BUY trade");
        assertTrue(hasSell, "Should have at least one SELL trade");
    }

    @Test
    void equityCurveHasSameSizeAsCandles() {
        TimeSeries series = createTrendingSeries(60, 100.0, 0.1);
        MaCrossoverStrategy strategy = new MaCrossoverStrategy(new MaCrossoverConfig(5, 20));
        StrategyExecution result = strategy.execute(series, bd(10000));

        assertEquals(series.candles().size(), result.equityCurve().size());
    }

    @Test
    void tradesHaveReasonStrings() {
        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < 30; i++) prices.add(50.0 + i * 2);
        for (int i = 0; i < 30; i++) prices.add(108.0 - i * 3);
        for (int i = 0; i < 30; i++) prices.add(20.0 + i * 2);

        TimeSeries series = createSeriesFromPrices(prices);
        MaCrossoverStrategy strategy = new MaCrossoverStrategy(new MaCrossoverConfig(5, 15));
        StrategyExecution result = strategy.execute(series, bd(10000));

        for (Trade trade : result.trades()) {
            assertNotNull(trade.reason());
            assertTrue(trade.reason().contains("SMA"), "Trade reason should mention SMA");
        }
    }

    private static TimeSeries createTrendingSeries(int days, double startPrice, double dailyChange) {
        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            prices.add(startPrice + dailyChange * i);
        }
        return createSeriesFromPrices(prices);
    }

    private static TimeSeries createSeriesFromPrices(List<Double> closePrices) {
        Instrument inst = new Instrument("TEST", "Test Stock", "EQUITY");
        List<Candle> candles = new ArrayList<>();
        LocalDate date = LocalDate.of(2020, 1, 1);
        for (double close : closePrices) {
            BigDecimal p = bd(close);
            candles.add(new Candle(date, p, p, p, p, 1000L));
            date = date.plusDays(1);
        }
        return new TimeSeries(inst, candles);
    }

    private static BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }
}
