package org.example.tas_backend.services;

import org.example.tas_backend.dtos.SubjectAliasDTO;
import org.example.tas_backend.dtos.SubjectTargetDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;


@Service
public class AiService {
    private final RestClient rest;
    private final OAuthClientTokenService tokens;

    public AiService(RestClient.Builder b,
                         OAuthClientTokenService tokens,
                         @Value("${ai.base-url}"+"/matcher") String baseUrl) {
        this.rest = b.baseUrl(baseUrl).build(); // base = http://127.0.0.1:8000/matcher
        this.tokens = tokens;
    }

    private RestClient.RequestHeadersSpec<?> auth(RestClient.RequestHeadersSpec<?> spec) {
        return spec.header("Authorization", "Bearer " + tokens.get());
    }

    // ---------- Targets ----------
    public List<SubjectTargetDTO> listTargets() {
        return auth(rest.get().uri("/targets/"))
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }

    public SubjectTargetDTO getTarget(String idOrUuid) {
        return auth(rest.get().uri("/targets/{id}/", idOrUuid))
                .retrieve().body(SubjectTargetDTO.class);
    }

    public SubjectTargetDTO createTarget(SubjectTargetDTO dto) {
        return auth(rest.post().uri("/targets/")
                .contentType(MediaType.APPLICATION_JSON).body(dto))
                .retrieve().body(SubjectTargetDTO.class);
    }

    public SubjectTargetDTO updateTarget(String idOrUuid, SubjectTargetDTO partial) {
        return auth(rest.patch().uri("/targets/{id}/", idOrUuid)
                .contentType(MediaType.APPLICATION_JSON).body(partial))
                .retrieve().body(SubjectTargetDTO.class);
    }

    public void deleteTarget(String idOrUuid) {
        auth(rest.delete().uri("/targets/{id}/"))
                .retrieve().toBodilessEntity();
    }

    // ---------- Aliases ----------
    public List<SubjectAliasDTO> listAliases(String language, String targetCode, String q) {
        var uri = new StringBuilder("/aliases/?");
        if (language != null) uri.append("language=").append(language).append("&");
        if (targetCode != null) uri.append("target_code=").append(targetCode).append("&");
        if (q != null) uri.append("q=").append(q);
        return auth(rest.get().uri(uri.toString()))
                .retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }

    public SubjectAliasDTO createAlias(SubjectAliasDTO dto) {
        return auth(rest.post().uri("/aliases/")
                .contentType(MediaType.APPLICATION_JSON).body(dto))
                .retrieve().body(SubjectAliasDTO.class);
    }

    public SubjectAliasDTO updateAlias(String idOrUuid, SubjectAliasDTO partial) {
        return auth(rest.patch().uri("/aliases/{id}/", idOrUuid)
                .contentType(MediaType.APPLICATION_JSON).body(partial))
                .retrieve().body(SubjectAliasDTO.class);
    }

    public void deleteAlias(String idOrUuid) {
        auth(rest.delete().uri("/aliases/{id}/"))
                .retrieve().toBodilessEntity();
    }
}