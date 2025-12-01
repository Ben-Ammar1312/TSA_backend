package org.example.tas_backend.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tas_backend.entities.AcceptanceRule;
import org.example.tas_backend.entities.Application;
import org.example.tas_backend.enums.ApplicationStatus;
import org.example.tas_backend.repos.AcceptanceRuleRepo;
import org.example.tas_backend.repos.ApplicationRepo;
import org.example.tas_backend.repos.TargetSubjectRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AcceptanceService {

    private final AcceptanceRuleRepo ruleRepo;
    private final ApplicationRepo applicationRepo;
    private final MappingQueryService mappingQueryService;
    private final TargetSubjectRepo targetSubjectRepo;

    @Transactional
    public AcceptanceRule getRule() {
        AcceptanceRule rule = ruleRepo.findById(1L).orElseGet(() -> {
            var r = new AcceptanceRule();
            r.setId(1L);
            return ruleRepo.save(r);
        });
        int targetCount = Math.toIntExact(targetSubjectRepo.count());
        rule.setTargetCount(targetCount);
        if (rule.getThresholdCount() > targetCount) {
            rule.setThresholdCount(targetCount);
        }
        return ruleRepo.save(rule);
    }

    @Transactional
    public AcceptanceRule updateThreshold(int threshold) {
        AcceptanceRule rule = getRule();
        int targetCount = Math.toIntExact(targetSubjectRepo.count());
        rule.setTargetCount(targetCount);
        if (threshold > targetCount) {
            threshold = targetCount;
        }
        rule.setThresholdCount(threshold);
        ruleRepo.save(rule);
        reevaluateApplications(rule);
        return rule;
    }

    /** Re-evaluate a single application after mappings change. */
    @Transactional
    public void reevaluateApplication(Long appId) {
        AcceptanceRule rule = getRule();
        Application app = applicationRepo.findById(appId)
                .orElseThrow(() -> new NoSuchElementException("application not found: " + appId));
        applyRule(app, rule);
    }

    @Transactional
    public void reevaluateApplications(AcceptanceRule rule) {
        List<Application> apps = applicationRepo.findAll();
        for (Application app : apps) {
            applyRule(app, rule);
        }
    }

    private void applyRule(Application app, AcceptanceRule rule) {
        // Skip final decisions
        if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.REJECTED) {
            return;
        }
        int matched = mappingQueryService.countMappedSubjects(app.getId());
        if (matched >= rule.getThresholdCount()) {
            app.setStatus(ApplicationStatus.PRE_ADMISSIBLE); // provisional accept
        } else {
            app.setStatus(ApplicationStatus.REJECTED); // provisional deny; admin can still override (admin UI will label as pre-rejected)
        }
        applicationRepo.save(app);
    }
}
