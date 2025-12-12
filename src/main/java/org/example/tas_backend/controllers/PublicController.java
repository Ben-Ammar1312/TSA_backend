package org.example.tas_backend.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PublicController {

    @GetMapping("/public/zap-test")
    public Map<String, String> zapTest() {
        return Map.of("status", "OK");
    }
}