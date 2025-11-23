package io.vinta.agentic.tree.execution;

import io.vinta.agentic.tree.node.AgenticNodeStatus;
import io.vinta.agentic.tree.node.NodeId;

public interface AgenticExecutionResult {
	NodeId getNodeId();

	AgenticNodeStatus getStatus();
}