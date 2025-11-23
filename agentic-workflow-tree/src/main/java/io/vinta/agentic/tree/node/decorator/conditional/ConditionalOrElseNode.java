package io.vinta.agentic.tree.node.decorator.conditional;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.execution.SimpleAgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.decorator.DecoratorNode;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionalOrElseNode extends DecoratorNode {
	private final Predicate<AgenticExecutionContext> conditionPredicate;
	private final AgenticNode orElseChild;

	@Builder
	public ConditionalOrElseNode(NodeId nodeId, Predicate<AgenticExecutionContext> conditionPredicate,
			AgenticNode child, AgenticNode orElseChild) {
		super(nodeId, child);
		this.conditionPredicate = conditionPredicate;
		this.orElseChild = orElseChild;
	}

	@Override
	public AgenticExecutionResult onExecute(AgenticExecutionContext context) {
		log.debug("Executing ConditionalNode: {}", getNodeId());
		final var isConditionMet = conditionPredicate.test(context);
		final var executionResult = SimpleAgenticExecutionResult.builder()
				.nodeId(getNodeId())
				.build();
		final var childToExecute = isConditionMet ? getChild() : orElseChild;
		final var childResult = childToExecute.execute(context);
		log.debug("Condition {} for node {}, executed child {} with result: {}", isConditionMet ? "met" : "not met",
				getNodeId(), childToExecute.getNodeId(), childResult.getStatus());
		return executionResult.withStatus(childResult.getStatus());

	}
}