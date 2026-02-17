package com.github.mezink.strategylab.application;

import com.github.mezink.strategylab.domain.engine.BacktestEngine;
import com.github.mezink.strategylab.domain.model.BacktestConfig;
import com.github.mezink.strategylab.domain.model.BacktestResult;
import com.github.mezink.strategylab.domain.model.TimeSeries;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case: run one or more backtests (comparison mode).
 * Fetches data, runs each strategy, returns all results.
 */
public class RunBacktestUseCase {

    private final MarketDataProvider marketDataProvider;
    private final BacktestEngine engine;

    public RunBacktestUseCase(MarketDataProvider marketDataProvider, BacktestEngine engine) {
        this.marketDataProvider = marketDataProvider;
        this.engine = engine;
    }

    public List<BacktestResult> execute(List<BacktestConfig> configs) {
        List<BacktestResult> results = new ArrayList<>();

        for (BacktestConfig config : configs) {
            TimeSeries series = marketDataProvider.getDailySeries(
                    config.symbol(), config.startDate(), config.endDate());

            BacktestResult result = engine.run(series, config);
            results.add(result);
        }

        return results;
    }
}
