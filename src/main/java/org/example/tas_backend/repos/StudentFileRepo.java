package org.example.tas_backend.repos;

import org.example.tas_backend.entities.StudentFile;
import org.example.tas_backend.entities.StudentApplicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentFileRepo extends JpaRepository<StudentFile, Long> {
    List<StudentFile> findByStudent(StudentApplicant student);
}