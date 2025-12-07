package io.vinta.agentic.tree.execution;

public interface AgenticExecutionMemoryRepository {
	boolean isExistsById(String executionResultId);

	void save(String executionResultId, AgenticExecutionResult result);
}