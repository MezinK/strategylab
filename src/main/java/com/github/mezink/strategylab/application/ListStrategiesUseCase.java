package com.github.mezink.strategylab.application;

import com.github.mezink.strategylab.domain.strategy.StrategyId;
import com.github.mezink.strategylab.domain.strategy.StrategyParameterDescriptor;

import java.util.Arrays;
import java.util.List;

/**
 * Use case: list available strategies and their parameter descriptors.
 * Reads static metadata from the {@link StrategyId} enum.
 */
public class ListStrategiesUseCase {

    public List<StrategyInfo> execute() {
        return Arrays.stream(StrategyId.values())
                .map(id -> new StrategyInfo(
                        id,
                        id.displayName(),
                        id.description(),
                        id.parameterDescriptors()
                ))
                .toList();
    }

    public record StrategyInfo(
            StrategyId id,
            String displayName,
            String description,
            List<StrategyParameterDescriptor> parameters
    ) {
    }
}
