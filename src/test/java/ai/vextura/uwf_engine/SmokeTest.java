package ai.vextura.uwf_engine;

import ai.vextura.uwf_engine.models.*;
import ai.vextura.uwf_engine.runtime.BearerAuth;

import java.util.Map;
import java.util.HashMap;

/**
 * Hand-written smoke test for the generated uwf-engine Java SDK.
 * Run: mvn test-compile exec:java -Dexec.mainClass=ai.vextura.uwf_engine.SmokeTest \
 *      -Dexec.classpathScope=test
 */
public class SmokeTest {

    // Endpoint resolved at runtime via RIP — never hardcoded.
    // Obtain before running: vexctl rip resolve uwf-engine <region>
    // Pass as: -e UWF_ENDPOINT=<resolved-url>
    static final String ENDPOINT;
    static {
        String ep = System.getenv("UWF_ENDPOINT");
        if (ep == null || ep.isBlank()) {
            System.err.println("ERROR: UWF_ENDPOINT env var is required.");
            System.err.println("       Resolve it first: vexctl rip resolve uwf-engine <region>");
            System.exit(1);
        }
        ENDPOINT = ep;
    }
    static final BearerAuth NO_AUTH = new BearerAuth("");

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {
        UwfEngineClient client = UwfEngineClient.withEndpoint(ENDPOINT, NO_AUTH);

        test("HealthCheck", () -> {
            HealthResponse r = client.healthCheck();
            assert "healthy".equals(r.status) : "expected status=healthy, got " + r.status;
            assert r.nats != null && Boolean.TRUE.equals(r.nats.ok) : "expected nats.ok=true";
            assert r.redis != null && Boolean.TRUE.equals(r.redis.ok) : "expected redis.ok=true";
            System.out.println("  status=" + r.status + " nats.ok=" + r.nats.ok + " redis.ok=" + r.redis.ok);
        });

        test("ListWorkflows", () -> {
            ListWorkflowsResponse r = client.listWorkflows();
            assert r.workflows != null : "workflows list is null";
            System.out.println("  workflows count=" + r.workflows.size());
            for (WorkflowInfo w : r.workflows) {
                System.out.println("    id=" + w.id + " name=" + w.name + " status=" + w.status);
            }
        });

        test("GetWorkflow (kaspi-payment-v1)", () -> {
            WorkflowIdInput req = new WorkflowIdInput();
            req.workflowId = "kaspi-payment-v1";
            WorkflowInfo w = client.getWorkflow(req);
            assert w.id != null && !w.id.isEmpty() : "workflow id is empty";
            System.out.println("  id=" + w.id + " name=" + w.name + " version=" + w.version);
        });

        test("ExecuteWorkflow (kaspi-payment-v1)", () -> {
            ExecuteWorkflowInput req = new ExecuteWorkflowInput();
            req.workflowId = "kaspi-payment-v1";
            Map<String, Object> input = new HashMap<>();
            input.put("amount", 1000);
            input.put("currency", "KZT");
            input.put("sender", "smoke-test-java");
            req.inputData = input;
            ExecutionResult r = client.executeWorkflow(req);
            assert r.runId != null && !r.runId.isEmpty() : "runId is empty";
            System.out.println("  runId=" + r.runId + " status=" + r.status + " durationMs=" + r.durationMs);

            // Use the runId for follow-up tests
            System.setProperty("smoke.runId", r.runId);
        });

        test("GetExecutionStatus", () -> {
            String runId = System.getProperty("smoke.runId");
            if (runId == null) { System.out.println("  SKIP (no runId)"); return; }
            RunIdInput req = new RunIdInput();
            req.runId = runId;
            ExecutionStatus s = client.getExecutionStatus(req);
            assert s.runId != null : "runId null in status";
            assert s.status != null : "status null";
            System.out.println("  runId=" + s.runId + " status=" + s.status + " step=" + s.currentStep);
        });

        test("ListExecutions", () -> {
            ListExecutionsInput req = new ListExecutionsInput();
            req.limit = 5;
            ListExecutionsResponse r = client.listExecutions(req);
            assert r.executions != null : "executions list is null";
            System.out.println("  executions count=" + r.executions.size() + " total=" + r.total);
        });

        System.out.println("\n=== Java SDK Smoke Tests: " + passed + "/" + (passed + failed) + " passed ===");
        if (failed > 0) System.exit(1);
    }

    static void test(String name, Runnable fn) {
        System.out.print("[TEST] " + name + " ... ");
        try {
            fn.run();
            System.out.println("PASS");
            passed++;
        } catch (AssertionError | Exception e) {
            System.out.println("FAIL: " + e.getMessage());
            failed++;
        }
    }
}
