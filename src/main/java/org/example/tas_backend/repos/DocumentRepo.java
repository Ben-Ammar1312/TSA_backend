package org.example.tas_backend.repos;

import org.example.tas_backend.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepo extends JpaRepository<Document, Long> {
    List<Document> findByApplication(org.example.tas_backend.entities.Application application);
}
