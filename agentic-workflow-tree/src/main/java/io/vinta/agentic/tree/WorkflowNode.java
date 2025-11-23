package io.vinta.agentic.tree;

public class WorkflowNode {
	private final String id;
	private final String name;

	public WorkflowNode(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "WorkflowNode{id='" + id
				+ "', name='"
				+ name
				+ "'}";
	}
}
