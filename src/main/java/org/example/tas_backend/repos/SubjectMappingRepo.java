package org.example.tas_backend.repos;

import org.example.tas_backend.entities.ExtractedSubject;
import org.example.tas_backend.entities.SubjectMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectMappingRepo extends JpaRepository<SubjectMapping, Long> {

    @EntityGraph(attributePaths = {"targetSubject"})
    List<SubjectMapping> findByExtractedSubjectIn(List<ExtractedSubject> subjects);

    List<SubjectMapping> findByExtractedSubject(ExtractedSubject subject);
}
