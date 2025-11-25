package org.example.tas_backend.controllers;


import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.SubjectAliasDTO;
import org.example.tas_backend.dtos.SubjectTargetDTO;
import org.example.tas_backend.services.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/matcher")
@RequiredArgsConstructor
@Slf4j
public class MatcherController {
    private final AiService django;

    @GetMapping("/targets")
    public List<SubjectTargetDTO> listTargets() {
        log.debug("Admin list targets");
        return django.listTargets();
    }

    @PatchMapping("/targets/{id}")
    public SubjectTargetDTO updateTarget(@PathVariable String id,
                                         @RequestBody SubjectTargetDTO body) {
        log.debug("Admin update target id={} payload={}", id, body);
        return django.updateTarget(id, body);
    }

    @PostMapping("/targets")
    public SubjectTargetDTO createTarget(@RequestBody SubjectTargetDTO body) {
        log.debug("Admin create target payload={}", body);
        return django.createTarget(body);
    }

    @DeleteMapping("/targets/{id}")
    public void deleteTarget(@PathVariable String id) {
        log.debug("Admin delete target id={}", id);
        django.deleteTarget(id);
    }

    @GetMapping("/aliases")
    public List<SubjectAliasDTO> listAliases(@RequestParam(required=false) String language,
                                             @RequestParam(name="target_code", required=false) String targetCode,
                                             @RequestParam(required=false) String q) {
        log.debug("Admin list aliases language={} targetCode={} q={}", language, targetCode, q);
        return django.listAliases(language, targetCode, q);
    }

    @PostMapping("/aliases")
    public SubjectAliasDTO createAlias(@RequestBody SubjectAliasDTO body) {
        log.debug("Admin create alias payload={}", body);
        return django.createAlias(body);
    }

    @PatchMapping("/aliases/{id}")
    public SubjectAliasDTO updateAlias(@PathVariable String id,
                                       @RequestBody SubjectAliasDTO body) {
        log.debug("Admin update alias id={} payload={}", id, body);
        return django.updateAlias(id, body);
    }

    @DeleteMapping("/aliases/{id}")
    public void deleteAlias(@PathVariable String id) {
        log.debug("Admin delete alias id={}", id);
        django.deleteAlias(id);
    }
}
