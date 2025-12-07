package io.vinta.agentic.tree.context;

import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.execution.AgenticExecutionResultRepository;
import io.vinta.agentic.tree.identifier.ExecutionId;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

@Getter
@With
@Slf4j
public class AgenticExecutionContext {
	private final AgenticContextState<String, String> currentMetadata;
	private final AgenticExecutionResultRepository executionResultRepository;
	private final ExecutionId executionId;

	@Builder
	public AgenticExecutionContext(AgenticContextState<String, String> currentMetadata,
			AgenticExecutionResultRepository executionResultRepository, ExecutionId executionId) {
		this.executionResultRepository = executionResultRepository;
		this.currentMetadata = Optional.ofNullable(currentMetadata)
				.orElse(new AgenticContextState<>());
		this.executionId = executionId;
	}

	public void saveGlobalResult(AgenticExecutionResult result) {
		final var executionResultId = "%s#%s".formatted(this.getExecutionId()
				.id(), result.getNodeId()
						.id());
		if (executionResultRepository.isExistsById(executionResultId)) {
			log.warn("Overwriting existing execution result for executionResultId: {}", executionResultId);
		}
		executionResultRepository.save(executionResultId, result);
	}
}