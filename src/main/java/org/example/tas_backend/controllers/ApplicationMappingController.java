package org.example.tas_backend.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.ApplicationDecisionDTO;
import org.example.tas_backend.dtos.ApplicationMappingViewDTO;
import org.example.tas_backend.dtos.ApplicationSummaryDTO;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.entities.AcceptanceRule;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.DocumentRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.example.tas_backend.services.MappingQueryService;
import org.example.tas_backend.services.AcceptanceService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ApplicationMappingController {

    private final MappingQueryService mappingQueryService;
    private final ApplicationRepo applicationRepo;
    private final DocumentRepo documentRepo;
    private final StudentApplicantRepo studentRepo;
    private final AcceptanceService acceptanceService;
    private final SimpMessagingTemplate broker;

    // -------- Student endpoints --------

    @GetMapping("/student/applications/latest/mappings")
    public ApplicationMappingViewDTO latestForStudent(@AuthenticationPrincipal Jwt jwt) {
        return mappingQueryService.latestForStudent(jwt.getSubject());
    }

    @GetMapping("/student/applications/latest/summary")
    public ApplicationSummaryDTO latestSummaryForStudent(@AuthenticationPrincipal Jwt jwt) {
        String sub = jwt.getSubject();

        // Primary: query by keycloakSub to avoid failures when student snapshot is missing
        Optional<Application> latestBySub = applicationRepo.findTopByStudent_KeycloakSubOrderByIdDesc(sub);

        // Fallback: resolve student entity then search
        Optional<Application> latestByEntity = studentRepo.findByKeycloakSub(sub)
                .flatMap(applicationRepo::findTopByStudentOrderByIdDesc);

        Application app = latestBySub.orElseGet(() -> latestByEntity.orElse(null));
        if (app == null) {
            return new ApplicationSummaryDTO(null, null, null, 0, null, null);
        }

        int docsCount = Math.toIntExact(documentRepo.countByApplication(app));

        return new ApplicationSummaryDTO(
                app.getId(),
                null,
                studentFacingStatus(app),
                docsCount,
                null,
                null
        );
    }

    @GetMapping("/student/applications/{id}/mappings")
    public ApplicationMappingViewDTO mappingsForStudentApp(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Long id) {
        Application app = applicationRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("application not found"));
        StudentApplicant student = app.getStudent();
        if (student == null || !jwt.getSubject().equals(student.getKeycloakSub())) {
            throw new NoSuchElementException("application not found");
        }
        return mappingQueryService.forApplication(id);
    }

    // -------- Admin endpoints --------

    @GetMapping("/admin/applications/{id}/mappings")
    public ApplicationMappingViewDTO mappingsForAdmin(@PathVariable Long id) {
        return mappingQueryService.forApplication(id);
    }

    @GetMapping("/admin/applications")
    @Transactional
    public List<ApplicationSummaryDTO> listRecentApplications() {
        AcceptanceRule rule = acceptanceService.getRule();

        return applicationRepo.findTop20ByOrderByIdDesc().stream()
                .map(app -> buildAdminSummary(app, rule))
                .toList();
    }

    @PostMapping("/admin/applications/{id}/decision")
    @Transactional
    public ApplicationSummaryDTO decideApplication(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable Long id,
                                                   @RequestBody ApplicationDecisionDTO body) {
        if (body == null || body.action() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action is required");
        }
        String action = body.action().trim().toLowerCase();
        ApplicationStatus newStatus = switch (action) {
            case "approve", "approved" -> ApplicationStatus.APPROVED;
            case "reject", "rejected", "deny", "denied" -> ApplicationStatus.REJECTED;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must be approve/reject");
        };

        Application app = applicationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "application not found"));

        app.setStatus(newStatus);
        app.setDecisionDate(Instant.now());
        app.setDecisionBy(resolveActor(jwt));
        applicationRepo.save(app);

        // Notify student via WebSocket (/topic/app_status/{studentSub})
        StudentApplicant student = app.getStudent();
        if (student != null && student.getKeycloakSub() != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("applicationId", app.getId());
            payload.put("status", newStatus.name());
            payload.put("decidedAt", app.getDecisionDate());
            broker.convertAndSend("/topic/app_status/" + student.getKeycloakSub(), payload);
        }

        AcceptanceRule rule = acceptanceService.getRule();
        return buildAdminSummary(app, rule);
    }

    private String buildStudentName(StudentApplicant student) {
        if (student == null) return null;
        String first = Optional.ofNullable(student.getFirstName()).orElse("").trim();
        String last = Optional.ofNullable(student.getLastName()).orElse("").trim();
        return (first + " " + last).trim();
    }

    private String resolveActor(Jwt jwt) {
        if (jwt == null) return "system";
        String name = Optional.ofNullable(jwt.getClaimAsString("name"))
                .orElseGet(() -> Optional.ofNullable(jwt.getClaimAsString("preferred_username")).orElse(jwt.getSubject()));
        return name != null && !name.isBlank() ? name : "system";
    }

    private String statusOrDefault(Application app) {
        return app.getStatus() != null ? app.getStatus().name() : ApplicationStatus.SUBMITTED.name();
    }

    private ApplicationSummaryDTO buildAdminSummary(Application app, AcceptanceRule rule) {
        int docsCount = Math.toIntExact(documentRepo.countByApplication(app));
        int matched = mappingQueryService.countMappedSubjects(app.getId());
        Integer threshold = rule != null ? rule.getThresholdCount() : null;

        String rawStatus = statusOrDefault(app);
        String displayStatus = rawStatus;

        if (app.getStatus() == ApplicationStatus.PRE_ADMISSIBLE) {
            displayStatus = ApplicationStatus.PRE_ADMISSIBLE.name();
        } else if (app.getStatus() == ApplicationStatus.REJECTED && app.getDecisionBy() == null) {
            displayStatus = "PRE_REJECTED";
        }

        return new ApplicationSummaryDTO(
                app.getId(),
                buildStudentName(app.getStudent()),
                displayStatus,
                docsCount,
                matched,
                threshold
        );
    }

    /**
     * Students should only see coarse-grained statuses; auto pre-approval/rejection stays admin-facing.
     */
    private String studentFacingStatus(Application app) {
        ApplicationStatus status = app.getStatus();
        if (status == ApplicationStatus.APPROVED) return ApplicationStatus.APPROVED.name();

        // Only show REJECTED if an admin took a decision (decisionBy populated)
        if (status == ApplicationStatus.REJECTED && app.getDecisionBy() != null) {
            return ApplicationStatus.REJECTED.name();
        }

        // Everything else (SUBMITTED, UNDER_REVIEW, PRE_ADMISSIBLE, auto-rejected, null)
        return ApplicationStatus.UNDER_REVIEW.name();
    }
}
