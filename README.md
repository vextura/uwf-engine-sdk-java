# uwf-engine-sdk-java

Official Java client for the **Vextura Workflow Engine API**.
Handles authentication, retries, and endpoint resolution automatically.

## Requirements

- Java 21+
- Maven 3.8+ (or Gradle)

## Installation

### 1. Add the Vextura Maven registry

The package is hosted on GitHub Packages. Add the repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>vextura-github</id>
        <name>Vextura GitHub Packages</name>
        <url>https://maven.pkg.github.com/vextura/uwf-engine-sdk-java</url>
    </repository>
</repositories>
```

Or for Gradle, in `build.gradle`:

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/vextura/uwf-engine-sdk-java")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### 2. Configure credentials

GitHub Packages requires a Personal Access Token (PAT) with `read:packages` scope.

Add to `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>vextura-github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_PAT</password>
        </server>
    </servers>
</settings>
```

### 3. Add the dependency

```xml
<dependency>
    <groupId>ai.vextura</groupId>
    <artifactId>uwf-engine-sdk-java</artifactId>
    <version>1.2.0</version>
</dependency>
```

Or with Gradle:

```groovy
implementation 'ai.vextura:uwf-engine-sdk-java:1.2.0'
```

---

## Setup

```java
import ai.vextura.uwf_engine.UwfEngineClient;
import ai.vextura.uwf_engine.runtime.BearerAuth;

UwfEngineClient client = UwfEngineClient.withEndpoint(
    "https://api.vextura.ai",   // or your deployment's vex-gate URL
    new BearerAuth("YOUR_API_TOKEN")
);
```

> If you are running inside the Vextura platform, use `new UwfEngineClient(ripUrl, auth)` instead — the client resolves the endpoint automatically via service discovery.

---

## Usage

### Check service health

```java
HealthResponse health = client.healthCheck();
System.out.println(health.status); // "healthy"
System.out.println(health.nats.ok); // true
```

### List available workflows

```java
ListWorkflowsResponse resp = client.listWorkflows();
for (WorkflowInfo w : resp.workflows) {
    System.out.println(w.id + " — " + w.name);
}
```

### Get a workflow definition

```java
WorkflowIdInput req = new WorkflowIdInput();
req.workflowId = "kaspi-payment-v1";

WorkflowInfo wf = client.getWorkflow(req);
System.out.println(wf.name + " " + wf.version);
```

### Execute a workflow (synchronous)

Blocks until the workflow finishes and returns the final result.
Automatically retries up to 3 times on server errors.

```java
ExecuteWorkflowInput req = new ExecuteWorkflowInput();
req.workflowId = "kaspi-payment-v1";
req.inputData = Map.of(
    "amount",   5000,
    "currency", "KZT",
    "sender",   "acme-service"
);

ExecutionResult result = client.executeWorkflow(req);
System.out.println(result.status);  // "completed"
System.out.println(result.output);
```

### Execute a workflow (asynchronous)

Returns a `runId` immediately. Use it to poll status or receive a webhook callback.

```java
AsyncExecuteInput req = new AsyncExecuteInput();
req.workflowId  = "kaspi-payment-v1";
req.inputData   = Map.of("amount", 5000);
req.callbackUrl = "https://your-service.com/webhook"; // optional

AsyncExecuteResponse resp = client.asyncExecuteWorkflow(req);
String runId = resp.runId;
```

### Poll execution status

```java
RunIdInput req = new RunIdInput();
req.runId = runId;

ExecutionStatus status = client.getExecutionStatus(req);
// status.status: "pending" | "running" | "completed" | "failed"
System.out.println(status.status + " step=" + status.currentStep);
```

### Get execution result

Available once `status == "completed"`.

```java
ExecutionResult result = client.getExecutionResult(new RunIdInput() {{ runId = "run-123"; }});
System.out.println(result.output);
```

### Get execution metrics

```java
ExecutionMetrics m = client.getExecutionMetrics(new RunIdInput() {{ runId = "run-123"; }});
System.out.printf("steps: %d/%d, duration: %dms%n",
    m.completedSteps, m.totalSteps, m.durationMs);
```

### List executions

```java
ListExecutionsInput req = new ListExecutionsInput();
req.workflowId = "kaspi-payment-v1"; // optional
req.status     = "completed";        // optional
req.limit      = 20;
req.offset     = 0;

ListExecutionsResponse resp = client.listExecutions(req);
for (ExecutionStatus e : resp.executions) {
    System.out.println(e.runId + " " + e.status);
}
```

### Cancel, pause, resume, retry

```java
RunIdInput req = new RunIdInput();
req.runId = runId;

AckResponse ack    = client.cancelExecution(req);
AckResponse ack2   = client.pauseExecution(req);
AckResponse ack3   = client.resumeExecution(req);
AsyncExecuteResponse r = client.retryExecution(req);
```

---

## Error handling

The client throws `SdkException` (a `RuntimeException`) on HTTP errors:

```java
import ai.vextura.uwf_engine.runtime.SdkException;

try {
    ExecutionResult result = client.executeWorkflow(req);
} catch (SdkException e) {
    System.err.println("HTTP " + e.getStatusCode() + ": " + e.getMessage());
}
```

| Status | Meaning |
|--------|---------|
| 404 | Workflow or execution not found |
| 409 | Execution not yet complete (result not available) |
| 500 | Workflow execution failed |

---

## License

Apache 2.0
