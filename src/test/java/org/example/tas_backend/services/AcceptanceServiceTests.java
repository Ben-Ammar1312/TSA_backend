package org.example.tas_backend.services;

import org.example.tas_backend.entities.AcceptanceRule;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.repos.AcceptanceRuleRepo;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.TargetSubjectRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptanceServiceTests {

    @Mock
    private AcceptanceRuleRepo ruleRepo;
    @Mock
    private ApplicationRepo applicationRepo;
    @Mock
    private MappingQueryService mappingQueryService;
    @Mock
    private TargetSubjectRepo targetSubjectRepo;

    @InjectMocks
    private AcceptanceService acceptanceService;

    private AcceptanceRule rule;
    private Application application;

    @BeforeEach
    void setup() {
        rule = new AcceptanceRule();
        rule.setId(1L);
        rule.setThresholdCount(2);

        application = new Application();
        application.setId(10L);
        application.setStatus(ApplicationStatus.SUBMITTED);

        when(ruleRepo.findById(1L)).thenReturn(Optional.of(rule));
        when(ruleRepo.save(any(AcceptanceRule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepo.findById(10L)).thenReturn(Optional.of(application));
        when(applicationRepo.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(targetSubjectRepo.count()).thenReturn(5L);
    }

    @Test
    void shouldSetPreAdmissibleWhenMatchedExceedsThreshold() {
        when(mappingQueryService.countMappedSubjects(10L)).thenReturn(3);

        acceptanceService.reevaluateApplication(10L);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PRE_ADMISSIBLE);
    }

    @Test
    void shouldSetRejectedWhenMatchedBelowThreshold() {
        when(mappingQueryService.countMappedSubjects(10L)).thenReturn(1);

        acceptanceService.reevaluateApplication(10L);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void shouldSetPreAdmissibleAtThresholdBoundary() {
        when(mappingQueryService.countMappedSubjects(10L)).thenReturn(2);

        acceptanceService.reevaluateApplication(10L);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PRE_ADMISSIBLE);
    }
}
