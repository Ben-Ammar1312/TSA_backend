package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.StudentFileDTO;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.entities.StudentFile;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.example.tas_backend.repos.StudentFileRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentFileService {

    @Value("${storage.upload-root:uploads}")
    private String uploadRoot;

    private final StudentApplicantRepo studentRepo;
    private final StudentFileRepo fileRepo;

    private StudentApplicant findBySub(String sub) {
        return studentRepo.findByKeycloakSub(sub)
                .orElseThrow(() -> new NoSuchElementException("student profile not found"));
    }

    @Transactional
    public StudentFileDTO uploadForStudent(String sub, MultipartFile file) throws IOException {
        StudentApplicant student = findBySub(sub);

        // directory: uploads/<keycloakSub>/
        Path userDir = Path.of(uploadRoot, student.getKeycloakSub());
        Files.createDirectories(userDir);

        String cleanName = StringUtils.cleanPath(file.getOriginalFilename());
        String newName = UUID.randomUUID() + "_" + cleanName;
        Path target = userDir.resolve(newName);

        // save to disk
        file.transferTo(target);

        // persist metadata
        StudentFile sf = new StudentFile();
        sf.setStudent(student);
        sf.setOriginalFilename(cleanName);
        sf.setStoragePath(target.toString());
        sf.setContentType(file.getContentType());
        sf.setSizeBytes(file.getSize());

        sf = fileRepo.save(sf);

        return new StudentFileDTO(sf.getId(), sf.getOriginalFilename(), sf.getContentType(), sf.getSizeBytes());
    }

    @Transactional
    public List<StudentFileDTO> listForStudent(String sub) {
        StudentApplicant student = findBySub(sub);
        return fileRepo.findByStudent(student).stream()
                .map(f -> new StudentFileDTO(
                        f.getId(), f.getOriginalFilename(), f.getContentType(), f.getSizeBytes()))
                .toList();
    }
}