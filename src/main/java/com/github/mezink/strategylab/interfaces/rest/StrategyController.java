package com.github.mezink.strategylab.interfaces.rest;

import com.github.mezink.strategylab.application.ListStrategiesUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final ListStrategiesUseCase listStrategiesUseCase;

    public StrategyController(ListStrategiesUseCase listStrategiesUseCase) {
        this.listStrategiesUseCase = listStrategiesUseCase;
    }

    @GetMapping
    public List<ListStrategiesUseCase.StrategyInfo> list() {
        return listStrategiesUseCase.execute();
    }
}
