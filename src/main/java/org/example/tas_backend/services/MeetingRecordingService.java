package org.example.tas_backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tas_backend.entities.Invite;
import org.example.tas_backend.repos.InviteRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingRecordingService {

    @Value("${storage.upload-root:uploads}")
    private String uploadRoot;
    @Value("${app.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    private final InviteRepo repo;
    private final CallSummaryService summaryService;
    private final SimpMessagingTemplate broker;

    public Invite storeRecording(UUID inviteId, MultipartFile audioFile) throws IOException {
        Invite invite = repo.findById(inviteId).orElseThrow();

        Path dir = Path.of(uploadRoot, "meetings", inviteId.toString());
        Files.createDirectories(dir);

        String extension = resolveExtension(audioFile.getOriginalFilename());
        String filename = "recording_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
        Path target = dir.resolve(filename);

        audioFile.transferTo(target);
        log.info("Saved meeting audio for invite {} to {} ({} bytes)", inviteId, target, audioFile.getSize());

        invite.setRecordingPath(target.toString());
        String relativeUrl = "/uploads/meetings/" + inviteId + "/" + filename;
        invite.setRecordingUrl(toAbsoluteUrl(relativeUrl));
        // Always reset previous summaries so callers do not see stale content
        invite.setCallSummary(null);
        invite.setSummaryGeneratedAt(null);

        long fileSize = Files.size(target);
        if (fileSize < 2048) {
            log.warn("Recording for invite {} is too small ({} bytes); skipping summary generation", inviteId, fileSize);
            invite.setCallSummary("Recording saved but was too short to transcribe.");
            invite.setSummaryGeneratedAt(OffsetDateTime.now());
            Invite saved = repo.save(invite);
            publish(saved);
            return saved;
        }

        Optional<String> maybeSummary = summaryService.summarize(target, invite.getRecordingUrl());
        if (maybeSummary.isPresent()) {
            invite.setCallSummary(maybeSummary.get());
            invite.setSummaryGeneratedAt(OffsetDateTime.now());
        } else {
            log.warn("Summary generation skipped/failed for invite {}", inviteId);
            invite.setCallSummary("Recording saved. Summary service unavailable or still processing.");
            invite.setSummaryGeneratedAt(OffsetDateTime.now());
        }

        Invite saved = repo.save(invite);
        publish(saved);
        return saved;
    }

    private void publish(Invite inv) {
        if (inv.getCreatedBy() != null && !inv.getCreatedBy().isBlank()) {
            broker.convertAndSend("/topic/invites/" + inv.getCreatedBy(), inv);
        }
        broker.convertAndSend("/topic/invites/" + inv.getTargetUserId(), inv);
        broker.convertAndSend("/topic/invites/admin", inv);
    }

    private String toAbsoluteUrl(String url) {
        if (url == null || url.isBlank()) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        String base = publicBaseUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!url.startsWith("/")) url = "/" + url;
        return base + url;
    }

    private String resolveExtension(String originalName) {
        if (originalName == null) return ".webm";
        String ext = StringUtils.getFilenameExtension(originalName);
        return (ext != null && !ext.isBlank()) ? "." + ext : ".webm";
    }
}
