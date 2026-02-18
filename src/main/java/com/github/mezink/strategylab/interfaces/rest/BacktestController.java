package com.github.mezink.strategylab.interfaces.rest;

import com.github.mezink.strategylab.application.RunBacktestUseCase;
import com.github.mezink.strategylab.domain.model.BacktestConfig;
import com.github.mezink.strategylab.domain.model.BacktestResult;
import com.github.mezink.strategylab.interfaces.dto.BacktestRequest;
import com.github.mezink.strategylab.interfaces.dto.BacktestRequestItem;
import com.github.mezink.strategylab.interfaces.dto.BacktestResponse;
import com.github.mezink.strategylab.interfaces.dto.BacktestResultDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {

    private final RunBacktestUseCase runBacktestUseCase;

    public BacktestController(RunBacktestUseCase runBacktestUseCase) {
        this.runBacktestUseCase = runBacktestUseCase;
    }

    @PostMapping
    public ResponseEntity<BacktestResponse> runBacktest(@RequestBody BacktestRequest request) {
        if (request.backtests() == null || request.backtests().isEmpty()) {
            throw new IllegalArgumentException("At least one backtest configuration is required");
        }

        List<BacktestConfig> configs = request.backtests().stream()
                .map(BacktestRequestItem::toDomainConfig)
                .toList();

        List<BacktestResult> results = runBacktestUseCase.execute(configs);

        List<BacktestResultDto> dtos = results.stream()
                .map(BacktestResultDto::from)
                .toList();

        return ResponseEntity.ok(new BacktestResponse(dtos));
    }
}
