package io.vinta.agentic.tree.context;

import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.runtime.AgenticCumulativeData;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@With
@Slf4j
public class AgenticExecutionContext {
	private final AgenticContextState<NodeId, AgenticExecutionResult> globalExecutionResults;
	private final AgenticContextState<String, Object> currentState;

	private final AgenticContextState<NodeId, AgenticCumulativeData<?>> cumulativeData;


	private final AgenticContextState<String, String> currentNodeMetadata;
	private final String executionId;

	@Builder
	public AgenticExecutionContext(
			AgenticContextState<NodeId, AgenticExecutionResult> globalExecutionResults,
			AgenticContextState<String, Object> currentState,
			AgenticContextState<NodeId, AgenticCumulativeData<?>> cumulativeData,
			AgenticContextState<String, String> currentNodeMetadata,
			String executionId) {
        this.globalExecutionResults = Optional.ofNullable(globalExecutionResults).orElse(new AgenticContextState<>());
        this.currentState = Optional.ofNullable(currentState).orElse(new AgenticContextState<>());
        this.cumulativeData = Optional.ofNullable(cumulativeData).orElse(new AgenticContextState<>());
        this.currentNodeMetadata = Optional.ofNullable(currentNodeMetadata).orElse(new AgenticContextState<>());
        this.executionId = executionId;
	}

	public void trackExecutionResult(NodeId nodeId, AgenticExecutionResult result) {
		if (globalExecutionResults.getStateData().containsKey(nodeId)) {
			log.warn("Overwriting existing execution result for nodeId: {}", nodeId);
		}
		globalExecutionResults.getStateData().putIfAbsent(nodeId, result);
	}
}