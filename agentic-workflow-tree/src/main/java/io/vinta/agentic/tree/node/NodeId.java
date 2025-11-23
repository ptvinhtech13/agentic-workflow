package io.vinta.agentic.tree.node;

import lombok.Builder;

@Builder
public record NodeId(String id) {
	public NodeId {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("NodeId cannot be null or blank");
		}
	}

	public static NodeId of(String id) {
		return NodeId.builder()
				.id(id)
				.build();
	}
}