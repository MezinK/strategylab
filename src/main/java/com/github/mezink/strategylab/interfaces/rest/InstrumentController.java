package com.github.mezink.strategylab.interfaces.rest;

import com.github.mezink.strategylab.application.ValidateInstrumentUseCase;
import com.github.mezink.strategylab.domain.model.Instrument;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final ValidateInstrumentUseCase validateInstrumentUseCase;

    public InstrumentController(ValidateInstrumentUseCase validateInstrumentUseCase) {
        this.validateInstrumentUseCase = validateInstrumentUseCase;
    }

    @GetMapping("/validate")
    public ResponseEntity<Instrument> validate(@RequestParam String symbol) {
        Optional<Instrument> result = validateInstrumentUseCase.execute(symbol);

        return result
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Symbol not found or not fetchable: " + symbol));
    }
}
