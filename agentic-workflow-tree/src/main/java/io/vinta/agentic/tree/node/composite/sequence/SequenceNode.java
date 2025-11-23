package io.vinta.agentic.tree.node.composite.sequence;

import io.vinta.agentic.tree.configuration.AgenticNodeConfiguration;
import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import io.vinta.agentic.tree.node.NodeId;
import io.vinta.agentic.tree.node.composite.CompositeNode;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SequenceNode extends CompositeNode {

	private static final AgenticNodeConfiguration DEFAULT_CONFIG = AgenticNodeConfiguration.builder()
			.name("Default Sequence Node Configuration")
			.build();

	public SequenceNode(NodeId nodeId) {
		this(nodeId, DEFAULT_CONFIG);
	}

	protected SequenceNode(NodeId nodeId, AgenticNodeConfiguration configuration) {
		super(nodeId, configuration);
	}

	@Override
	public AgenticExecutionResult execute(AgenticNodeConfiguration configuration, AgenticExecutionContext context) {
		log.info("Executing SequenceNode: {}", getNodeId());
		final var sequenceExecutionResult = SequenceNodeExecutionResult.builder()
				.nodeId(getNodeId())
				.childrenResults(new HashMap<>())
				.build();

		for (var child : getChildren()) {
			final var childExecutionResult = child.execute(child.getConfiguration(), context);
			final var isContinuing = child.getConfiguration()
					.isContinuingExecution(childExecutionResult);
			log.debug("Child [{}]'s Execution Result: {} - isContinuing: {}", child.getNodeId(), childExecutionResult
					.getStatus(), isContinuing);
			sequenceExecutionResult.getChildrenResults()
					.put(child.getNodeId(), childExecutionResult);
			if (!isContinuing) {
				return sequenceExecutionResult.withStatus(AgenticNodeStatus.FAILURE);
			}
		}
		log.info("SequenceNode {} completed successfully", getNodeId());
		return sequenceExecutionResult.withStatus(AgenticNodeStatus.SUCCESS);
	}

	public static SequenceNode of(NodeId nodeId, AgenticNode... nodes) {
		SequenceNode sequence = new SequenceNode(nodeId);
		sequence.addChildren(nodes);
		return sequence;
	}
}