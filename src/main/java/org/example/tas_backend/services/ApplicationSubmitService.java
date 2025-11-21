package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.ApplicationSubmitDTO;
import org.example.tas_backend.dtos.OcrResponse;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.Document;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.enums.DocumentType;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.DocumentRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationSubmitService {

    @Value("${storage.upload-root:uploads}")
    private String uploadRoot;

    // MUST include trailing slash:  http://127.0.0.1:8000/ocr/
    @Value("${ai.ocr-url}")
    private String ocrUrl;

    private final OAuthClientTokenService tokens;
    private final StudentApplicantRepo studentRepo;
    private final ApplicationRepo applicationRepo;
    private final DocumentRepo documentRepo;

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
        app.setIntakePeriod(dto.intakePeriod());
        app.setLanguageLevel(dto.languageLevel());
        app.setStatus(ApplicationStatus.SUBMITTED);

        app = applicationRepo.save(app);

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

                documentRepo.save(doc);

                // ======================
                // *** OCR CALL ***
                // ======================
                try {
                    FileSystemResource fileResource = new FileSystemResource(target.toFile());

                    // Step 2: sanity check
                    System.out.println("DEBUG OCR file exists=" + fileResource.exists()
                            + " readable=" + fileResource.isReadable()
                            + " sizeBytes=" + Files.size(target)
                            + " path=" + target);

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

                    System.out.println("DEBUG Spring -> Django OCR: POST " + ocrUrl +
                            " file=" + fileResource.getFilename() +
                            " boundary=" + boundary +
                            " contentLength=" + multipartBytes.length);

                    ResponseEntity<OcrResponse> resp =
                            restTemplate.postForEntity(ocrUrl, new HttpEntity<>(multipartBytes, headers), OcrResponse.class);

                    System.out.println("DEBUG Spring -> Django OCR: status=" + resp.getStatusCode());
                    System.out.println("DEBUG Spring -> Django OCR body=" + resp.getBody());

                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        doc.setRawText(resp.getBody().ocr_text());
                        documentRepo.save(doc);
                    } else {
                        System.err.println("OCR non-2xx for " + cleanName + ": " + resp.getStatusCode());
                    }

                } catch (HttpStatusCodeException ex) {
                    System.err.println("OCR HTTP error for file " + cleanName + ": "
                            + ex.getStatusCode() + " body=" + ex.getResponseBodyAsString());

                } catch (RestClientException ex) {
                    System.err.println("OCR call failed for file " + cleanName + ": " + ex.getMessage());
                }
            }
        }

        return app;
    }
}
