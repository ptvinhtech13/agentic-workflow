package io.vinta.agentic.tree.node.leaf.task;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.leaf.LeafNode;
import java.util.function.Function;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskNode extends LeafNode {
	private final Function<AgenticExecutionContext, AgenticExecutionResult> taskFunction;

	@Builder
	public TaskNode(NodeId nodeId, Function<AgenticExecutionContext, AgenticExecutionResult> taskFunction) {
		super(nodeId);
		this.taskFunction = taskFunction;
	}

	@Override
	public AgenticExecutionResult onExecute(AgenticExecutionContext context) {
		log.debug("Executing TaskNode: {}", getNodeId());
		final var result = taskFunction.apply(context);
		log.info("TaskNode {} completed with status: {}", getNodeId(), result.getStatus());
		return result;
	}
}