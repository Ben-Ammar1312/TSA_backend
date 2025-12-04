package org.example.tas_backend;

import org.example.tas_backend.services.CallSummaryService;
import org.example.tas_backend.services.OAuthClientTokenService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CallSummaryServiceTests {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private OAuthClientTokenService tokens;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        tokens = Mockito.mock(OAuthClientTokenService.class);
        Mockito.when(tokens.get()).thenReturn("dummy-token");
    }

    @Test
    void sendsUrlAsFormPayload() throws Exception {
        Path dummyAudio = Files.createTempFile("audio", ".webm");
        dummyAudio.toFile().deleteOnExit();

        String endpoint = "http://localhost:9999/matcher/summarize/audio";
        CallSummaryService svc = new CallSummaryService(restTemplate, tokens, "http://localhost:9999", endpoint, "http://public");

        server.expect(requestTo(Matchers.containsString(endpoint + "?url=http://public/uploads/test.webm")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(Matchers.containsString("url=http://public/uploads/test.webm")))
                .andExpect(content().string(Matchers.containsString("recording_url=http://public/uploads/test.webm")))
                .andRespond(withSuccess("{\"summary\":\"ok\"}", MediaType.APPLICATION_JSON));

        Optional<String> summary = svc.summarize(dummyAudio, "/uploads/test.webm");
        server.verify();
        assertThat(summary).contains("ok");
    }

    @Test
    void sendsFileWhenUrlMissing() throws Exception {
        Path temp = Files.createTempFile("audio", ".webm");
        Files.writeString(temp, "data");
        temp.toFile().deleteOnExit();

        String endpoint = "http://localhost:9998/matcher/summarize/audio";
        CallSummaryService svc = new CallSummaryService(restTemplate, tokens, "http://localhost:9998", endpoint, "http://public");

        server.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andExpect(content().string(Matchers.containsString(temp.getFileName().toString())))
                .andRespond(withSuccess("{\"summary\":\"file-ok\"}", MediaType.APPLICATION_JSON));

        Optional<String> summary = svc.summarize(temp, null);
        server.verify();
        assertThat(summary).contains("file-ok");
    }

    @Test
    void fallsBackToFileWhenUrlCallFails() throws Exception {
        Path temp = Files.createTempFile("audio", ".webm");
        Files.writeString(temp, "data");
        temp.toFile().deleteOnExit();

        String endpoint = "http://localhost:9997/matcher/summarize/audio";
        CallSummaryService svc = new CallSummaryService(restTemplate, tokens, "http://localhost:9997", endpoint, "http://public");

        server.expect(requestTo(Matchers.containsString(endpoint + "?url=http://public/uploads/fail-then-file.webm")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(Matchers.containsString("url=http://public/uploads/fail-then-file.webm")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        server.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andExpect(content().string(Matchers.containsString("recording_url=http://public/uploads/fail-then-file.webm")))
                .andRespond(withSuccess("{\"summary\":\"file-ok\"}", MediaType.APPLICATION_JSON));

        Optional<String> summary = svc.summarize(temp, "/uploads/fail-then-file.webm");
        server.verify();
        assertThat(summary).contains("file-ok");
    }

    @Test
    void fallsBackToFileWhenUrlReturnsNoContent() throws Exception {
        Path temp = Files.createTempFile("audio", ".webm");
        Files.writeString(temp, "data");
        temp.toFile().deleteOnExit();

        String endpoint = "http://localhost:9994/matcher/summarize/audio";
        CallSummaryService svc = new CallSummaryService(restTemplate, tokens, "http://localhost:9994", endpoint, "http://public");

        server.expect(requestTo(Matchers.containsString(endpoint + "?url=http://public/uploads/204-then-file.webm")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        server.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andExpect(content().string(Matchers.containsString("recording_url=http://public/uploads/204-then-file.webm")))
                .andRespond(withSuccess("{\"summary\":\"file-ok\"}", MediaType.APPLICATION_JSON));

        Optional<String> summary = svc.summarize(temp, "/uploads/204-then-file.webm");
        server.verify();
        assertThat(summary).contains("file-ok");
    }

    @Test
    void returnsEmptyOnNoContentResponse() throws Exception {
        Path temp = Files.createTempFile("audio", ".webm");
        Files.writeString(temp, "data");
        temp.toFile().deleteOnExit();

        String endpoint = "http://localhost:9996/matcher/summarize/audio";
        CallSummaryService svc = new CallSummaryService(restTemplate, tokens, "http://localhost:9996", endpoint, "http://public");

        server.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        Optional<String> summary = svc.summarize(temp, null);
        server.verify();
        assertThat(summary).isEmpty();
    }

    @Test
    void buildsAbsoluteUrlFromRelativeWithoutLeadingSlash() throws Exception {
        Path dummyAudio = Files.createTempFile("audio", ".webm");
        dummyAudio.toFile().deleteOnExit();

        String endpoint = "http://localhost:9995/matcher/summarize/audio";
        CallSummaryService svc = new CallSummaryService(restTemplate, tokens, "http://localhost:9995", endpoint, "http://public");

        server.expect(requestTo(Matchers.containsString(endpoint + "?url=http://public/uploads/relative.webm")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(Matchers.containsString("url=http://public/uploads/relative.webm")))
                .andRespond(withSuccess("{\"summary\":\"ok\"}", MediaType.APPLICATION_JSON));

        Optional<String> summary = svc.summarize(dummyAudio, "uploads/relative.webm");
        server.verify();
        assertThat(summary).contains("ok");
    }
}
