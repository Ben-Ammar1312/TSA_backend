package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.ApplicationMappingViewDTO;
import org.example.tas_backend.dtos.DocumentMappingViewDTO;
import org.example.tas_backend.dtos.ExtractedSubjectViewDTO;
import org.example.tas_backend.dtos.SubjectMappingViewDTO;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.Document;
import org.example.tas_backend.entities.ExtractedSubject;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.entities.SubjectMapping;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.DocumentRepo;
import org.example.tas_backend.repos.ExtractedSubjectRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.example.tas_backend.repos.SubjectMappingRepo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MappingQueryService {

    private final StudentApplicantRepo studentRepo;
    private final ApplicationRepo applicationRepo;
    private final DocumentRepo documentRepo;
    private final ExtractedSubjectRepo extractedSubjectRepo;
    private final SubjectMappingRepo subjectMappingRepo;

    @Transactional
    public ApplicationMappingViewDTO latestForStudent(String sub) {
        StudentApplicant student = studentRepo.findByKeycloakSub(sub)
                .orElseThrow(() -> new NoSuchElementException("student profile not found"));

        Application app = applicationRepo.findTopByStudentOrderByIdDesc(student)
                .orElseThrow(() -> new NoSuchElementException("no applications found for student"));

        return build(app);
    }

    @Transactional
    public ApplicationMappingViewDTO forApplication(Long appId) {
        Application app = applicationRepo.findById(appId)
                .orElseThrow(() -> new NoSuchElementException("application not found: " + appId));
        return build(app);
    }

    @Transactional
    public int countMappedSubjects(Long appId) {
        Application app = applicationRepo.findById(appId)
                .orElseThrow(() -> new NoSuchElementException("application not found: " + appId));
        List<Document> docs = documentRepo.findByApplication(app);
        if (docs.isEmpty()) return 0;
        List<ExtractedSubject> subjects = extractedSubjectRepo.findByDocumentIn(docs);
        if (subjects.isEmpty()) return 0;
        List<SubjectMapping> mappings = subjectMappingRepo.findByExtractedSubjectIn(subjects);
        return (int) mappings.stream()
                .filter(m -> m.getTargetSubject() != null)
                .count();
    }

    private ApplicationMappingViewDTO build(Application app) {
        List<Document> docs = documentRepo.findByApplication(app);
        List<ExtractedSubject> subjects = docs.isEmpty()
                ? List.of()
                : extractedSubjectRepo.findByDocumentIn(docs);

        Map<Long, List<ExtractedSubject>> subjectsByDoc = subjects.stream()
                .collect(Collectors.groupingBy(es -> es.getDocument().getId()));

        List<SubjectMapping> mappings = subjects.isEmpty()
                ? List.of()
                : subjectMappingRepo.findByExtractedSubjectIn(subjects);

        Map<Long, List<SubjectMapping>> mappingsBySubject = mappings.stream()
                .collect(Collectors.groupingBy(sm -> sm.getExtractedSubject().getId()));

        List<DocumentMappingViewDTO> docDtos = new ArrayList<>();
        for (Document doc : docs) {
            List<ExtractedSubject> docSubjects = subjectsByDoc.getOrDefault(doc.getId(), List.of());
            List<ExtractedSubjectViewDTO> subjectDtos = new ArrayList<>();

            for (ExtractedSubject es : docSubjects) {
                List<SubjectMapping> esMappings = mappingsBySubject.getOrDefault(es.getId(), List.of());
                List<SubjectMappingViewDTO> mappingDtos = esMappings.stream()
                        .map(m -> new SubjectMappingViewDTO(
                                m.getId(),
                                m.getTargetSubject() != null ? m.getTargetSubject().getCode() : null,
                                m.getTargetSubject() != null ? m.getTargetSubject().getName() : null,
                                m.getConfidence(),
                                m.getMethod(),
                                m.getNormalizedScore()
                        ))
                        .toList();

                subjectDtos.add(new ExtractedSubjectViewDTO(
                        es.getId(),
                        es.getRawName(),
                        es.getRawScore(),
                        es.getRawScale(),
                        es.getYear(),
                        es.getSourceCoefficient(),
                        mappingDtos
                ));
            }

            docDtos.add(new DocumentMappingViewDTO(
                    doc.getId(),
                    doc.getFilename(),
                    doc.getRawText(),
                    subjectDtos
            ));
        }

        var student = app.getStudent();
        String studentName = formatStudentName(student);

        return new ApplicationMappingViewDTO(app.getId(), studentName, docDtos);
    }

    private String formatStudentName(StudentApplicant student) {
        if (student == null) return null;
        String first = Optional.ofNullable(student.getFirstName()).orElse("").trim();
        String last = Optional.ofNullable(student.getLastName()).orElse("").trim();
        return (first + " " + last).trim();
    }
}
