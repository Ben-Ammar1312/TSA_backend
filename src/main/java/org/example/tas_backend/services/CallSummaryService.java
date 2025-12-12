package org.example.tas_backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CallSummaryService {

    private final RestTemplate restTemplate;
    private final OAuthClientTokenService tokens;
    private final String summaryEndpoint;
    private final String publicBaseUrl;

    public CallSummaryService(RestTemplate restTemplate,
                              OAuthClientTokenService tokens,
                              @Value("${ai.base-url}") String aiBase,
                              @Value("${ai.call-summary-url:}") String overrideUrl,
                              @Value("${app.public-base-url:http://localhost:8081}") String publicBaseUrl) {
        this.restTemplate = restTemplate;
        this.tokens = tokens;
        String base = aiBase.endsWith("/") ? aiBase.substring(0, aiBase.length() - 1) : aiBase;
        this.summaryEndpoint = (overrideUrl != null && !overrideUrl.isBlank())
                ? overrideUrl
                : base + "/matcher/summarize/audio";
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        log.info("Call summary endpoint configured at {}", this.summaryEndpoint);
    }

    /**
     * Send recorded audio to the AI service for transcription + summarization.
     * Returns Optional.empty() if the endpoint is not configured or the call fails.
     */
    public Optional<String> summarize(Path audioFile) {
        return summarize(audioFile, null);
    }

    /**
     * Send recorded audio with an optional recording URL. We prefer uploading the file
     * first (most reliable) while also attaching the URL as metadata/query param so the
     * AI service can still fetch the file if multipart parsing fails. If the file upload
     * fails or returns an empty summary, we fall back to a lightweight URL-only request.
     */
    public Optional<String> summarize(Path audioFile, String recordingUrl) {
        if (summaryEndpoint == null || summaryEndpoint.isBlank()) {
            log.warn("ai.call-summary-url not configured; skipping LLM summary generation");
            return Optional.empty();
        }

        Optional<String> viaFile = sendWithFile(audioFile, recordingUrl)
                .filter(s -> s != null && !s.isBlank());
        if (viaFile.isPresent()) {
            return viaFile;
        }

        if (recordingUrl != null && !recordingUrl.isBlank()) {
            log.warn("File upload for summary failed/empty; falling back to URL for {}", recordingUrl);
            return sendWithUrl(toAbsoluteUrl(recordingUrl))
                    .filter(s -> s != null && !s.isBlank());
        }

        return Optional.empty();
    }

    private String toAbsoluteUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            return publicBaseUrl + "/" + url;
        }
        return publicBaseUrl + url;
    }

    private Optional<String> sendWithUrl(String absoluteUrl) {
        try {
            var uri = UriComponentsBuilder.fromHttpUrl(summaryEndpoint)
                    .queryParam("url", absoluteUrl)
                    .build(true)
                    .toUri();

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("url", absoluteUrl);
            // Some parsers expect recording_url as well; include it to be safe
            form.add("recording_url", absoluteUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(tokens.get());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
            log.info("Sending audio URL to summary endpoint url={}", absoluteUrl);

            ResponseEntity<SummaryResponse> resp = restTemplate.postForEntity(uri, entity, SummaryResponse.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return Optional.ofNullable(resp.getBody().summary());
            }
            log.warn("Summary endpoint responded with status {} and body {}", resp.getStatusCode(), resp.getBody());
        } catch (Exception ex) {
            log.error("Failed to call LLM summary endpoint {}", summaryEndpoint, ex);
        }
        return Optional.empty();
    }

    private Optional<String> sendWithFile(Path audioFile, String recordingUrl) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(audioFile);
        } catch (IOException ex) {
            log.error("Failed to read audio file {}: {}", audioFile, ex.getMessage());
            return Optional.empty();
        }

        String absoluteUrl = (recordingUrl != null && !recordingUrl.isBlank())
                ? toAbsoluteUrl(recordingUrl)
                : null;

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // Include both multipart file and URL metadata; the AI service can use either.
        var fileResource = new org.springframework.core.io.FileSystemResource(audioFile.toFile());
        body.add("file", fileResource);
        // Some services expect "audio" or "audio_file" as the key; include them as aliases.
        body.add("audio", fileResource);
        body.add("audio_file", fileResource);
        if (absoluteUrl != null) {
            body.add("url", absoluteUrl);
            body.add("recording_url", absoluteUrl);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(tokens.get());

        var uriBuilder = UriComponentsBuilder.fromHttpUrl(summaryEndpoint);
        if (absoluteUrl != null) {
            uriBuilder.queryParam("url", absoluteUrl);
        }

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Sending audio file to summary endpoint file={} size={} bytes url={}", audioFile, bytes.length, absoluteUrl);

        try {
            ResponseEntity<SummaryResponse> resp = restTemplate.postForEntity(uriBuilder.build(true).toUri(), entity, SummaryResponse.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return Optional.ofNullable(resp.getBody().summary());
            }
            log.warn("Summary endpoint responded with status {} and body {}", resp.getStatusCode(), resp.getBody());
        } catch (Exception ex) {
            log.error("Failed to call LLM summary endpoint {}", summaryEndpoint, ex);
        }
        return Optional.empty();
    }

    public record SummaryResponse(String summary) {}
}
