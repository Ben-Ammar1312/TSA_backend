package org.example.tas_backend.repos;

import org.example.tas_backend.entities.StudentApplicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentApplicantRepo extends JpaRepository<StudentApplicant, Long> {
    Optional<StudentApplicant> findByKeycloakSub(String sub);
    @Query("select s.id from StudentApplicant s where s.keycloakSub = :sub")
    Optional<Long> findIdByKeycloakSub(@Param("sub") String sub);
}