package io.vinta.agentic.tree.node.leaf;

import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNode;
import lombok.Getter;

@Getter
public abstract class LeafNode implements AgenticNode {

	private final NodeId nodeId;

	protected LeafNode(NodeId nodeId) {
		this.nodeId = nodeId;
	}
}