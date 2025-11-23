package io.vinta.agentic.tree.execution;

import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Getter
@Builder
@With
@RequiredArgsConstructor
public class SimpleAgenticExecutionResult implements AgenticExecutionResult {
	private final NodeId nodeId;
	private final AgenticNodeStatus status;
}