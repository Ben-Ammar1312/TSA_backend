package org.example.tas_backend.controllers;

import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tas_admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.envers.autoRegisterListeners=false",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop"
})
class AdminDecisionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationRepo applicationRepo;
    @Autowired
    private StudentApplicantRepo studentApplicantRepo;

    private Application application;

    @BeforeEach
    void setup() {
        applicationRepo.deleteAll();
        studentApplicantRepo.deleteAll();

        StudentApplicant student = new StudentApplicant();
        student.setKeycloakSub("admin-student");
        student = studentApplicantRepo.save(student);

        application = new Application();
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setStudent(student);
        application = applicationRepo.save(application);
    }

    @Test
    void adminDecisionShouldUpdateStatus() throws Exception {
        mockMvc.perform(post("/admin/applications/{id}/decision", application.getId())
                        .with(jwt().jwt(jwt -> jwt.subject("admin-sub")
                                .claim("preferred_username", "adminUser")
                                .claim("realm_access", Map.of("roles", List.of("admin")))
                                .claim("aud", List.of("angular"))
                                .issuer("http://localhost:8080/realms/TAS"))
                                .authorities(t -> java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"approve\"}"))
                .andExpect(status().is2xxSuccessful());

        Application updated = applicationRepo.findById(application.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }
}
