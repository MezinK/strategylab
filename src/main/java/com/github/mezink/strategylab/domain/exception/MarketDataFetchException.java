package com.github.mezink.strategylab.domain.exception;

/**
 * Thrown when market data cannot be fetched or parsed.
 * Lives in domain so all layers can reference it without cross-layer coupling.
 */
public class MarketDataFetchException extends RuntimeException {

    public MarketDataFetchException(String message) {
        super(message);
    }

    public MarketDataFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
