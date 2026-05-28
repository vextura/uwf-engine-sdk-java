package com.vextura.uwf_engine.runtime;

/** Configures and executes retry logic for SDK operations. */
public class RetryPolicy {

    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final boolean jitter;

    public RetryPolicy(int maxAttempts, long baseDelayMs, long maxDelayMs, boolean jitter) {
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.jitter = jitter;
    }

    @FunctionalInterface
    public interface Operation<T> {
        T execute();
    }

    public static <T> T execute(RetryPolicy policy, Operation<T> op) {
        SdkException lastEx = null;
        for (int attempt = 0; attempt < policy.maxAttempts; attempt++) {
            try {
                return op.execute();
            } catch (SdkException e) {
                lastEx = e;
                int status = e.getStatusCode();
                // Don't retry 4xx
                if (status >= 400 && status < 500) throw e;
                if (attempt < policy.maxAttempts - 1) {
                    long delay = Math.min(policy.baseDelayMs * (1L << attempt), policy.maxDelayMs);
                    if (policy.jitter) delay = (long)(Math.random() * delay);
                    try { Thread.sleep(delay); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SdkException("Interrupted during retry backoff", ie);
                    }
                }
            }
        }
        throw lastEx;
    }
}
