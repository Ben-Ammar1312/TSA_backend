package org.example.tas_backend.repos;

import org.example.tas_backend.entities.TargetSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TargetSubjectRepo extends JpaRepository<TargetSubject, Long> {
    Optional<TargetSubject> findByCode(String code);
}
