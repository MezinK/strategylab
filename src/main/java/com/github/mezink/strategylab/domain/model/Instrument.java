package com.github.mezink.strategylab.domain.model;

/**
 * Represents a tradeable instrument (stock, ETF, crypto, etc.).
 */
public record Instrument(
        String symbol,
        String name,
        String assetType
) {
    public Instrument {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be null or blank");
        }
    }
}
