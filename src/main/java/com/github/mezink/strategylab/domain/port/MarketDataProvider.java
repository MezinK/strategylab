package com.github.mezink.strategylab.domain.port;

import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.model.TimeSeries;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Port for fetching market data. Infrastructure layer provides the implementation.
 */
public interface MarketDataProvider {

    /**
     * Fetch daily candles for the given instrument in [start, end].
     */
    TimeSeries getDailySeries(String symbol, LocalDate start, LocalDate end);

    /**
     * Validate that a symbol exists and return basic instrument info.
     */
    Optional<Instrument> validateSymbol(String symbol);
}
