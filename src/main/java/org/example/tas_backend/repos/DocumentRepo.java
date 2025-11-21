package org.example.tas_backend.repos;

import org.example.tas_backend.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepo extends JpaRepository<Document, Long> {}