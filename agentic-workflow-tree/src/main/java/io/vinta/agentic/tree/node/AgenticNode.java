package io.vinta.agentic.tree.node;

import io.vinta.agentic.tree.configuration.AgenticNodeConfiguration;
import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;

public interface AgenticNode {

	NodeId getNodeId();

	AgenticNodeConfiguration getConfiguration();

	AgenticExecutionResult execute(AgenticNodeConfiguration configuration, AgenticExecutionContext context);

	default void onBefore(AgenticNodeConfiguration config, AgenticExecutionContext context) {
	}

	default void onAfter(AgenticNodeConfiguration config, AgenticExecutionContext context) {
	}
}