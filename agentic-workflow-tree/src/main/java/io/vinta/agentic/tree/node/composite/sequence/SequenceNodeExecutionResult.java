package io.vinta.agentic.tree.node.composite.sequence;

import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Getter
@With
@Builder
public class SequenceNodeExecutionResult implements AgenticExecutionResult {
	private final NodeId nodeId;
	private final AgenticNodeStatus status;
	private final Map<NodeId, AgenticExecutionResult> childrenResults;
}