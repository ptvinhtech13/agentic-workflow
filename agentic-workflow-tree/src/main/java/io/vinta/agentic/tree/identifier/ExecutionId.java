package io.vinta.agentic.tree.identifier;

import lombok.Builder;

@Builder
public record ExecutionId(String id) {
	public ExecutionId {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("ExecutionId cannot be null or blank");
		}
	}

	public static ExecutionId of(String id) {
		return ExecutionId.builder()
				.id(id)
				.build();
	}
}