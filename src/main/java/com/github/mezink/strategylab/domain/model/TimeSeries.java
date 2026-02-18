package com.github.mezink.strategylab.domain.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * An ordered list of daily candles for a given instrument.
 * Candles are sorted by date ascending (enforced in constructor).
 */
public record TimeSeries(
        Instrument instrument,
        List<Candle> candles
) {
    public TimeSeries {
        if (instrument == null) throw new IllegalArgumentException("instrument must not be null");
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("candles must not be null or empty");
        }
        candles = candles.stream()
                .sorted(Comparator.comparing(Candle::date))
                .toList();
    }

    /**
     * Returns a sub-series filtered to [start, end] inclusive.
     */
    public TimeSeries slice(LocalDate start, LocalDate end) {
        List<Candle> filtered = candles.stream()
                .filter(c -> !c.date().isBefore(start) && !c.date().isAfter(end))
                .toList();
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException(
                    "No candles in range [%s, %s] for %s".formatted(start, end, instrument.symbol()));
        }
        return new TimeSeries(instrument, filtered);
    }

    public LocalDate startDate() {
        return candles.getFirst().date();
    }

    public LocalDate endDate() {
        return candles.getLast().date();
    }

    public int size() {
        return candles.size();
    }

    public Optional<Candle> candleAt(LocalDate date) {
        // Binary-search-friendly since candles are sorted, but linear is fine for prototype
        return candles.stream().filter(c -> c.date().equals(date)).findFirst();
    }
}
