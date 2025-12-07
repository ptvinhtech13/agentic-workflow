package io.vinta.agentic.tree.fluent;

import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.identifier.WorkflowId;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.composite.sequence.SequenceBuilder;
import io.vinta.agentic.tree.node.composite.sequence.SequenceNode;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AgenticWorkflowBuilder {
	private AgenticNode rootNode;
	private WorkflowId workflowId;
	private String workflowName;

	public static AgenticWorkflowBuilder create(WorkflowId workflowId) {
		return AgenticWorkflowBuilder.builder()
				.workflowId(workflowId)
				.build();
	}

	public AgenticWorkflowBuilder rootNode(AgenticNode rootNode) {
		this.rootNode = rootNode;
		return this;
	}

	public AgenticWorkflowBuilder workflowId(WorkflowId workflowId) {
		this.workflowId = workflowId;
		return this;
	}

	public AgenticWorkflowBuilder workflowName(String workflowName) {
		this.workflowName = workflowName;
		return this;
	}

	public AgenticWorkflowBuilder sequence(String nodeId, Consumer<SequenceBuilder> builder) {
		return sequence(NodeId.of(nodeId), builder);
	}

	public AgenticWorkflowBuilder sequence(SequenceNode sequenceNode) {
		this.rootNode = sequenceNode;
		return this;
	}

	public AgenticWorkflowBuilder sequence(NodeId nodeId, Consumer<SequenceBuilder> builder) {
		final var seqBuilder = new SequenceBuilder(nodeId);
		builder.accept(seqBuilder);
		this.rootNode = seqBuilder.build();
		return this;
	}

	public AgenticWorkflow build() {
		if (rootNode == null) {
			throw new IllegalStateException("Root node not set");
		}
		return new AgenticWorkflow(workflowId, rootNode);
	}

}