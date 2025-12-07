package io.vinta.agentic.tree.context;

import io.vinta.agentic.tree.execution.AgenticExecutionMemoryRepository;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.ExecutionId;
import io.vinta.agentic.tree.identifier.NodeId;
import java.util.Optional;
import lombok.Builder;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

@With
@Slf4j
public class AgenticExecutionContext {
	private final AgenticContextState<String, String> currentMetadata;
	private final AgenticExecutionMemoryRepository executionMemoryRepository;
	private final ExecutionId executionId;

	@Builder
	public AgenticExecutionContext(AgenticContextState<String, String> currentMetadata,
			AgenticExecutionMemoryRepository executionMemoryRepository, ExecutionId executionId) {
		this.executionMemoryRepository = executionMemoryRepository;
		this.currentMetadata = Optional.ofNullable(currentMetadata)
				.orElse(new AgenticContextState<>());
		this.executionId = executionId;
	}

	public void saveExecutionResult(NodeId nodeId, AgenticExecutionResult result) {
		final var executionResultId = "%s#%s".formatted(this.executionId.id(), nodeId.id());
		if (executionMemoryRepository.isExistsById(executionResultId)) {
			log.warn("Overwriting existing execution result for executionResultId: {}", executionResultId);
		}
		executionMemoryRepository.save(executionResultId, result);
	}

	public Optional<AgenticExecutionResult> findExecutionMemoryByNodeId(NodeId nodeId) {
		final var executionResultId = "%s#%s".formatted(this.executionId.id(), nodeId.id());
		return executionMemoryRepository.findByExecutionResultId(executionResultId);
	}
}