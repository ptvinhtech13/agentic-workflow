package io.vinta.agentic.tree.execution;

public interface AgenticExecutionResultRepository {
	boolean isExistsById(String executionResultId);

	void save(String executionResultId, AgenticExecutionResult result);
}