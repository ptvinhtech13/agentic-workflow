package io.vinta.agentic.tree.node;

import io.vinta.agentic.tree.contants.AgenticConstants;
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
			context = context.withCurrentMetadata(new AgenticContextState<>(Map.of(AgenticConstants.NODE_ID, getNodeId()
					.toString())));
			context = onBefore(context);
			final var result = onExecute(context);
			onAfter(context, result);
			return result;
		} catch (Exception e) {
			onError(context);
			return new SimpleAgenticExecutionResult(getNodeId(), AgenticNodeStatus.FAILURE);
		}
	}

	default AgenticExecutionContext onBefore(AgenticExecutionContext context) {
		return context;
	}

	AgenticExecutionResult onExecute(AgenticExecutionContext context);

	default void onAfter(AgenticExecutionContext context, AgenticExecutionResult result) {
		context.saveGlobalResult(result);
	}

	default void onError(AgenticExecutionContext context) {
	}
}