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
	void testSequenceWorkflow() {
		// Test implementation goes here

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("workflow-1"))
				.sequence("seq-1", seq ->
						seq.task("task-1", context -> {
						// Task execution logic
						return new SimpleAgenticExecutionResult(NodeId.of("task-1"), AgenticNodeStatus.SUCCESS);
					})
						.task("task-2", context -> {
							// Task execution logic
							return new SimpleAgenticExecutionResult(NodeId.of("task-2"), AgenticNodeStatus.SUCCESS);
						})
						.task("task-3", context -> {
							// Task execution logic
							return new SimpleAgenticExecutionResult(NodeId.of("task-3"), AgenticNodeStatus.SUCCESS);
						})
				)
				.workflowName("Sample Workflow")
				.build();

		final var result = workflow.execute(AgenticExecutionContext.builder()
						.executionId("exec-1")
				.build());
		System.out.println(result.getStatus());
	}

	@Test
	void testSequenceWith2BranchesWorkflow() {
		// Test implementation goes here

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("workflow-1"))
				.sequence("root", seq ->
						seq.sequence(NodeId.of("seq-11"), seqBranch1 -> seqBranch1.task("task-111", context -> {
                            // Task execution logic
                            return new SimpleAgenticExecutionResult(NodeId.of("task-111"), AgenticNodeStatus.SUCCESS);
                        })
                                .task("task-112", context -> {
                                    // Task execution logic
                                    return new SimpleAgenticExecutionResult(NodeId.of("task-112"), AgenticNodeStatus.SUCCESS);
                                }))
								.sequence(NodeId.of("seq-12"), seqBranch2 -> seqBranch2.task("task-121", context -> {
									// Task execution logic
									return new SimpleAgenticExecutionResult(NodeId.of("task-121"), AgenticNodeStatus.SUCCESS);
								})
								.task("task-122", context -> {
									// Task execution logic
									return new SimpleAgenticExecutionResult(NodeId.of("task-122"), AgenticNodeStatus.SUCCESS);
								}))
				)
				.workflowName("Sample Workflow")
				.build();

		final var result = workflow.execute(AgenticExecutionContext.builder()
				.executionId("exec-1")
				.build());
		System.out.println(result.getStatus());
	}
}