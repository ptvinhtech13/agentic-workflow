package io.vinta.agentic.tree.node.composite;

import io.vinta.agentic.tree.configuration.AgenticNodeConfiguration;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.NodeId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
public abstract class CompositeNode implements AgenticNode {

	private final NodeId nodeId;
	private final AgenticNodeConfiguration configuration;
	private final List<AgenticNode> children;

	protected CompositeNode(NodeId nodeId, AgenticNodeConfiguration configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException("Configuration cannot be null");
		}
		this.nodeId = nodeId;
		this.configuration = configuration;
		this.children = new ArrayList<>();
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