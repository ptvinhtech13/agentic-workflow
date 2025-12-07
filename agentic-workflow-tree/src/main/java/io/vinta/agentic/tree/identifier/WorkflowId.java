package io.vinta.agentic.tree.identifier;

import lombok.Builder;

@Builder
public record WorkflowId(String id) {
	public WorkflowId {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("WorkflowId cannot be null or blank");
		}
	}

	public static WorkflowId of(String id) {
		return WorkflowId.builder()
				.id(id)
				.build();
	}
}