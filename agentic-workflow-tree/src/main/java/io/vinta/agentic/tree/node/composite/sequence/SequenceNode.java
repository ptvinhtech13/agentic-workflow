package io.vinta.agentic.tree.node.composite.sequence;

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

@Slf4j
public class SequenceNode extends CompositeNode {

	private static final Predicate<AgenticExecutionResult> DEFAULT_STOPPED_SEQUENCE_PREDICATE = executionResult -> !AgenticNodeStatus.SUCCESS
			.equals(executionResult.getStatus());

	private final Predicate<AgenticExecutionResult> stoppedSequencePredicate;

	@Builder
	public SequenceNode(NodeId nodeId, Predicate<AgenticExecutionResult> stoppedSequencePredicate,
			AgenticNode... children) {
		super(nodeId, children);
		this.stoppedSequencePredicate = Optional.ofNullable(stoppedSequencePredicate)
				.orElse(DEFAULT_STOPPED_SEQUENCE_PREDICATE);
	}

	@Override
	public AgenticExecutionResult onExecute(AgenticExecutionContext context) {
		log.debug("Executing SequenceNode: {}", getNodeId());
		final var sequenceExecutionResult = SequenceNodeExecutionResult.builder()
				.nodeId(getNodeId())
				.childrenResults(new HashMap<>())
				.build();

		for (var child : getChildren()) {
			final var childExecutionResult = child.execute(context);
			final var isStoppedSequence = stoppedSequencePredicate.test(childExecutionResult);
			log.debug("Child [{}]'s Execution Result: {} - isStopped: {}", child.getNodeId(), childExecutionResult
					.getStatus(), isStoppedSequence);
			sequenceExecutionResult.getChildrenResults()
					.put(child.getNodeId(), childExecutionResult);
			if (isStoppedSequence) {
				return sequenceExecutionResult.withStatus(childExecutionResult.getStatus());
			}
		}
		log.info("SequenceNode {} completed successfully", getNodeId());
		return sequenceExecutionResult.withStatus(AgenticNodeStatus.SUCCESS);
	}
}