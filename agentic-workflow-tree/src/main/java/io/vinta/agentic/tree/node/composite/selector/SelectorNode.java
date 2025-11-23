package io.vinta.agentic.tree.node.composite.selector;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import io.vinta.agentic.tree.node.composite.CompositeNode;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Selector node executes children in order until one succeeds
 * Returns FAILURE only if all children fail
 * Similar to OR logic or fallback pattern
 */
@Slf4j
public class SelectorNode extends CompositeNode {

	private static final Predicate<AgenticExecutionResult> DEFAULT_SELECTIVE_PREDICATE = executionResult -> AgenticNodeStatus.SUCCESS
			.equals(executionResult.getStatus());

	private final Predicate<AgenticExecutionResult> selectiveNodePredicate;

	@Builder
	public SelectorNode(NodeId nodeId, Predicate<AgenticExecutionResult> selectiveNodePredicate,
			AgenticNode... children) {
		super(nodeId, children);
		this.selectiveNodePredicate = Optional.ofNullable(selectiveNodePredicate)
				.orElse(DEFAULT_SELECTIVE_PREDICATE);
	}

	@Override
	public AgenticExecutionResult onExecute(AgenticExecutionContext context) {
		log.debug("Executing SequenceNode: {}", getNodeId());
		final var selectorNodeExecutionResult = SelectorNodeExecutionResult.builder()
				.nodeId(getNodeId())
				.childrenResults(new HashMap<>())
				.build();

		for (var child : getChildren()) {
			final var childExecutionResult = child.execute(context);
			final var isSelected = selectiveNodePredicate.test(childExecutionResult);
			log.debug("Child [{}]'s Execution Result: {} - isSelected: {}", child.getNodeId(), childExecutionResult
					.getStatus(), isSelected);
			selectorNodeExecutionResult.getChildrenResults()
					.put(child.getNodeId(), childExecutionResult);
			if (isSelected) {
				return selectorNodeExecutionResult.withStatus(AgenticNodeStatus.SUCCESS);
			}
		}
		log.debug("Selector Node {} completed", getNodeId());
		return selectorNodeExecutionResult.withStatus(AgenticNodeStatus.FAILURE);
	}
}