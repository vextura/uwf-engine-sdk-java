package ai.vextura.uwf_engine.runtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

/**
 * Machine-to-machine AuthProvider for production use.
 *
 * Fetches a JWT from vex-auth using the client_credentials grant and caches it
 * until 60 seconds before expiry, then refreshes automatically on the next call.
 * Thread-safe — safe to share across concurrent SDK calls.
 *
 * Usage:
 *   AuthProvider auth = new M2MAuth(
 *       System.getenv("VEX_AUTH_URL"),    // e.g. http://vex-auth.vex.internal:8095
 *       System.getenv("VEX_CLIENT_ID"),
 *       System.getenv("VEX_CLIENT_SECRET")
 *   );
 *   UwfEngineClient client = UwfEngineClient.withEndpoint(endpoint, auth);
 */
public class M2MAuth implements AuthProvider {

    private static final int REFRESH_MARGIN_SECONDS = 60;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final String authUrl;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken = null;
    private volatile Instant expiresAt = Instant.EPOCH;

    public M2MAuth(String authUrl, String clientId, String clientSecret) {
        if (authUrl == null || authUrl.isBlank())
            throw new IllegalArgumentException("VEX_AUTH_URL is required");
        if (clientId == null || clientId.isBlank())
            throw new IllegalArgumentException("VEX_CLIENT_ID is required");
        if (clientSecret == null || clientSecret.isBlank())
            throw new IllegalArgumentException("VEX_CLIENT_SECRET is required");

        this.authUrl      = authUrl.replaceAll("/$", "");
        this.clientId     = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public synchronized String getToken() {
        if (cachedToken == null || Instant.now().isAfter(expiresAt.minusSeconds(REFRESH_MARGIN_SECONDS))) {
            refresh();
        }
        return cachedToken;
    }

    private void refresh() {
        try {
            String body = MAPPER.writeValueAsString(new TokenRequest(clientId, clientSecret));
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(authUrl + "/auth/token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new SdkException("M2MAuth: token request failed (" + resp.statusCode() + "): " + resp.body());
            }

            TokenResponse tr = MAPPER.readValue(resp.body(), TokenResponse.class);
            this.cachedToken = tr.accessToken;
            this.expiresAt   = Instant.now().plusSeconds(tr.expiresIn);
        } catch (SdkException e) {
            throw e;
        } catch (Exception e) {
            throw new SdkException("M2MAuth: failed to fetch token: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TokenRequest {
        @JsonProperty("grant_type")  public final String grantType  = "client_credentials";
        @JsonProperty("client_id")   public final String clientId;
        @JsonProperty("client_secret") public final String clientSecret;

        TokenRequest(String clientId, String clientSecret) {
            this.clientId     = clientId;
            this.clientSecret = clientSecret;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TokenResponse {
        @JsonProperty("access_token") public String accessToken;
        @JsonProperty("expires_in")   public long   expiresIn;
    }
}
