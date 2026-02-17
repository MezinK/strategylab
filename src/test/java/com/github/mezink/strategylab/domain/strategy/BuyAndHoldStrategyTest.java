package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.model.EquityPoint;
import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.model.Trade;
import com.github.mezink.strategylab.domain.model.TradeAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuyAndHoldStrategyTest {

    private final BuyAndHoldStrategy strategy = new BuyAndHoldStrategy(new BuyAndHoldConfig());

    @Test
    void idAndMetadata() {
        assertEquals(StrategyId.BUY_AND_HOLD, strategy.id());
        assertNotNull(StrategyId.BUY_AND_HOLD.displayName());
        assertNotNull(StrategyId.BUY_AND_HOLD.description());
    }

    @Test
    void configIsAccessible() {
        assertNotNull(strategy.config());
        assertInstanceOf(BuyAndHoldConfig.class, strategy.config());
    }

    @Test
    void investsAllCapitalAtStart() {
        TimeSeries series = createSimpleSeries(List.of(100.0, 110.0, 120.0));
        StrategyExecution result = strategy.execute(series, bd(10000));

        assertEquals(1, result.trades().size());
        Trade trade = result.trades().getFirst();
        assertEquals(TradeAction.BUY, trade.action());
        // 10000 / 100 = 100 shares
        assertEquals(0, bd(100).compareTo(trade.quantity()));
    }

    @Test
    void equityCurveReflectsHolding() {
        TimeSeries series = createSimpleSeries(List.of(100.0, 200.0, 50.0));
        StrategyExecution result = strategy.execute(series, bd(10000));

        List<EquityPoint> curve = result.equityCurve();
        assertEquals(3, curve.size());
        // 100 shares * 100 = 10000
        assertEquals(0, bd(10000).compareTo(curve.get(0).portfolioValue()));
        // 100 shares * 200 = 20000
        assertEquals(0, bd(20000).compareTo(curve.get(1).portfolioValue()));
        // 100 shares * 50 = 5000
        assertEquals(0, bd(5000).compareTo(curve.get(2).portfolioValue()));
    }

    private static TimeSeries createSimpleSeries(List<Double> closePrices) {
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
