package org.example.tas_backend.repos;


import org.example.tas_backend.entities.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffRepo extends JpaRepository<Staff, Long> {
    Optional<Staff> findByKeycloakSub(String sub);

    @Query("select s.id from Staff s where s.keycloakSub = :sub")
    Optional<Long> findIdByKeycloakSub(@Param("sub") String sub);
}