package io.vinta.agentic.tree.node.decorator.conditional;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.execution.SimpleAgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import io.vinta.agentic.tree.node.decorator.DecoratorNode;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionalNode extends DecoratorNode {
	private final Predicate<AgenticExecutionContext> conditionPredicate;

	@Builder
	public ConditionalNode(NodeId nodeId, Predicate<AgenticExecutionContext> conditionPredicate, AgenticNode child) {
		super(nodeId, child);
		this.conditionPredicate = conditionPredicate;
	}

	@Override
	public AgenticExecutionResult onExecute(AgenticExecutionContext context) {
		log.debug("Executing ConditionalNode: {}", getNodeId());
		final var isConditionMet = conditionPredicate.test(context);

		if (!isConditionMet) {
			log.debug("Condition not met for node {}, skipping child execution", getNodeId());
			return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SKIPPED);
		}
		final var childResult = getChild().execute(context);
		log.debug("Condition met for node {}, executed child with result: {}", getNodeId(), childResult.getStatus());
		return new SimpleAgenticExecutionResult<>(childResult.getStatus());

	}
}