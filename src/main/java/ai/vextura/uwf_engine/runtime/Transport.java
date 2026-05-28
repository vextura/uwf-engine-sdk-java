package ai.vextura.uwf_engine.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** HTTP transport with RIP endpoint resolution and 30s cache. */
public class Transport {

    private final String serviceName;
    private final String ripUrl;
    private volatile String cachedEndpoint;
    private volatile long cacheExpiry = 0;
    private static final long CACHE_TTL_MS = 30_000;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Transport(String serviceName, String ripUrl) {
        this.serviceName = serviceName;
        this.ripUrl = ripUrl;
    }

    /** Bypass RIP — use a fixed endpoint (local dev / smoke tests). */
    public void setFixedEndpoint(String endpoint) {
        this.cachedEndpoint = endpoint;
        this.cacheExpiry = Long.MAX_VALUE;
    }

    public HttpResponse<String> doRequest(String method, String path, String token, String body) {
        String endpoint = resolveEndpoint();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");
            if (token != null && !token.isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
            }
            if ("GET".equals(method) || "DELETE".equals(method)) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(
                    body != null ? body : "{}"));
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SdkException("Request interrupted", e);
        } catch (Exception e) {
            throw new SdkException("Transport error: " + e.getMessage(), e);
        }
    }

    private synchronized String resolveEndpoint() {
        if (cachedEndpoint != null && System.currentTimeMillis() < cacheExpiry) {
            return cachedEndpoint;
        }
        try {
            String url = ripUrl.replaceAll("/$", "") + "/rip/services/" + serviceName;
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new SdkException("RIP lookup failed: HTTP " + resp.statusCode());
            }
            JsonNode root = MAPPER.readTree(resp.body());
            JsonNode deployments = root.get("deployments");
            if (deployments == null || !deployments.isArray() || deployments.isEmpty()) {
                throw new SdkException("No deployments found for service: " + serviceName);
            }
            String endpoint = null;
            for (JsonNode d : deployments) {
                JsonNode ep = d.get("endpoint");
                if (ep != null && !ep.isNull() && !ep.asText().isEmpty()) {
                    endpoint = ep.asText();
                    break;
                }
            }
            if (endpoint == null) {
                throw new SdkException("No endpoint in RIP for service: " + serviceName);
            }
            cachedEndpoint = endpoint;
            cacheExpiry = System.currentTimeMillis() + CACHE_TTL_MS;
            return cachedEndpoint;
        } catch (SdkException e) {
            throw e;
        } catch (Exception e) {
            throw new SdkException("RIP resolution failed: " + e.getMessage(), e);
        }
    }
}
