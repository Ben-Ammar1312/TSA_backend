package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.entities.AcceptanceRule;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.repos.AcceptanceRuleRepo;
import org.example.tas_backend.repos.ApplicationRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcceptanceService {

    private final AcceptanceRuleRepo ruleRepo;
    private final ApplicationRepo applicationRepo;
    private final MappingQueryService mappingQueryService;

    @Transactional
    public AcceptanceRule getRule() {
        return ruleRepo.findById(1L).orElseGet(() -> {
            var r = new AcceptanceRule();
            r.setId(1L);
            return ruleRepo.save(r);
        });
    }

    @Transactional
    public AcceptanceRule updateThreshold(int threshold) {
        AcceptanceRule rule = getRule();
        rule.setThresholdCount(threshold);
        ruleRepo.save(rule);
        reevaluateApplications(rule);
        return rule;
    }

    @Transactional
    public void reevaluateApplications(AcceptanceRule rule) {
        List<Application> apps = applicationRepo.findAll();
        for (Application app : apps) {
            // Skip final decisions
            if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.REJECTED) {
                continue;
            }
            int matched = mappingQueryService.countMappedSubjects(app.getId());
            if (matched >= rule.getThresholdCount()) {
                app.setStatus(ApplicationStatus.PRE_ADMISSIBLE); // accepted pending review
            } else if (app.getStatus() == null || app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.PRE_ADMISSIBLE) {
                app.setStatus(ApplicationStatus.UNDER_REVIEW);
            }
            applicationRepo.save(app);
        }
    }
}
