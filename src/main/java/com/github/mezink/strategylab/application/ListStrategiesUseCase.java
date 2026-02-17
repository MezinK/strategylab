package com.github.mezink.strategylab.application;

import com.github.mezink.strategylab.domain.strategy.Strategy;
import com.github.mezink.strategylab.domain.strategy.StrategyParameterDescriptor;

import java.util.List;

/**
 * Use case: list available strategies and their parameter descriptors.
 */
public class ListStrategiesUseCase {

    private final List<Strategy> strategies;

    public ListStrategiesUseCase(List<Strategy> strategies) {
        this.strategies = strategies;
    }

    public List<StrategyInfo> execute() {
        return strategies.stream()
                .map(s -> new StrategyInfo(s.id(), s.displayName(), s.description(), s.parameterDescriptors()))
                .toList();
    }

    public record StrategyInfo(
            String id,
            String displayName,
            String description,
            List<StrategyParameterDescriptor> parameters
    ) {
    }
}
