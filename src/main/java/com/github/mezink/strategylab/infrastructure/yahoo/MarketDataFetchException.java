package com.github.mezink.strategylab.infrastructure.yahoo;

/**
 * Thrown when market data cannot be fetched or parsed.
 */
public class MarketDataFetchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MarketDataFetchException(String message) {
        super(message);
    }

    public MarketDataFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
