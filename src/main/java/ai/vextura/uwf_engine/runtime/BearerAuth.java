package ai.vextura.uwf_engine.runtime;

/** Static bearer token auth. For dev/testing. Production: use M2MAuth. */
public class BearerAuth implements AuthProvider {
    private final String token;

    public BearerAuth(String token) {
        this.token = token != null ? token : "";
    }

    @Override
    public String getToken() {
        return token;
    }
}
