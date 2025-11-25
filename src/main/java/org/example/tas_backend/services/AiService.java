package org.example.tas_backend.services;

import org.example.tas_backend.dtos.SubjectAliasDTO;
import org.example.tas_backend.dtos.SubjectTargetDTO;
import org.example.tas_backend.dtos.MatchResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class AiService {
    private final RestClient rest;
    private final RestTemplate restTemplate;
    private final OAuthClientTokenService tokens;
    private final String matcherBase;
    private final ObjectMapper objectMapper;

    public AiService(RestClient.Builder b,
                         RestTemplate restTemplate,
                         OAuthClientTokenService tokens,
                         ObjectMapper objectMapper,
                         @Value("${ai.base-url}"+"/matcher") String baseUrl) {
        this.rest = b.baseUrl(baseUrl).build(); // base = http://127.0.0.1:8000/matcher
        this.restTemplate = restTemplate;
        this.tokens = tokens;
        this.matcherBase = baseUrl;
        this.objectMapper = objectMapper;
        log.info("AI service configured with baseUrl={}", baseUrl);
    }

    private RestClient.RequestHeadersSpec<?> auth(RestClient.RequestHeadersSpec<?> spec) {
        return spec.header("Authorization", "Bearer " + tokens.get());
    }

    // ---------- Targets ----------
    public List<SubjectTargetDTO> listTargets() {
        log.debug("Calling AI listTargets");
        return auth(rest.get().uri("/targets/"))
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }

    public SubjectTargetDTO getTarget(String idOrUuid) {
        log.debug("Calling AI getTarget id={}", idOrUuid);
        return auth(rest.get().uri("/targets/{id}/", idOrUuid))
                .retrieve().body(SubjectTargetDTO.class);
    }

    public SubjectTargetDTO createTarget(SubjectTargetDTO dto) {
        log.debug("Calling AI createTarget payload={}", dto);
        return auth(rest.post().uri("/targets/")
                .contentType(MediaType.APPLICATION_JSON).body(dto))
                .retrieve().body(SubjectTargetDTO.class);
    }

    public SubjectTargetDTO updateTarget(String idOrUuid, SubjectTargetDTO partial) {
        log.debug("Calling AI updateTarget id={} payload={}", idOrUuid, partial);
        return auth(rest.patch().uri("/targets/{id}/", idOrUuid)
                .contentType(MediaType.APPLICATION_JSON).body(partial))
                .retrieve().body(SubjectTargetDTO.class);
    }

    public void deleteTarget(String idOrUuid) {
        log.debug("Calling AI deleteTarget id={}", idOrUuid);
        auth(rest.delete().uri("/targets/{id}/", idOrUuid))
                .retrieve().toBodilessEntity();
    }

    // ---------- Aliases ----------
    public List<SubjectAliasDTO> listAliases(String language, String targetCode, String q) {
        var uri = new StringBuilder("/aliases/?");
        if (language != null) uri.append("language=").append(language).append("&");
        if (targetCode != null) uri.append("target_code=").append(targetCode).append("&");
        if (q != null) uri.append("q=").append(q);
        log.debug("Calling AI listAliases {}", uri);
        return auth(rest.get().uri(uri.toString()))
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }

    public SubjectAliasDTO createAlias(SubjectAliasDTO dto) {
        log.debug("Calling AI createAlias payload={}", dto);
        return auth(rest.post().uri("/aliases/")
                .contentType(MediaType.APPLICATION_JSON).body(dto))
                .retrieve().body(SubjectAliasDTO.class);
    }

    public SubjectAliasDTO updateAlias(String idOrUuid, SubjectAliasDTO partial) {
        log.debug("Calling AI updateAlias id={} payload={}", idOrUuid, partial);
        return auth(rest.patch().uri("/aliases/{id}/", idOrUuid)
                .contentType(MediaType.APPLICATION_JSON).body(partial))
                .retrieve().body(SubjectAliasDTO.class);
    }

    public void deleteAlias(String idOrUuid) {
        log.debug("Calling AI deleteAlias id={}", idOrUuid);
        auth(rest.delete().uri("/aliases/{id}/"))
                .retrieve().toBodilessEntity();
    }

    // ---------- Matching ----------
    public MatchResponseDTO matchSubjects(List<String> subjects) {
        return matchSubjects(subjects, null);
    }

    public MatchResponseDTO matchSubjects(List<String> subjects,
                                          List<java.util.Map<String, Object>> targets) {
        if (subjects == null || subjects.isEmpty()) {
            log.debug("matchSubjects called with empty subjects list");
            return null;
        }
        var payload = new java.util.HashMap<String, Object>();
        payload.put("subjects", subjects);
        if (targets != null && !targets.isEmpty()) {
            payload.put("targets", targets);
            log.debug("Including {} targets in matcher payload", targets.size());
        }
        log.debug("Calling AI matchSubjects payload={} ({} items)", payload, subjects.size());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize matcher payload", ex);
            return null;
        }
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // Use RestTemplate to ensure body is serialized and sent
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(tokens.get());
        headers.setContentLength(jsonBytes.length);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        MatchResponseDTO response = restTemplate.postForObject(
                matcherBase + "/match/",
                entity,
                MatchResponseDTO.class
        );

        log.debug("AI matchSubjects response={}", response);
        return response;
    }
}
