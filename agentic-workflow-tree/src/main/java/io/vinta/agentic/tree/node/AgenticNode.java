package io.vinta.agentic.tree.node;

import io.vinta.agentic.tree.context.AgenticContextState;
import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.execution.SimpleAgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;

import java.util.Map;

public interface AgenticNode {

	NodeId getNodeId();

	default AgenticExecutionResult execute(AgenticExecutionContext context) {
		try {
			onBefore(context);
			context = context.withCurrentNodeMetadata(new AgenticContextState<>(Map.of("NODE_ID", getNodeId().toString())));
			final var result = onExecute(context);
			onAfter(context, result);
			return result;
		} catch (Exception e) {
			onError(context);
			return new SimpleAgenticExecutionResult(getNodeId(), AgenticNodeStatus.FAILURE);
		}
	}

	default void onBefore(AgenticExecutionContext context) {
	}

	AgenticExecutionResult onExecute(AgenticExecutionContext context);

	default void onAfter(AgenticExecutionContext context, AgenticExecutionResult result) {
		context.trackExecutionResult(getNodeId(), result);
	}

	default void onError(AgenticExecutionContext context) {
	}
}