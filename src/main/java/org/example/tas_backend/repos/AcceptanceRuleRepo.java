package org.example.tas_backend.repos;

import org.example.tas_backend.entities.AcceptanceRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcceptanceRuleRepo extends JpaRepository<AcceptanceRule, Long> {
}
