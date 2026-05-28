package ai.vextura.uwf_engine.hooks;

import ai.vextura.uwf_engine.models.*;
import ai.vextura.uwf_engine.runtime.*;

/**
 * UwfEngineClient lifecycle hooks — customize behavior per operation.
 * This file is protected and will NOT be overwritten by vexctl sdk generate.
 *
 * Override methods here to add logging, metrics, validation, etc.
 */
public class UwfEngineClientHooks {

    // Called before executeWorkflow
    public ExecuteWorkflowInput beforeExecuteWorkflow(ExecuteWorkflowInput req) { return req; }

    // Called after executeWorkflow
    public ExecutionResult afterExecuteWorkflow(ExecutionResult resp) { return resp; }

    // Called before asyncExecuteWorkflow
    public AsyncExecuteInput beforeAsyncExecuteWorkflow(AsyncExecuteInput req) { return req; }

    // Called after asyncExecuteWorkflow
    public AsyncExecuteResponse afterAsyncExecuteWorkflow(AsyncExecuteResponse resp) { return resp; }

    // Called before getExecutionStatus
    public RunIdInput beforeGetExecutionStatus(RunIdInput req) { return req; }

    // Called after getExecutionStatus
    public ExecutionStatus afterGetExecutionStatus(ExecutionStatus resp) { return resp; }

    // Called before getExecutionResult
    public RunIdInput beforeGetExecutionResult(RunIdInput req) { return req; }

    // Called after getExecutionResult
    public ExecutionResult afterGetExecutionResult(ExecutionResult resp) { return resp; }

    // Called before getExecutionMetrics
    public RunIdInput beforeGetExecutionMetrics(RunIdInput req) { return req; }

    // Called after getExecutionMetrics
    public ExecutionMetrics afterGetExecutionMetrics(ExecutionMetrics resp) { return resp; }

    // Called before cancelExecution
    public RunIdInput beforeCancelExecution(RunIdInput req) { return req; }

    // Called after cancelExecution
    public AckResponse afterCancelExecution(AckResponse resp) { return resp; }

    // Called before pauseExecution
    public RunIdInput beforePauseExecution(RunIdInput req) { return req; }

    // Called after pauseExecution
    public AckResponse afterPauseExecution(AckResponse resp) { return resp; }

    // Called before resumeExecution
    public RunIdInput beforeResumeExecution(RunIdInput req) { return req; }

    // Called after resumeExecution
    public AckResponse afterResumeExecution(AckResponse resp) { return resp; }

    // Called before retryExecution
    public RunIdInput beforeRetryExecution(RunIdInput req) { return req; }

    // Called after retryExecution
    public AsyncExecuteResponse afterRetryExecution(AsyncExecuteResponse resp) { return resp; }

    // Called before listExecutions
    public ListExecutionsInput beforeListExecutions(ListExecutionsInput req) { return req; }

    // Called after listExecutions
    public ListExecutionsResponse afterListExecutions(ListExecutionsResponse resp) { return resp; }

    // Called before listWorkflows
    public void beforeListWorkflows() {}

    // Called after listWorkflows
    public ListWorkflowsResponse afterListWorkflows(ListWorkflowsResponse resp) { return resp; }

    // Called before getWorkflow
    public WorkflowIdInput beforeGetWorkflow(WorkflowIdInput req) { return req; }

    // Called after getWorkflow
    public WorkflowInfo afterGetWorkflow(WorkflowInfo resp) { return resp; }

    // Called before healthCheck
    public void beforeHealthCheck() {}

    // Called after healthCheck
    public HealthResponse afterHealthCheck(HealthResponse resp) { return resp; }

}
