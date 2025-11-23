package io.vinta.agentic.tree.context;

import io.vinta.agentic.tree.node.NodeId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@AllArgsConstructor
@With
public class AgenticExecutionContext {
	// Global State
	// Current State
	// Metadata
	private final Map<NodeId, Object> globalState = new ConcurrentHashMap<>();
	private final Map<String, Object> data;
	private final Map<String, Object> metadata;
	private final String executionId;

	@Builder
	public AgenticExecutionContext(String executionId) {
		this.executionId = executionId;
		this.data = new ConcurrentHashMap<>();
		this.metadata = new ConcurrentHashMap<>();
	}

}