package org.example.tas_backend.repos;

import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.StudentApplicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepo extends JpaRepository<Application, Long> {
    Optional<Application> findTopByStudentOrderByIdDesc(StudentApplicant student);
    Optional<Application> findTopByStudent_KeycloakSubOrderByIdDesc(String keycloakSub);
    List<Application> findTop20ByOrderByIdDesc();
}
