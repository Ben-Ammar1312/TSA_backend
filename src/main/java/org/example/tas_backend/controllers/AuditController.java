package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.RevisionSummaryDTO;
import org.example.tas_backend.entities.*;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.example.tas_backend.services.AuditHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditHistoryService svc;
    private final StudentApplicantRepo studentRepo;

    // Register which fields you care about per entity
    private static final Map<Class<?>, List<String>> TYPES = Map.of(
            StudentApplicant.class, List.of(
                    "firstName","lastName","email","phoneNumber","gender","dateOfBirth",
                    "nationalID","address"
            ),
            Application.class, List.of(
                    "preferredProgram","intakePeriod","languageLevel","status","decisionBy","decisionDate"
            ),
            Document.class, List.of("type","filename","storageKey","mimeType","sizeBytes","rawText"),
            Interview.class, List.of("interviewDate","interviewLink","result","notes","interviewerName"),
            Evaluation.class, List.of("equivalenceScore","aiComments","status","confidence","scoreMaxPossible"),
            ExtractedSubject.class, List.of("rawName","rawScore","rawScale","year","sourceCoefficient"),
            SubjectMapping.class, List.of("confidence","auto","normalizedScore","targetSubject.id","extractedSubject.id"),
            TargetSubject.class, List.of("code","name","coefficient")
    );

    @GetMapping("/{entity}/{id}/history")
    @PreAuthorize("hasAuthority('ROLE_staff')")
    public List<RevisionSummaryDTO> history(@PathVariable String entity, @PathVariable String id) {
        Class<?> type = resolve(entity);
        Object typedId = Long.parseLong(id);
        return svc.historyOf(type, typedId, TYPES.getOrDefault(type, List.of()));
    }

    @GetMapping("/feed")
    @PreAuthorize("hasAuthority('ROLE_staff')")
    public List<RevisionSummaryDTO> feed(@RequestParam(defaultValue = "20") int perType) {
        int n = Math.min(Math.max(perType, 1), 100);
        return svc.latestChanges(TYPES, n);
    }

    @GetMapping("/me/student/history")
    @PreAuthorize("hasAuthority('ROLE_student')")
    public List<RevisionSummaryDTO> myStudentHistory(JwtAuthenticationToken auth) {
        String sub = auth.getToken().getSubject();
        Long id = studentRepo.findIdByKeycloakSub(sub)
                .orElseThrow(() -> new NoSuchElementException("profile not found"));
        return svc.historyOf(StudentApplicant.class, id, TYPES.get(StudentApplicant.class));
    }

    private Class<?> resolve(String e) {
        e = e.trim().toLowerCase();
        return switch (e) {
            case "studentapplicant","student" -> StudentApplicant.class;
            case "application","app" -> Application.class;
            case "document","doc" -> Document.class;
            case "interview" -> Interview.class;
            case "evaluation","eval" -> Evaluation.class;
            case "extractedsubject","exsubject" -> ExtractedSubject.class;
            case "subjectmapping","mapping" -> SubjectMapping.class;
            case "targetsubject","subject" -> TargetSubject.class;
            default -> throw new IllegalArgumentException("unknown entity: " + e);
        };
    }
}