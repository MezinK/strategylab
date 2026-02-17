package com.github.mezink.strategylab.domain.strategy;

/**
 * Describes a parameter that a strategy requires.
 */
public record StrategyParameterDescriptor(
        String name,
        String description,
        String type,
        String defaultValue
) {
    /** Parameter type for whole numbers. */
    public static final String TYPE_INTEGER = "integer";

    /** Parameter type for decimal numbers. */
    public static final String TYPE_NUMBER = "number";
}
