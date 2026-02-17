package com.github.mezink.strategylab.infrastructure;

import tools.jackson.databind.ObjectMapper;
import com.github.mezink.strategylab.domain.engine.BacktestEngine;
import com.github.mezink.strategylab.domain.port.MarketDataProvider;
import com.github.mezink.strategylab.infrastructure.cache.CachedMarketDataProvider;
import com.github.mezink.strategylab.infrastructure.yahoo.YahooFinanceMarketDataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.github.mezink.strategylab.application.ListStrategiesUseCase;
import com.github.mezink.strategylab.application.RunBacktestUseCase;
import com.github.mezink.strategylab.application.ValidateInstrumentUseCase;

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
    public ListStrategiesUseCase listStrategiesUseCase() {
        return new ListStrategiesUseCase();
    }

    @Bean
    public ValidateInstrumentUseCase validateInstrumentUseCase(MarketDataProvider marketDataProvider) {
        return new ValidateInstrumentUseCase(marketDataProvider);
    }

    @Bean
    public RunBacktestUseCase runBacktestUseCase(
            MarketDataProvider marketDataProvider,
            BacktestEngine backtestEngine
    ) {
        return new RunBacktestUseCase(marketDataProvider, backtestEngine);
    }
}
