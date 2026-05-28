package ai.vextura.uwf_engine.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;

/** Base exception for all SDK errors. Wraps transport, auth, and HTTP errors. */
public class SdkException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public SdkException(String message) { super(message); this.statusCode = 0; this.responseBody = null; }
    public SdkException(String message, Throwable cause) { super(message, cause); this.statusCode = 0; this.responseBody = null; }
    public SdkException(int statusCode, String body) {
        super("HTTP " + statusCode + ": " + body);
        this.statusCode = statusCode;
        this.responseBody = body;
    }

    public int getStatusCode() { return statusCode; }
    public String getResponseBody() { return responseBody; }

    public static SdkException fromResponse(HttpResponse<String> resp) {
        return new SdkException(resp.statusCode(), resp.body());
    }

    public static <T> T parseJson(ObjectMapper mapper, String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new SdkException("JSON parse error: " + e.getMessage(), e);
        }
    }

    public static String toJson(ObjectMapper mapper, Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new SdkException("JSON serialize error: " + e.getMessage(), e);
        }
    }
}
