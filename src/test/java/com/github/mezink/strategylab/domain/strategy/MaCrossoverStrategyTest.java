package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.model.Trade;
import com.github.mezink.strategylab.domain.model.TradeAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaCrossoverStrategyTest {

    private static final String SHORT_WINDOW = "shortWindow";
    private static final String LONG_WINDOW = "longWindow";
    private final MaCrossoverStrategy strategy = new MaCrossoverStrategy();

    @Test
    void idAndMetadata() {
        assertEquals("ma_crossover", strategy.id());
        assertFalse(strategy.parameterDescriptors().isEmpty());
    }

    @Test
    void throwsIfShortWindowNotLessThanLong() {
        TimeSeries series = createTrendingSeries(100, 50.0, 0.5);
        BigDecimal capital = bd(10000);
        Map<String, String> params = Map.of(SHORT_WINDOW, "20", LONG_WINDOW, "20");
        assertThrows(IllegalArgumentException.class, () ->
                strategy.execute(series, capital, params));
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
        StrategyExecution result = strategy.execute(series, bd(10000), Map.of(
                SHORT_WINDOW, "5",
                LONG_WINDOW, "15"
        ));

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
        StrategyExecution result = strategy.execute(series, bd(10000), Map.of(
                SHORT_WINDOW, "5",
                LONG_WINDOW, "20"
        ));

        assertEquals(series.candles().size(), result.equityCurve().size());
    }

    @Test
    void tradesHaveReasonStrings() {
        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < 30; i++) prices.add(50.0 + i * 2);
        for (int i = 0; i < 30; i++) prices.add(108.0 - i * 3);
        for (int i = 0; i < 30; i++) prices.add(20.0 + i * 2);

        TimeSeries series = createSeriesFromPrices(prices);
        StrategyExecution result = strategy.execute(series, bd(10000), Map.of(
                SHORT_WINDOW, "5",
                LONG_WINDOW, "15"
        ));

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
