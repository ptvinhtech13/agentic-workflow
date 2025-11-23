package io.vinta.agentic.tree;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.SimpleAgenticExecutionResult;
import io.vinta.agentic.tree.fluent.AgenticWorkflowBuilder;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.identifier.WorkflowId;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import org.junit.jupiter.api.Test;

class AgenticWorkflowBuilderTest {

	@Test
	void testWorkflowBuilder() {
		// Test implementation goes here

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("workflow-1"))
				.sequence("seq-1", seq -> seq.task("task-1", context -> {
					// Task execution logic
					return new SimpleAgenticExecutionResult(NodeId.of("task-1"), AgenticNodeStatus.SUCCESS);
				})
						.task("task-2", context -> {
							// Task execution logic
							return new SimpleAgenticExecutionResult(NodeId.of("task-2"), AgenticNodeStatus.SUCCESS);
						}))
				.workflowName("Sample Workflow")
				.build();

		final var result = workflow.execute(new AgenticExecutionContext("exec-1"));
		System.out.println(result.getStatus());
	}
}