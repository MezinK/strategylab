package com.github.mezink.strategylab.application;

import com.github.mezink.strategylab.domain.engine.BacktestEngine;
import com.github.mezink.strategylab.domain.model.BacktestConfig;
import com.github.mezink.strategylab.domain.model.BacktestResult;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;
import com.github.mezink.strategylab.domain.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Use case: run one or more backtests (comparison mode).
 * Fetches data, runs each strategy, returns all results.
 */
public class RunBacktestUseCase {

    private final MarketDataProvider marketDataProvider;
    private final BacktestEngine engine;
    private final Map<String, Strategy> strategyMap;

    public RunBacktestUseCase(MarketDataProvider marketDataProvider, BacktestEngine engine, Map<String, Strategy> strategyMap) {
        this.marketDataProvider = marketDataProvider;
        this.engine = engine;
        this.strategyMap = strategyMap;
    }

    public List<BacktestResult> execute(List<BacktestConfig> configs) {
        List<BacktestResult> results = new ArrayList<>();

        for (BacktestConfig config : configs) {
            Strategy strategy = strategyMap.get(config.strategyId());
            if (strategy == null) {
                throw new IllegalArgumentException("Unknown strategy: " + config.strategyId());
            }

            TimeSeries series = marketDataProvider.getDailySeries(
                    config.symbol(), config.startDate(), config.endDate());

            BacktestResult result = engine.run(strategy, series, config);
            results.add(result);
        }

        return results;
    }
}
