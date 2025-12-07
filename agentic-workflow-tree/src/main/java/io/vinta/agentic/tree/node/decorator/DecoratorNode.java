package io.vinta.agentic.tree.node.decorator;

import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNode;
import lombok.Getter;

@Getter
public abstract class DecoratorNode implements AgenticNode {

	private final NodeId nodeId;
	private final AgenticNode child;

	protected DecoratorNode(NodeId nodeId, AgenticNode child) {
		this.nodeId = nodeId;
		this.child = child;
	}
}