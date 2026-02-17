package com.github.mezink.strategylab.application;

import com.github.mezink.strategylab.domain.model.Instrument;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;

import java.util.Optional;

/**
 * Use case: validate that a ticker symbol exists.
 */
public class ValidateInstrumentUseCase {

    private final MarketDataProvider marketDataProvider;

    public ValidateInstrumentUseCase(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    public Optional<Instrument> execute(String symbol) {
        return marketDataProvider.validateSymbol(symbol);
    }
}
