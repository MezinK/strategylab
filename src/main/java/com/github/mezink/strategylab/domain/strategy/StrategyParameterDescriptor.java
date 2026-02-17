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
}
