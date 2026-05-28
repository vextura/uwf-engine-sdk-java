package ai.vextura.uwf_engine.runtime;

/** Strategy interface for obtaining bearer tokens. */
public interface AuthProvider {
    /** Returns the current auth token. May perform token refresh internally. */
    String getToken();
}
