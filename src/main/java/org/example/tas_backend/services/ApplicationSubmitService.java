package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.ApplicationSubmitDTO;
import org.example.tas_backend.dtos.MatchResponseDTO;
import org.example.tas_backend.dtos.MatchTraceDTO;
import org.example.tas_backend.dtos.OcrResponse;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.Document;
import org.example.tas_backend.entities.ExtractedSubject;
import org.example.tas_backend.entities.SubjectMapping;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.entities.TargetSubject;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.enums.DocumentType;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.DocumentRepo;
import org.example.tas_backend.repos.ExtractedSubjectRepo;
import org.example.tas_backend.repos.SubjectMappingRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.example.tas_backend.repos.TargetSubjectRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationSubmitService {

    @Value("${storage.upload-root:uploads}")
    private String uploadRoot;

    // MUST include trailing slash:  http://127.0.0.1:8000/ocr/
    @Value("${ai.ocr-url}")
    private String ocrUrl;

    private final StudentApplicantRepo studentRepo;
    private final ApplicationRepo applicationRepo;
    private final DocumentRepo documentRepo;
    private final ExtractedSubjectRepo extractedSubjectRepo;
    private final SubjectMappingRepo subjectMappingRepo;
    private final TargetSubjectRepo targetSubjectRepo;
    private final AiService aiService;

    private final RestTemplate restTemplate;

    private StudentApplicant findBySub(String sub) {
        return studentRepo.findByKeycloakSub(sub)
                .orElseThrow(() -> new NoSuchElementException("student profile not found"));
    }

    @Transactional
    public Application submit(String sub, ApplicationSubmitDTO dto, List<MultipartFile> files) throws IOException {
        StudentApplicant student = findBySub(sub);

        var p = dto.profile();
        if (p != null) {
            if (p.phoneNumber() != null) student.setPhoneNumber(p.phoneNumber());
            if (p.dateOfBirth() != null) student.setDateOfBirth(p.dateOfBirth());
            if (p.nationalID() != null)  student.setNationalID(p.nationalID());
            if (p.nationality() != null) student.setNationality(p.nationality());
            if (p.residence() != null)   student.setResidence(p.residence());
            if (p.visaStatus() != null)  student.setVisaStatus(p.visaStatus());
        }

        Application app = new Application();
        app.setStudent(student);
        app.setPreferredProgram(dto.preferredProgram());
        app.setLanguageLevel(dto.languageLevel());
        app.setStatus(ApplicationStatus.SUBMITTED);

        app = applicationRepo.save(app);

        List<ExtractedSubject> pendingExtracted = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            String subKey = student.getKeycloakSub();
            Path userDir = Path.of(uploadRoot, subKey);
            Files.createDirectories(userDir);

            for (MultipartFile mf : files) {
                if (mf.isEmpty()) continue;

                String originalName = mf.getOriginalFilename();
                String cleanName = StringUtils.cleanPath(
                        originalName != null ? originalName : "uploaded-file"
                );
                String newName = UUID.randomUUID() + "_" + cleanName;
                Path target = userDir.resolve(newName);

                mf.transferTo(target);

                Document doc = new Document();
                doc.setApplication(app);
                doc.setType(DocumentType.TRANSCRIPT);
                doc.setFilename(cleanName);
                doc.setStorageKey(target.toString());
                doc.setMimeType(mf.getContentType());
                doc.setSizeBytes(mf.getSize());
                doc.setOcrJobId(null);
                doc.setRawText(null);

                doc = documentRepo.save(doc);

                // ======================
                // *** OCR CALL ***
                // ======================
                try {
                    FileSystemResource fileResource = new FileSystemResource(target.toFile());

                    // Step 2: sanity check
                    log.debug("OCR sanity file exists={} readable={} sizeBytes={} path={}",
                            fileResource.exists(), fileResource.isReadable(), Files.size(target), target);

                    // Build multipart body manually to force Content-Length (Django/WSGI rejects chunked)
                    String boundary = "----TASBoundary" + UUID.randomUUID();
                    String contentType = StringUtils.hasText(mf.getContentType())
                            ? mf.getContentType()
                            : MediaType.APPLICATION_OCTET_STREAM_VALUE;

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    // opening boundary + headers
                    bos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                    bos.write(("Content-Disposition: form-data; name=\"image\"; filename=\"" +
                            fileResource.getFilename() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                    bos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    // file bytes
                    bos.write(Files.readAllBytes(target));
                    bos.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    // closing boundary
                    bos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

                    byte[] multipartBytes = bos.toByteArray();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    headers.setContentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary));
                    headers.setContentLength(multipartBytes.length);

                    log.debug("Spring -> Django OCR POST {} file={} boundary={} contentLength={}",
                            ocrUrl, fileResource.getFilename(), boundary, multipartBytes.length);

                    ResponseEntity<OcrResponse> resp =
                            restTemplate.postForEntity(ocrUrl, new HttpEntity<>(multipartBytes, headers), OcrResponse.class);

                    log.debug("Spring -> Django OCR status={} body={}", resp.getStatusCode(), resp.getBody());

                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        var body = resp.getBody();
                        doc.setRawText(body.ocr_text());
                        documentRepo.save(doc);

                        if (body.courses() != null) {
                            for (String course : body.courses()) {
                                if (!StringUtils.hasText(course)) continue;
                                ExtractedSubject es = new ExtractedSubject();
                                es.setDocument(doc);
                                es.setRawName(course.trim());
                                pendingExtracted.add(es);
                            }
                        }
                    } else {
                        log.warn("OCR non-2xx for {}: {}", cleanName, resp.getStatusCode());
                    }

                } catch (HttpStatusCodeException ex) {
                    log.error("OCR HTTP error for file {}: {} body={}", cleanName, ex.getStatusCode(), ex.getResponseBodyAsString());

                } catch (RestClientException ex) {
                    log.error("OCR call failed for file {}: {}", cleanName, ex.getMessage());
                }
            }
        }

        // persist extracted subjects and run matching
        var savedSubjects = saveExtractedSubjects(pendingExtracted);
        log.debug("Persisted {} extracted subjects (pre-match)", savedSubjects.size());
        runMatching(savedSubjects);

        return app;
    }

    private List<ExtractedSubject> saveExtractedSubjects(List<ExtractedSubject> subjects) {
        if (subjects == null || subjects.isEmpty()) return List.of();

        var saved = new ArrayList<ExtractedSubject>(subjects.size());
        for (ExtractedSubject es : subjects) {
            saved.add(extractedSubjectRepo.save(es));
        }
        return saved;
    }

    private List<Map<String, Object>> buildTargetPayload() {
        var targets = targetSubjectRepo.findAll();
        if (targets == null || targets.isEmpty()) return List.of();

        var payload = new ArrayList<Map<String, Object>>(targets.size());
        for (TargetSubject t : targets) {
            if (!StringUtils.hasText(t.getCode())) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("code", t.getCode());
            String label = StringUtils.hasText(t.getName()) ? t.getName() : t.getCode();
            row.put("title_fr", label);
            if (t.getCoefficient() != null) {
                row.put("coef", t.getCoefficient().intValue());
            }
            payload.add(row);
        }
        return payload;
    }

    private void runMatching(List<ExtractedSubject> subjects) {
        if (subjects == null || subjects.isEmpty()) return;

        var labels = subjects.stream()
                .map(ExtractedSubject::getRawName)
                .filter(StringUtils::hasText)
                .toList();
        if (labels.isEmpty()) return;

        // Send the admin-managed catalog to the matcher so its view of targets stays aligned.
        var targets = buildTargetPayload();
        if (targets.isEmpty()) {
            log.warn("No target subjects found in Spring DB; matcher will not have a catalog to use");
        }

        MatchResponseDTO match;
        try {
            log.debug("Calling AI matcher with labels={} targets={}", labels, targets.size());
            match = aiService.matchSubjects(labels, targets);
        } catch (Exception ex) {
            log.error("Matcher call failed", ex);
            return;
        }
        if (match == null || match.trace() == null || match.trace().isEmpty()) {
            log.warn("Matcher returned empty trace for labels={}", labels);
            return;
        }

        log.debug("Matcher response coverage_pct={} matched={}", match.coveragePct(), match.matched());

        int limit = Math.min(subjects.size(), match.trace().size());
        for (int i = 0; i < limit; i++) {
            ExtractedSubject subject = subjects.get(i);
            MatchTraceDTO trace = match.trace().get(i);

            // skip if already mapped (idempotent)
            if (!subjectMappingRepo.findByExtractedSubject(subject).isEmpty()) {
                continue;
            }

            String targetCode = trace.target();
            if (!StringUtils.hasText(targetCode)) {
                log.debug("No target code for extracted subject {} rawName={}", subject.getId(), subject.getRawName());
                continue;
            }

            TargetSubject target = upsertTarget(targetCode, trace.targetTitle(), trace.targetCoef());
            if (target == null) {
                log.warn("Skipping mapping for extractedId={} because target code '{}' not found", subject.getId(), targetCode);
                continue;
            }

            SubjectMapping mapping = new SubjectMapping();
            mapping.setExtractedSubject(subject);
            mapping.setTargetSubject(target);
            mapping.setConfidence(trace.score() != null ? trace.score().floatValue() : null);
            mapping.setNormalizedScore(mapping.getConfidence());
            mapping.setAuto(true);
            mapping.setMethod(trace.method());
            subjectMappingRepo.save(mapping);
            log.debug("Saved auto mapping extractedId={} -> targetCode={} score={} method={}",
                    subject.getId(), targetCode, mapping.getConfidence(), trace.method());
        }
    }

    private TargetSubject upsertTarget(String code, String title, Integer coef) {
        // Target list is static; do not create or update here
        Optional<TargetSubject> maybe = targetSubjectRepo.findByCode(code);
        if (maybe.isEmpty()) {
            log.warn("Target code '{}' not found; skipping (targets are admin-managed only)", code);
            return null;
        }
        return maybe.get();
    }
}
