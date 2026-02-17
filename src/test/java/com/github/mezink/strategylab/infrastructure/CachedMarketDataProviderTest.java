package com.github.mezink.strategylab.infrastructure;

import com.github.mezink.strategylab.domain.model.Candle;
import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;
import com.github.mezink.strategylab.infrastructure.cache.CachedMarketDataProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CachedMarketDataProviderTest {

    private static final String EQUITY_TYPE = "EQUITY";

    @Test
    void cachesSeriesOnSecondCall() {
        AtomicInteger fetchCount = new AtomicInteger(0);

        MarketDataProvider delegate = new MarketDataProvider() {
            @Override
            public TimeSeries getDailySeries(String symbol, LocalDate start, LocalDate end) {
                fetchCount.incrementAndGet();
                Instrument inst = new Instrument(symbol, symbol, EQUITY_TYPE);
                Candle candle = new Candle(start, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, 100);
                return new TimeSeries(inst, List.of(candle));
            }

            @Override
            public Optional<Instrument> validateSymbol(String symbol) {
                fetchCount.incrementAndGet();
                return Optional.of(new Instrument(symbol, symbol, EQUITY_TYPE));
            }
        };

        CachedMarketDataProvider cached = new CachedMarketDataProvider(delegate);
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2020, 1, 31);

        // First call => delegate is called
        TimeSeries first = cached.getDailySeries("SPY", start, end);
        assertEquals(1, fetchCount.get());

        // Second call => cache hit, delegate NOT called
        TimeSeries second = cached.getDailySeries("SPY", start, end);
        assertEquals(1, fetchCount.get());

        assertSame(first, second);
    }

    @Test
    void cachesSymbolValidation() {
        AtomicInteger fetchCount = new AtomicInteger(0);

        MarketDataProvider delegate = new MarketDataProvider() {
            @Override
            public TimeSeries getDailySeries(String symbol, LocalDate start, LocalDate end) {
                return null;
            }

            @Override
            public Optional<Instrument> validateSymbol(String symbol) {
                fetchCount.incrementAndGet();
                return Optional.of(new Instrument(symbol, symbol, EQUITY_TYPE));
            }
        };

        CachedMarketDataProvider cached = new CachedMarketDataProvider(delegate);

        cached.validateSymbol("AAPL");
        cached.validateSymbol("AAPL");
        assertEquals(1, fetchCount.get(), "Should only fetch once for same symbol");
    }

    @Test
    void differentKeysCallDelegateSeparately() {
        AtomicInteger fetchCount = new AtomicInteger(0);

        MarketDataProvider delegate = new MarketDataProvider() {
            @Override
            public TimeSeries getDailySeries(String symbol, LocalDate start, LocalDate end) {
                fetchCount.incrementAndGet();
                Instrument inst = new Instrument(symbol, symbol, EQUITY_TYPE);
                Candle candle = new Candle(start, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, 100);
                return new TimeSeries(inst, List.of(candle));
            }

            @Override
            public Optional<Instrument> validateSymbol(String symbol) {
                return Optional.empty();
            }
        };

        CachedMarketDataProvider cached = new CachedMarketDataProvider(delegate);
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2020, 1, 31);

        cached.getDailySeries("SPY", start, end);
        cached.getDailySeries("AAPL", start, end);
        assertEquals(2, fetchCount.get(), "Different symbols should each call delegate");
    }
}
