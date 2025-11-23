package io.vinta.agentic.tree.fluent;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.AgenticExecutionResult;
import io.vinta.agentic.tree.identifier.WorkflowId;
import io.vinta.agentic.tree.node.AgenticNode;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@RequiredArgsConstructor
@Builder
@Slf4j
public class AgenticWorkflow {
	private final WorkflowId workflowId;
	private final AgenticNode rootNode;

	/**
	 * Execute the workflow tree with an existing context
	 */
	public AgenticExecutionResult execute(AgenticExecutionContext context) {
		log.info("Starting execution of workflow tree: {}", workflowId);
		long startTime = System.currentTimeMillis();

		final var result = rootNode.execute(context);

		long duration = System.currentTimeMillis() - startTime;
		log.info("Workflow {} completed with status in {}ms", workflowId.id(), duration);

		return result;
	}

}