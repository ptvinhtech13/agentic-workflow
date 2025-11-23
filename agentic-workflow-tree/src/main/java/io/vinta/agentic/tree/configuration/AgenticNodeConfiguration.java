package io.vinta.agentic.tree.configuration;

import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import java.util.Map;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@With
@Builder
public class AgenticNodeConfiguration {
	private final String name;
	private final String description;
	private final Map<String, Object> settings;
	private final Predicate<AgenticExecutionResult> continuationExecutionPredicate;

	private static final Predicate<AgenticExecutionResult> DEFAULT_CONTINUATION_PREDICATE = executionResult -> AgenticNodeStatus.SUCCESS
			.equals(executionResult.getStatus());

	public boolean isContinuingExecution(AgenticExecutionResult executionResult) {
		return continuationExecutionPredicate.test(executionResult);

	}
}