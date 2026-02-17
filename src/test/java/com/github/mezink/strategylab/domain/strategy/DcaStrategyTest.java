package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.model.EquityPoint;
import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.model.Trade;
import com.github.mezink.strategylab.domain.model.TradeAction;
import com.github.mezink.strategylab.domain.strategy.config.DcaConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DcaStrategyTest {

    @Test
    void idAndMetadata() {
        DcaStrategy strategy = new DcaStrategy(new DcaConfig(bd(500), 5));
        assertEquals(StrategyId.DCA, strategy.id());
        assertFalse(StrategyId.DCA.parameterDescriptors().isEmpty());
    }

    @Test
    void configIsAccessible() {
        DcaConfig config = new DcaConfig(bd(500), 5);
        DcaStrategy strategy = new DcaStrategy(config);
        assertSame(config, strategy.config());
    }

    @Test
    void investsInitialCapitalOnFirstDay() {
        TimeSeries series = createSeries(10);
        DcaStrategy strategy = new DcaStrategy(new DcaConfig(bd(500), 5));
        StrategyExecution result = strategy.execute(series, bd(1000));

        // First trade should be the initial investment
        assertFalse(result.trades().isEmpty());
        Trade first = result.trades().getFirst();
        assertEquals(TradeAction.BUY, first.action());
        assertTrue(first.reason().contains("Initial"));
    }

    @Test
    void makesDcaContributionsAtFrequency() {
        // 11 days, frequency=5 -> initial buy + 2 DCA contributions (day 5 and day 10)
        TimeSeries series = createSeries(11);
        DcaStrategy strategy = new DcaStrategy(new DcaConfig(bd(500), 5));
        StrategyExecution result = strategy.execute(series, bd(1000));

        assertEquals(3, result.trades().size(), "Should have initial + 2 DCA buys");
        // All should be BUY
        assertTrue(result.trades().stream().allMatch(t -> t.action() == TradeAction.BUY));
    }

    @Test
    void equityCurveGrowsWithContributions() {
        TimeSeries series = createConstantPriceSeries(10, 100.0);
        DcaStrategy strategy = new DcaStrategy(new DcaConfig(bd(500), 5));
        StrategyExecution result = strategy.execute(series, bd(1000));

        List<EquityPoint> curve = result.equityCurve();
        // With constant price, equity should grow at each contribution
        BigDecimal lastValue = curve.getLast().portfolioValue();
        BigDecimal firstValue = curve.getFirst().portfolioValue();
        assertTrue(lastValue.compareTo(firstValue) > 0,
                "Equity should grow with contributions");
    }

    @Test
    void totalContributionsCalculation() {
        DcaConfig config = new DcaConfig(bd(500), 5);
        BigDecimal total = config.totalContributions(bd(1000), 3);
        // 1000 + 2 * 500 = 2000 (tradeCount=3: 1 initial + 2 DCA)
        assertEquals(0, bd(2000).compareTo(total));
    }

    private static TimeSeries createSeries(int days) {
        return createConstantPriceSeries(days, 100.0);
    }

    private static TimeSeries createConstantPriceSeries(int days, double price) {
        Instrument inst = new Instrument("TEST", "Test Stock", "EQUITY");
        List<Candle> candles = new ArrayList<>();
        LocalDate date = LocalDate.of(2020, 1, 1);
        for (int i = 0; i < days; i++) {
            BigDecimal p = bd(price);
            candles.add(new Candle(date, p, p, p, p, 1000L));
            date = date.plusDays(1);
        }
        return new TimeSeries(inst, candles);
    }

    private static BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }
}
