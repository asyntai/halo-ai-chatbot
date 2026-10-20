package com.asyntai.halo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import reactor.core.publisher.Mono;

/**
 * Asks asyntai.com whether the sign-in for one connect attempt is finished.
 *
 * <p>The request runs on the Halo server, and the reply is read as data only.
 * No code from asyntai.com ever runs in the Halo console.</p>
 */
public class ConnectStatusClient {

    public static final String DEFAULT_ORIGIN = "https://asyntai.com";
    static final String CALLBACK = "asyntaiHalo";

    private static final Pattern STATE = Pattern.compile("^halo_[0-9a-f]{16}$");
    private static final Pattern WRAPPED = Pattern.compile(
        "^\\s*" + CALLBACK + "\\((.*)\\)\\s*;?\\s*$", Pattern.DOTALL);
    private static final int MAX_BODY = 16 * 1024;

    // Strict: text after the JSON value makes the whole reply invalid.
    private final ObjectMapper mapper = new ObjectMapper()
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private final String origin;

    public ConnectStatusClient(String origin) {
        this.origin = origin;
    }

    /**
     * asyntai.com, or a local test server named in the system property
     * {@code asyntai.origin} (only http://127.0.0.1 or http://localhost).
     */
    public static String originFromSystem() {
        String value = System.getProperty("asyntai.origin", "");
        if (value.matches("^http://(127\\.0\\.0\\.1|localhost)(:\\d{1,5})?$")) {
            return value;
        }
        return DEFAULT_ORIGIN;
    }

    public static boolean isValidState(String state) {
        return state != null && STATE.matcher(state).matches();
    }

    /** The status for this state. Fails with an error when asyntai.com cannot be reached. */
    public Mono<ConnectStatus> fetch(String state) {
        if (!isValidState(state)) {
            return Mono.error(new IllegalArgumentException("invalid state"));
        }
        URI uri = URI.create(origin + "/connect-status.js?state="
            + URLEncoder.encode(state, StandardCharsets.UTF_8) + "&cb=" + CALLBACK);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/javascript, application/json")
            .GET()
            .build();
        return Mono.fromFuture(() -> http.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
            .map(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("status " + response.statusCode());
                }
                return parse(response.body());
            });
    }

    /**
     * Reads the reply {@code asyntaiHalo(null)} or {@code asyntaiHalo({...})}
     * as JSON. Every value is checked; anything unexpected counts as "not
     * connected yet".
     */
    ConnectStatus parse(String body) {
        if (body == null || body.length() > MAX_BODY) {
            return ConnectStatus.notConnected();
        }
        Matcher m = WRAPPED.matcher(body);
        if (!m.matches()) {
            return ConnectStatus.notConnected();
        }
        JsonNode data;
        try {
            data = mapper.readTree(m.group(1));
        } catch (Exception e) {
            return ConnectStatus.notConnected();
        }
        if (data == null || !data.isObject()) {
            return ConnectStatus.notConnected();
        }
        String siteId = data.path("site_id").asText("");
        if (!WidgetTag.isValidWidgetId(siteId)) {
            return ConnectStatus.notConnected();
        }
        String scriptUrl = data.path("script_url").asText("");
        if (!WidgetTag.isTrustedScriptUrl(scriptUrl)) {
            scriptUrl = "";
        }
        String email = data.path("account_email").asText("");
        if (email.length() > 254 || !email.matches("^[^\\s<>\"']*$")) {
            email = "";
        }
        return new ConnectStatus(true, siteId, scriptUrl, email);
    }

    public record ConnectStatus(boolean connected, String siteId, String scriptUrl,
                                String accountEmail) {
        static ConnectStatus notConnected() {
            return new ConnectStatus(false, "", "", "");
        }
    }
}
