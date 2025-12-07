package io.vinta.agentic.tree.execution;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAgenticExecutionMemoryRepository implements AgenticExecutionMemoryRepository {
	private final Map<String, AgenticExecutionResult> agenticExecutionResultMap = new ConcurrentHashMap<>();

	@Override
	public boolean isExistsById(String executionResultId) {
		if (executionResultId == null || executionResultId.isEmpty()) {
			throw new IllegalArgumentException("ExecutionResultId cannot be null or empty");
		}
		return agenticExecutionResultMap.containsKey(executionResultId);
	}

	@Override
	public void save(String executionResultId, AgenticExecutionResult result) {
		if (executionResultId == null || executionResultId.isEmpty()) {
			throw new IllegalArgumentException("ExecutionResultId cannot be null or empty");
		}
		if (result == null) {
			throw new IllegalArgumentException("AgenticExecutionResult cannot be null");
		}
		agenticExecutionResultMap.put(executionResultId, result);
	}

	@Override
	public Optional<AgenticExecutionResult> findByExecutionResultId(String executionResultId) {
		if (executionResultId == null || executionResultId.isEmpty()) {
			throw new IllegalArgumentException("ExecutionResultId cannot be null or empty");
		}
		return Optional.ofNullable(agenticExecutionResultMap.get(executionResultId));
	}
}