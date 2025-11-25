package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.entities.TargetSubject;
import org.example.tas_backend.repos.TargetSubjectRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/admin/targets")
@RequiredArgsConstructor
public class TargetSubjectController {

    private final TargetSubjectRepo repo;

    @GetMapping
    public List<TargetSubject> list() {
        return repo.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<TargetSubject> create(@RequestBody TargetSubject body) {
        validate(body);
        if (repo.findByCode(body.getCode()).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        TargetSubject saved = repo.save(body);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TargetSubject> update(@PathVariable Long id, @RequestBody TargetSubject body) {
        return repo.findById(id)
                .map(existing -> {
                    if (StringUtils.hasText(body.getName())) existing.setName(body.getName());
                    if (body.getCoefficient() != null) existing.setCoefficient(body.getCoefficient());
                    if (StringUtils.hasText(body.getCode())) existing.setCode(body.getCode());
                    validate(existing);
                    return ResponseEntity.ok(repo.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void validate(TargetSubject t) {
        if (!StringUtils.hasText(t.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        }
        if (!StringUtils.hasText(t.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
    }
}
