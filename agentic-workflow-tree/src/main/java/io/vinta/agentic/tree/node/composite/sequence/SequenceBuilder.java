package io.vinta.agentic.tree.node.composite.sequence;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.decorator.conditional.ConditionalNode;
import io.vinta.agentic.tree.node.decorator.conditional.ConditionalOrElseNode;
import io.vinta.agentic.tree.node.leaf.task.TaskNode;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;

@Getter
public class SequenceBuilder {
	private final SequenceNode sequence;
	private final NodeId nodeId;

	public SequenceBuilder(NodeId nodeId) {
		this.nodeId = nodeId;
		this.sequence = SequenceNode.builder()
				.nodeId(nodeId)
				.build();
	}

	public SequenceBuilder task(String nodeId, Function<AgenticExecutionContext, AgenticExecutionResult> taskFunc) {
		return task(NodeId.of(nodeId), taskFunc);
	}

	public SequenceBuilder task(NodeId nodeId, Function<AgenticExecutionContext, AgenticExecutionResult> taskFunc) {
		sequence.addChild(TaskNode.builder()
				.nodeId(nodeId)
				.taskFunction(taskFunc)
				.build());
		return this;
	}

	public SequenceBuilder conditional(NodeId nodeId, Predicate<AgenticExecutionContext> condition, AgenticNode child) {
		sequence.addChild(ConditionalNode.builder()
				.nodeId(nodeId)
				.conditionPredicate(condition)
				.child(child)
				.build());
		return this;
	}

	public SequenceBuilder conditionalOrElse(NodeId nodeId, Predicate<AgenticExecutionContext> condition,
			AgenticNode child, AgenticNode orElseChild) {
		sequence.addChild(ConditionalOrElseNode.builder()
				.nodeId(nodeId)
				.conditionPredicate(condition)
				.child(child)
				.orElseChild(orElseChild)
				.build());
		return this;
	}

	public SequenceBuilder sequence(NodeId nodeId, Consumer<SequenceBuilder> builder) {
		SequenceBuilder nested = new SequenceBuilder(nodeId);
		builder.accept(nested);
		sequence.addChild(nested.build());
		return this;
	}

	public SequenceBuilder node(AgenticNode node) {
		sequence.addChild(node);
		return this;
	}

	public SequenceNode build() {
		return sequence;
	}
}