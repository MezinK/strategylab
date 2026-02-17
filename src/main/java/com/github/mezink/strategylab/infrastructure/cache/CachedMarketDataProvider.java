package com.github.mezink.strategylab.infrastructure.cache;

import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching decorator for MarketDataProvider. Stores results in memory keyed by
 * symbol + start + end + interval.
 */
public class CachedMarketDataProvider implements MarketDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CachedMarketDataProvider.class);

    private final MarketDataProvider delegate;
    private final Map<String, TimeSeries> seriesCache = new ConcurrentHashMap<>();
    private final Map<String, Optional<Instrument>> symbolCache = new ConcurrentHashMap<>();

    public CachedMarketDataProvider(MarketDataProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public TimeSeries getDailySeries(String symbol, LocalDate start, LocalDate end) {
        String key = cacheKey(symbol, start, end);
        return seriesCache.computeIfAbsent(key, k -> {
            LOG.info("Cache miss for series: {}", k);
            return delegate.getDailySeries(symbol, start, end);
        });
    }

    @Override
    public Optional<Instrument> validateSymbol(String symbol) {
        String key = symbol.toUpperCase(Locale.ROOT);
        return symbolCache.computeIfAbsent(key, k -> {
            LOG.info("Cache miss for symbol validation: {}", k);
            return delegate.validateSymbol(symbol);
        });
    }

    private static String cacheKey(String symbol, LocalDate start, LocalDate end) {
        return "%s:%s:%s:1d".formatted(symbol.toUpperCase(Locale.ROOT), start, end);
    }
}
