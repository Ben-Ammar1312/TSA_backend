package org.example.tas_backend.services;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OAuthClientTokenService {
    private final RestClient rest;
    private final String tokenUrl, clientId, clientSecret;
    private volatile String token;
    private volatile long expiresAtMs;

    public OAuthClientTokenService(RestClient.Builder b,
                                   @Value("${keycloak.token-url}") String tokenUrl,
                                   @Value("${django.client-id}") String clientId,
                                   @Value("${django.client-secret}") String clientSecret) {
        this.rest = b.build();
        this.tokenUrl = tokenUrl; this.clientId = clientId; this.clientSecret = clientSecret;
    }

    public synchronized String get() {
        long now = System.currentTimeMillis();
        if (token != null && now < expiresAtMs - 30_000) return token;

        var body = "grant_type=client_credentials&client_id=" + clientId +
                "&client_secret=" + clientSecret;
        var resp = rest.post().uri(tokenUrl)
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve().body(Token.class);
        this.token = resp.access_token;
        this.expiresAtMs = now + resp.expires_in * 1000L;
        return token;
    }
    public record Token(String access_token, int expires_in) {}
}