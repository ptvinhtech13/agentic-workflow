package io.vinta.agentic.tree.context;

import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public class AgenticExecutionContext {
	// Global State
	// Current State
	// Metadata
	private final Map<NodeId, AgenticExecutionResult> globalResults = new ConcurrentHashMap<>();
	private final Map<String, Object> data;
	private final Map<String, Object> metadata;
	private final String executionId;

	@Builder
	public AgenticExecutionContext(String executionId) {
		this.executionId = executionId;
		this.data = new ConcurrentHashMap<>();
		this.metadata = new ConcurrentHashMap<>();
	}

	public void trackExecutionResult(NodeId nodeId, AgenticExecutionResult result) {
		if (globalResults.containsKey(nodeId)) {
			log.warn("Overwriting existing execution result for nodeId: {}", nodeId);
		}
		globalResults.put(nodeId, result);
	}
}