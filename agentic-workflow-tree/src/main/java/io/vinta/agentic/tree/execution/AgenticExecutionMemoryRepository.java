package io.vinta.agentic.tree.execution;

import java.util.Optional;

public interface AgenticExecutionMemoryRepository {
	boolean isExistsById(String executionResultId);

	void save(String executionResultId, AgenticExecutionResult result);

	Optional<AgenticExecutionResult> findByExecutionResultId(String executionResultId);
}