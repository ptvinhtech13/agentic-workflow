package io.vinta.agentic.tree.node.composite;

import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.node.AgenticNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
public abstract class CompositeNode implements AgenticNode {

	private final NodeId nodeId;
	private final List<AgenticNode> children;

	protected CompositeNode(NodeId nodeId) {
		this.nodeId = nodeId;
		this.children = new ArrayList<>();
	}

	protected CompositeNode(NodeId nodeId, AgenticNode... children) {
		this.nodeId = nodeId;
		this.children = children != null ? new ArrayList<>(Arrays.asList(children)) : new ArrayList<>();
	}

	public void addChild(AgenticNode child) {
		children.add(child);
	}

	public void addChildren(AgenticNode... nodes) {
		children.addAll(Arrays.asList(nodes));
	}

	public void addChildren(List<AgenticNode> nodes) {
		children.addAll(nodes);
	}
}