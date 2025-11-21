package org.example.tas_backend.repos;

import org.example.tas_backend.entities.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepo extends JpaRepository<Application, Long> {}
