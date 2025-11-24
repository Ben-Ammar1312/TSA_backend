package org.example.tas_backend.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.ApplicationMappingViewDTO;
import org.example.tas_backend.dtos.ApplicationSummaryDTO;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.DocumentRepo;
import org.example.tas_backend.services.MappingQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ApplicationMappingController {

    private final MappingQueryService mappingQueryService;
    private final ApplicationRepo applicationRepo;
    private final DocumentRepo documentRepo;

    // -------- Student endpoints --------

    @GetMapping("/student/applications/latest/mappings")
    public ApplicationMappingViewDTO latestForStudent(@AuthenticationPrincipal Jwt jwt) {
        return mappingQueryService.latestForStudent(jwt.getSubject());
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
        return applicationRepo.findTop20ByOrderByIdDesc().stream()
                .map(app -> new ApplicationSummaryDTO(
                        app.getId(),
                        buildStudentName(app.getStudent()),
                        app.getStatus() != null ? app.getStatus().name() : null,
                        Optional.ofNullable(app.getDocuments())
                                .map(List::size)
                                .orElseGet(() -> documentRepo.findByApplication(app).size())
                ))
                .toList();
    }

    private String buildStudentName(StudentApplicant student) {
        if (student == null) return null;
        String first = Optional.ofNullable(student.getFirstName()).orElse("").trim();
        String last = Optional.ofNullable(student.getLastName()).orElse("").trim();
        return (first + " " + last).trim();
    }
}
