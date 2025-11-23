package io.vinta.agentic.tree.execution;

import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNodeStatus;

public interface AgenticExecutionResult {
	NodeId getNodeId();

	AgenticNodeStatus getStatus();
}