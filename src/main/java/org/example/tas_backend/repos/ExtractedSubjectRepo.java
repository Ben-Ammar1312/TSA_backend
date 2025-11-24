package org.example.tas_backend.repos;

import org.example.tas_backend.entities.Document;
import org.example.tas_backend.entities.ExtractedSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractedSubjectRepo extends JpaRepository<ExtractedSubject, Long> {
    List<ExtractedSubject> findByDocument(Document document);
    List<ExtractedSubject> findByDocumentIn(List<Document> documents);
}
