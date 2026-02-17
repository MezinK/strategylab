package com.github.mezink.strategylab.domain.engine;

import com.github.mezink.strategylab.domain.model.*;
import com.github.mezink.strategylab.domain.strategy.BuyAndHoldStrategy;
import com.github.mezink.strategylab.domain.strategy.DcaStrategy;
import com.github.mezink.strategylab.domain.strategy.MaCrossoverStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BacktestEngineTest {

    private static final String TEST_SYMBOL = "TEST";
    private final BacktestEngine engine = new BacktestEngine();

    @Test
    void runBuyAndHold() {
        TimeSeries series = createTrendingSeries(100, 100.0, 0.5);
        BacktestConfig config = new BacktestConfig(
                TEST_SYMBOL, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 4, 9),
                BigDecimal.valueOf(10000), "buy_and_hold", Map.of()
        );

        BacktestResult result = engine.run(new BuyAndHoldStrategy(), series, config);

        assertEquals("buy_and_hold", result.strategyId());
        assertEquals(TEST_SYMBOL, result.symbol());
        assertNotNull(result.equityCurve());
        assertNotNull(result.metrics());
        assertTrue(result.metrics().finalValue().compareTo(BigDecimal.valueOf(10000)) > 0,
                "Final value should be > initial for uptrending series");
        assertEquals(1, result.metrics().numberOfTrades());
    }

    @Test
    void runDcaComputesTotalContributions() {
        TimeSeries series = createConstantPriceSeries(30, 100.0);
        BacktestConfig config = new BacktestConfig(
                TEST_SYMBOL, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 30),
                BigDecimal.valueOf(5000), "dca",
                Map.of("contributionAmount", "1000", "frequencyDays", "10")
        );

        BacktestResult result = engine.run(new DcaStrategy(), series, config);

        // initial(5000) + 2 DCA contributions (day 10, day 20) * 1000 = 7000
        assertTrue(result.metrics().totalContributions().compareTo(BigDecimal.valueOf(5000)) > 0,
                "Total contributions should include DCA amounts");
    }

    @Test
    void runMaCrossoverProducesTradesAndMetrics() {
        // Create a series with enough data points for SMA computation
        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < 30; i++) prices.add(50.0 + i * 2);
        for (int i = 0; i < 30; i++) prices.add(108.0 - i * 3);
        for (int i = 0; i < 30; i++) prices.add(20.0 + i * 2);

        TimeSeries series = createSeriesFromPrices(prices);
        BacktestConfig config = new BacktestConfig(
                TEST_SYMBOL, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 4, 1),
                BigDecimal.valueOf(10000), "ma_crossover",
                Map.of("shortWindow", "5", "longWindow", "15")
        );

        BacktestResult result = engine.run(new MaCrossoverStrategy(), series, config);

        assertNotNull(result.metrics());
        assertTrue(result.trades().size() > 0, "MA crossover should produce trades");
        assertEquals(BigDecimal.valueOf(10000), result.metrics().totalContributions());
    }

    private static TimeSeries createTrendingSeries(int days, double startPrice, double dailyChange) {
        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            prices.add(startPrice + dailyChange * i);
        }
        return createSeriesFromPrices(prices);
    }

    private static TimeSeries createConstantPriceSeries(int days, double price) {
        List<Double> prices = new ArrayList<>();
        for (int i = 0; i < days; i++) prices.add(price);
        return createSeriesFromPrices(prices);
    }

    private static TimeSeries createSeriesFromPrices(List<Double> closePrices) {
        Instrument inst = new Instrument(TEST_SYMBOL, "Test Stock", "EQUITY");
        List<Candle> candles = new ArrayList<>();
        LocalDate date = LocalDate.of(2020, 1, 1);
        for (double close : closePrices) {
            BigDecimal p = BigDecimal.valueOf(close);
            candles.add(new Candle(date, p, p, p, p, 1000L));
            date = date.plusDays(1);
        }
        return new TimeSeries(inst, candles);
    }
}
