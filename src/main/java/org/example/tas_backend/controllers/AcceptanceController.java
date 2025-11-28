package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.entities.AcceptanceRule;
import org.example.tas_backend.services.AcceptanceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/acceptance")
@RequiredArgsConstructor
public class AcceptanceController {

    private final AcceptanceService acceptanceService;

    @GetMapping
    public AcceptanceRule getRule() {
        return acceptanceService.getRule();
    }

    @PostMapping
    public AcceptanceRule update(@RequestBody AcceptanceRule body) {
        int threshold = body.getThresholdCount();
        return acceptanceService.updateThreshold(threshold);
    }
}
