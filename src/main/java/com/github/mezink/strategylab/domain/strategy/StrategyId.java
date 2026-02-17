package com.github.mezink.strategylab.domain.strategy;

import com.github.mezink.strategylab.domain.strategy.config.BuyAndHoldConfig;
import com.github.mezink.strategylab.domain.strategy.config.DcaConfig;
import com.github.mezink.strategylab.domain.strategy.config.MaCrossoverConfig;

import java.util.List;
import java.util.Map;

/**
 * Enumeration of all supported strategy identifiers.
 * Also serves as a factory for creating configured strategy instances
 * and carries static metadata for each strategy type.
 */
public enum StrategyId {

    BUY_AND_HOLD(
            "Buy & Hold",
            "Invest all initial capital at the start date and hold until end. No additional contributions.",
            List.of()
    ),
    DCA(
            "Dollar Cost Averaging (DCA)",
            "Buy a fixed dollar amount every N trading days. No selling. Fractional shares allowed.",
            List.of(
                    new StrategyParameterDescriptor(
                            "contributionAmount", "Dollar amount to invest each period",
                            StrategyParameterDescriptor.TYPE_NUMBER, "500"),
                    new StrategyParameterDescriptor(
                            "frequencyDays", "Trading days between contributions (5=weekly, 21=monthly)",
                            StrategyParameterDescriptor.TYPE_INTEGER, "21")
            )
    ),
    MA_CROSSOVER(
            "Moving Average Crossover",
            "Fully invested when short SMA > long SMA; fully in cash otherwise. Trades only on signal changes.",
            List.of(
                    new StrategyParameterDescriptor(
                            "shortWindow", "Short SMA window (trading days)",
                            StrategyParameterDescriptor.TYPE_INTEGER, "20"),
                    new StrategyParameterDescriptor(
                            "longWindow", "Long SMA window (trading days)",
                            StrategyParameterDescriptor.TYPE_INTEGER, "50")
            )
    );

    private final String label;
    private final String desc;
    private final List<StrategyParameterDescriptor> params;

    StrategyId(String label, String desc, List<StrategyParameterDescriptor> params) {
        this.label = label;
        this.desc = desc;
        this.params = params;
    }

    public String displayName() {
        return label;
    }

    public String description() {
        return desc;
    }

    public List<StrategyParameterDescriptor> parameterDescriptors() {
        return params;
    }

    /**
     * Create a configured strategy instance from the given parameters.
     * Each config implementation validates and throws on missing/invalid values.
     *
     * @param rawParams raw string parameters (may be empty for strategies with no params)
     * @return a fully configured strategy ready for execution
     */
    public Strategy createStrategy(Map<String, String> rawParams) {
        return switch (this) {
            case BUY_AND_HOLD -> new BuyAndHoldStrategy(new BuyAndHoldConfig());
            case DCA -> new DcaStrategy(DcaConfig.fromParams(rawParams));
            case MA_CROSSOVER -> new MaCrossoverStrategy(MaCrossoverConfig.fromParams(rawParams));
        };
    }
}
