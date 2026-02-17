package com.github.mezink.strategylab.infrastructure;

import tools.jackson.databind.ObjectMapper;
import com.github.mezink.strategylab.domain.engine.BacktestEngine;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;
import com.github.mezink.strategylab.domain.strategy.BuyAndHoldStrategy;
import com.github.mezink.strategylab.domain.strategy.DcaStrategy;
import com.github.mezink.strategylab.domain.strategy.MaCrossoverStrategy;
import com.github.mezink.strategylab.domain.strategy.Strategy;
import com.github.mezink.strategylab.infrastructure.cache.CachedMarketDataProvider;
import com.github.mezink.strategylab.infrastructure.yahoo.YahooFinanceMarketDataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.github.mezink.strategylab.application.ListStrategiesUseCase;
import com.github.mezink.strategylab.application.RunBacktestUseCase;
import com.github.mezink.strategylab.application.ValidateInstrumentUseCase;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class AppConfig {

    @Bean
    public MarketDataProvider marketDataProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        var yahoo = new YahooFinanceMarketDataProvider(restClientBuilder, objectMapper);
        return new CachedMarketDataProvider(yahoo);
    }

    @Bean
    public BacktestEngine backtestEngine() {
        return new BacktestEngine();
    }

    @Bean
    public List<Strategy> strategies() {
        return List.of(
                new BuyAndHoldStrategy(),
                new DcaStrategy(),
                new MaCrossoverStrategy()
        );
    }

    @Bean
    public Map<String, Strategy> strategyMap(List<Strategy> strategies) {
        return strategies.stream().collect(Collectors.toMap(Strategy::id, Function.identity()));
    }

    @Bean
    public ListStrategiesUseCase listStrategiesUseCase(List<Strategy> strategies) {
        return new ListStrategiesUseCase(strategies);
    }

    @Bean
    public ValidateInstrumentUseCase validateInstrumentUseCase(MarketDataProvider marketDataProvider) {
        return new ValidateInstrumentUseCase(marketDataProvider);
    }

    @Bean
    public RunBacktestUseCase runBacktestUseCase(
            MarketDataProvider marketDataProvider,
            BacktestEngine backtestEngine,
            Map<String, Strategy> strategyMap
    ) {
        return new RunBacktestUseCase(marketDataProvider, backtestEngine, strategyMap);
    }
}
