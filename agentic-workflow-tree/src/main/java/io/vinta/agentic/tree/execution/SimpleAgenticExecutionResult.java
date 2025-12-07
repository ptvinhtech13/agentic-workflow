package io.vinta.agentic.tree.execution;

import io.vinta.agentic.tree.node.AgenticNodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Getter
@With
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class SimpleAgenticExecutionResult<R> implements AgenticExecutionResult {
	private final AgenticNodeStatus status;
	private R result;
	private Class<R> classType;
}