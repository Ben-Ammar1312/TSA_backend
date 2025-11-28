package org.example.tas_backend.repos;

import org.example.tas_backend.entities.ExtractedSubject;
import org.example.tas_backend.entities.SubjectMapping;
import org.example.tas_backend.entities.TargetSubject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectMappingRepo extends JpaRepository<SubjectMapping, Long> {

    @EntityGraph(attributePaths = {"targetSubject"})
    List<SubjectMapping> findByExtractedSubjectIn(List<ExtractedSubject> subjects);

    List<SubjectMapping> findByExtractedSubject(ExtractedSubject subject);

    Optional<SubjectMapping> findByExtractedSubjectAndTargetSubject(ExtractedSubject subject,
                                                                    TargetSubject target);
}
