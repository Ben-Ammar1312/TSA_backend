package org.example.tas_backend.services;

import org.example.tas_backend.dtos.ApplicationSubmitDTO;
import org.example.tas_backend.dtos.MatchResponseDTO;
import org.example.tas_backend.dtos.MatchTraceDTO;
import org.example.tas_backend.dtos.OcrResponse;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.entities.ExtractedSubject;
import org.example.tas_backend.entities.StudentApplicant;
import org.example.tas_backend.entities.SubjectMapping;
import org.example.tas_backend.entities.TargetSubject;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.DocumentRepo;
import org.example.tas_backend.repos.ExtractedSubjectRepo;
import org.example.tas_backend.repos.MappingSuggestionRepo;
import org.example.tas_backend.repos.StudentApplicantRepo;
import org.example.tas_backend.repos.SubjectMappingRepo;
import org.example.tas_backend.repos.TargetSubjectRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationSubmitServiceTests {

    @Mock private StudentApplicantRepo studentRepo;
    @Mock private ApplicationRepo applicationRepo;
    @Mock private DocumentRepo documentRepo;
    @Mock private ExtractedSubjectRepo extractedSubjectRepo;
    @Mock private SubjectMappingRepo subjectMappingRepo;
    @Mock private TargetSubjectRepo targetSubjectRepo;
    @Mock private MappingSuggestionRepo mappingSuggestionRepo;
    @Mock private AiService aiService;
    @Mock private AcceptanceService acceptanceService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private ApplicationSubmitService service;

    private StudentApplicant student;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(service, "uploadRoot", Path.of("build/uploads").toString());
        ReflectionTestUtils.setField(service, "ocrUrl", "http://mock/ocr");

        student = new StudentApplicant();
        student.setId(5L);
        student.setKeycloakSub("kc-sub");

        when(studentRepo.findByKeycloakSub("kc-sub")).thenReturn(Optional.of(student));
        when(applicationRepo.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            if (app.getId() == null) app.setId(99L);
            return app;
        });
        when(documentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(extractedSubjectRepo.save(any())).thenAnswer(inv -> {
            ExtractedSubject es = inv.getArgument(0);
            if (es.getId() == null) es.setId(77L);
            return es;
        });
        when(subjectMappingRepo.save(any())).thenAnswer(inv -> {
            SubjectMapping sm = inv.getArgument(0);
            if (sm.getId() == null) sm.setId(88L);
            return sm;
        });
        when(subjectMappingRepo.findByExtractedSubject(any())).thenReturn(List.of());
        when(mappingSuggestionRepo.findByNormLabelAndProposedTargetCodeAndLanguage(any(), any(), any()))
                .thenReturn(Optional.empty());

        TargetSubject target = new TargetSubject();
        target.setId(3L);
        target.setCode("math.1");
        target.setName("Math");
        target.setCoefficient(1.0f);
        when(targetSubjectRepo.findAll()).thenReturn(List.of(target));
        when(targetSubjectRepo.findByCode(eq("math.1"))).thenReturn(Optional.of(target));
    }

    @Test
    void shouldPersistAndCallAiOnValidSubmission() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "transcript.pdf", "application/pdf", "dummy".getBytes());

        when(restTemplate.postForEntity(eq("http://mock/ocr"), any(HttpEntity.class), eq(OcrResponse.class)))
                .thenReturn(ResponseEntity.ok(new OcrResponse("transcript.pdf", "Advanced Algebra", 1, List.of("Advanced Algebra"))));

        MatchTraceDTO trace = new MatchTraceDTO("math", "math.1", "fuzzy", 0.9, "Math", null, 1);
        when(aiService.matchSubjects(anyList(), anyList()))
                .thenReturn(new MatchResponseDTO(List.of("math.1"), 50.0, List.of(trace)));

        ApplicationSubmitDTO dto = new ApplicationSubmitDTO(null, "Data Science", "B2");

        Application result = service.submit("kc-sub", dto, List.of(file));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        verify(aiService).matchSubjects(anyList(), anyList());
        verify(acceptanceService).reevaluateApplication(result.getId());
    }

    @Test
    void shouldThrowWhenStudentMissing() {
        when(studentRepo.findByKeycloakSub("missing")).thenReturn(Optional.empty());
        ApplicationSubmitDTO dto = new ApplicationSubmitDTO(null, "Data Science", "B2");

        assertThatThrownBy(() -> service.submit("missing", dto, List.of()))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(aiService, acceptanceService);
    }
}
