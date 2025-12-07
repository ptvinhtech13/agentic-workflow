package io.vinta.agentic.tree.execution;

import io.vinta.agentic.tree.node.AgenticNodeStatus;

public interface AgenticExecutionResult {
	AgenticNodeStatus getStatus();
}