package org.example.tas_backend.repos;

import org.example.tas_backend.entities.StudentApplicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface StudentApplicantRepo extends JpaRepository<StudentApplicant, Long> {
    Optional<StudentApplicant> findByKeycloakSub(String sub);
    @Query("select s.id from StudentApplicant s where s.keycloakSub = :sub")
    Optional<Long> findIdByKeycloakSub(@Param("sub") String sub);

    @Query("""
        select s from StudentApplicant s
        where lower(coalesce(s.firstName,'')) like lower(concat('%', :term, '%'))
           or lower(coalesce(s.lastName,'')) like lower(concat('%', :term, '%'))
           or lower(coalesce(s.email,'')) like lower(concat('%', :term, '%'))
    """)
    List<StudentApplicant> searchByTerm(@Param("term") String term);
}
