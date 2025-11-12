package org.example.tas_backend.controllers;


import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.SubjectAliasDTO;
import org.example.tas_backend.dtos.SubjectTargetDTO;
import org.example.tas_backend.services.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/matcher")
@RequiredArgsConstructor
public class MatcherController {
    private final AiService django;

    @GetMapping("/targets")
    public List<SubjectTargetDTO> listTargets() {
        return django.listTargets();
    }

    @PatchMapping("/targets/{id}")
    public SubjectTargetDTO updateTarget(@PathVariable String id,
                                         @RequestBody SubjectTargetDTO body) {
        return django.updateTarget(id, body);
    }

    @GetMapping("/aliases")
    public List<SubjectAliasDTO> listAliases(@RequestParam(required=false) String language,
                                             @RequestParam(name="target_code", required=false) String targetCode,
                                             @RequestParam(required=false) String q) {
        return django.listAliases(language, targetCode, q);
    }

    @PostMapping("/aliases")
    public SubjectAliasDTO createAlias(@RequestBody SubjectAliasDTO body) {
        return django.createAlias(body);
    }

    @PatchMapping("/aliases/{id}")
    public SubjectAliasDTO updateAlias(@PathVariable String id,
                                       @RequestBody SubjectAliasDTO body) {
        return django.updateAlias(id, body);
    }

    @DeleteMapping("/aliases/{id}")
    public void deleteAlias(@PathVariable String id) {
        django.deleteAlias(id);
    }
}