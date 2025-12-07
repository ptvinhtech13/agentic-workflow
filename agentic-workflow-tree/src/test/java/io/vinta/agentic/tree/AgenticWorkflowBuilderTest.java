package io.vinta.agentic.tree;

import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.InMemoryAgenticExecutionMemoryRepository;
import io.vinta.agentic.tree.execution.SimpleAgenticExecutionResult;
import io.vinta.agentic.tree.fluent.AgenticWorkflowBuilder;
import io.vinta.agentic.tree.identifier.ExecutionId;
import io.vinta.agentic.tree.identifier.NodeId;
import io.vinta.agentic.tree.identifier.WorkflowId;
import io.vinta.agentic.tree.node.AgenticNode;
import io.vinta.agentic.tree.node.AgenticNodeStatus;
import io.vinta.agentic.tree.node.composite.selector.SelectorNode;
import io.vinta.agentic.tree.node.composite.selector.SelectorNodeExecutionResult;
import io.vinta.agentic.tree.node.composite.sequence.SequenceNodeExecutionResult;
import io.vinta.agentic.tree.node.decorator.conditional.ConditionalNode;
import io.vinta.agentic.tree.node.decorator.conditional.ConditionalOrElseNode;
import io.vinta.agentic.tree.node.leaf.task.TaskNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class AgenticWorkflowBuilderTest {

	// ========================================
	// SIMPLE TESTS - Basic single-level behavior
	// ========================================

	/**
	 * Test: Basic task node execution and result storage
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * └─ [T] task-1 (SUCCESS)
	 * └─ Result: "Task completed" (String)
	 * </pre>
	 *
	 * Flow:
	 * 1. Execute single task
	 * 2. Task returns SUCCESS with result "Task completed"
	 * 3. Verify task result is stored in execution memory
	 */
	@Test
	void testSimpleTaskExecution() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("simple-task-workflow"))
				.sequence("root", seq -> seq.task("task-1", context -> {
					log.info("Executing simple task");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "Task completed",
							String.class);
				}))
				.workflowName("Simple Task Workflow")
				.build();

		final var result = workflow.execute(AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-1"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build());

		// Assert workflow result
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());
		Assertions.assertInstanceOf(SequenceNodeExecutionResult.class, result);

		SequenceNodeExecutionResult sequenceResult = (SequenceNodeExecutionResult) result;
		Assertions.assertEquals(1, sequenceResult.getChildrenResults()
				.size());

		// Assert task result
		final var taskResult = sequenceResult.getChildrenResults()
				.get(NodeId.of("task-1"));
		Assertions.assertNotNull(taskResult);
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, taskResult.getStatus());
	}

	/**
	 * Test: Sequence executing all tasks when all succeed
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] task-1 (SUCCESS)
	 * ├─ [T] task-2 (SUCCESS)
	 * │ └─ Result: "Result from task 2" (String)
	 * └─ [T] task-3 (SUCCESS)
	 * </pre>
	 *
	 * Flow:
	 * 1. Task-1 executes → SUCCESS
	 * 2. Task-2 executes → SUCCESS with result
	 * 3. Task-3 executes → SUCCESS
	 * 4. Sequence completes successfully
	 */
	@Test
	void testSimpleSequence_AllSuccess() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("sequence-all-success"))
				.sequence("root", seq -> seq.task("task-1", context -> {
					log.info("Task 1 executing");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
				})
						.task("task-2", context -> {
							log.info("Task 2 executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "Result from task 2",
									String.class);
						})
						.task("task-3", context -> {
							log.info("Task 3 executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						}))
				.workflowName("Sequence All Success Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-2"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow completed successfully
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());
		Assertions.assertInstanceOf(SequenceNodeExecutionResult.class, result);

		SequenceNodeExecutionResult sequenceResult = (SequenceNodeExecutionResult) result;
		Assertions.assertEquals(3, sequenceResult.getChildrenResults()
				.size());

		// Assert each task executed successfully
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, sequenceResult.getChildrenResults()
				.get(NodeId.of("task-1"))
				.getStatus());
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, sequenceResult.getChildrenResults()
				.get(NodeId.of("task-2"))
				.getStatus());
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, sequenceResult.getChildrenResults()
				.get(NodeId.of("task-3"))
				.getStatus());

		// Verify task-2 result is stored in execution memory
		final var task2Memory = context.findExecutionMemoryByNodeId(NodeId.of("task-2"));
		Assertions.assertTrue(task2Memory.isPresent());
		SimpleAgenticExecutionResult<?> task2Result = (SimpleAgenticExecutionResult<?>) task2Memory.get();
		Assertions.assertEquals(String.class, task2Result.getClassType());
		String task2Value = (String) task2Result.getResult();
		Assertions.assertEquals("Result from task 2", task2Value);
	}

	/**
	 * Test: Sequence stopping on first failure (short-circuit behavior)
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (FAILURE)
	 * ├─ [T] task-1 (SUCCESS) ✓
	 * ├─ [T] task-2 (FAILURE) ✗ ← Stops here
	 * └─ [T] task-3 (not executed)
	 * </pre>
	 *
	 * Flow:
	 * 1. Task-1 executes → SUCCESS
	 * 2. Task-2 executes → FAILURE
	 * 3. Sequence stops immediately (short-circuit)
	 * 4. Task-3 never executes
	 */
	@Test
	void testSimpleSequence_StopOnFailure() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("sequence-stop-on-failure"))
				.sequence("root", seq -> seq.task("task-1", context -> {
					log.info("Task 1 executing - SUCCESS");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
				})
						.task("task-2", context -> {
							log.info("Task 2 executing - FAILURE");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
						})
						.task("task-3", context -> {
							log.info("Task 3 should NOT execute");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						}))
				.workflowName("Sequence Stop On Failure Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-3"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow failed
		Assertions.assertEquals(AgenticNodeStatus.FAILURE, result.getStatus());
		Assertions.assertInstanceOf(SequenceNodeExecutionResult.class, result);

		SequenceNodeExecutionResult sequenceResult = (SequenceNodeExecutionResult) result;
		// Only 2 tasks should have executed (stopped at failure)
		Assertions.assertEquals(2, sequenceResult.getChildrenResults()
				.size());

		// Assert task-1 succeeded, task-2 failed, task-3 never executed
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, sequenceResult.getChildrenResults()
				.get(NodeId.of("task-1"))
				.getStatus());
		Assertions.assertEquals(AgenticNodeStatus.FAILURE, sequenceResult.getChildrenResults()
				.get(NodeId.of("task-2"))
				.getStatus());
		Assertions.assertNull(sequenceResult.getChildrenResults()
				.get(NodeId.of("task-3")));

		// Verify task-3 was never saved to execution memory
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("task-3"))
				.isPresent());
	}

	/**
	 * Test: Selector stopping at first successful child (OR/fallback logic)
	 *
	 * Tree Structure:
	 * <pre>
	 * [SEL] selector-1 (SUCCESS)
	 * ├─ [T] task-1 (FAILURE) ✗
	 * ├─ [T] task-2 (SUCCESS) ✓ ← Stops here
	 * │ └─ Result: "Selected task 2" (String)
	 * └─ [T] task-3 (not executed)
	 * </pre>
	 *
	 * Flow:
	 * 1. Task-1 executes → FAILURE
	 * 2. Task-2 executes → SUCCESS (first success!)
	 * 3. Selector stops immediately (short-circuit)
	 * 4. Task-3 never executes
	 */
	@Test
	void testSimpleSelector_FirstSuccess() {
		final var selectorNode = SelectorNode.builder()
				.nodeId(NodeId.of("selector-1"))
				.children(new AgenticNode[] { TaskNode.builder()
						.nodeId(NodeId.of("task-1"))
						.taskFunction(context -> {
							log.info("Task 1 executing - FAILURE");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
						})
						.build(), TaskNode.builder()
								.nodeId(NodeId.of("task-2"))
								.taskFunction(context -> {
									log.info("Task 2 executing - SUCCESS");
									return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
											"Selected task 2", String.class);
								})
								.build(), TaskNode.builder()
										.nodeId(NodeId.of("task-3"))
										.taskFunction(context -> {
											log.info("Task 3 should NOT execute");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
										})
										.build() })
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("selector-first-success"))
				.rootNode(selectorNode)
				.workflowName("Selector First Success Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-4"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert selector succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());
		Assertions.assertInstanceOf(SelectorNodeExecutionResult.class, result);

		SelectorNodeExecutionResult selectorResult = (SelectorNodeExecutionResult) result;
		// Only 2 tasks should have executed (stopped at first success)
		Assertions.assertEquals(2, selectorResult.getChildrenResults()
				.size());

		// Assert task-1 failed, task-2 succeeded, task-3 never executed
		Assertions.assertEquals(AgenticNodeStatus.FAILURE, selectorResult.getChildrenResults()
				.get(NodeId.of("task-1"))
				.getStatus());
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, selectorResult.getChildrenResults()
				.get(NodeId.of("task-2"))
				.getStatus());
		Assertions.assertNull(selectorResult.getChildrenResults()
				.get(NodeId.of("task-3")));
	}

	/**
	 * Test: Selector returning FAILURE when all children fail
	 *
	 * Tree Structure:
	 * <pre>
	 * [SEL] selector-1 (FAILURE)
	 * ├─ [T] task-1 (FAILURE) ✗
	 * ├─ [T] task-2 (FAILURE) ✗
	 * └─ [T] task-3 (FAILURE) ✗
	 * └─ All failed, selector returns FAILURE
	 * </pre>
	 *
	 * Flow:
	 * 1. Task-1 executes → FAILURE
	 * 2. Task-2 executes → FAILURE
	 * 3. Task-3 executes → FAILURE
	 * 4. All children failed → Selector returns FAILURE
	 */
	@Test
	void testSimpleSelector_AllFail() {
		final var selectorNode = SelectorNode.builder()
				.nodeId(NodeId.of("selector-1"))
				.children(new AgenticNode[] { TaskNode.builder()
						.nodeId(NodeId.of("task-1"))
						.taskFunction(context -> {
							log.info("Task 1 executing - FAILURE");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
						})
						.build(), TaskNode.builder()
								.nodeId(NodeId.of("task-2"))
								.taskFunction(context -> {
									log.info("Task 2 executing - FAILURE");
									return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
								})
								.build(), TaskNode.builder()
										.nodeId(NodeId.of("task-3"))
										.taskFunction(context -> {
											log.info("Task 3 executing - FAILURE");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
										})
										.build() })
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("selector-all-fail"))
				.rootNode(selectorNode)
				.workflowName("Selector All Fail Workflow")
				.build();

		final var result = workflow.execute(AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-5"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build());

		// Assert selector failed (all children failed)
		Assertions.assertEquals(AgenticNodeStatus.FAILURE, result.getStatus());
		Assertions.assertInstanceOf(SelectorNodeExecutionResult.class, result);

		SelectorNodeExecutionResult selectorResult = (SelectorNodeExecutionResult) result;
		// All 3 tasks should have been tried
		Assertions.assertEquals(3, selectorResult.getChildrenResults()
				.size());

		// Assert all tasks failed
		Assertions.assertEquals(AgenticNodeStatus.FAILURE, selectorResult.getChildrenResults()
				.get(NodeId.of("task-1"))
				.getStatus());
		Assertions.assertEquals(AgenticNodeStatus.FAILURE, selectorResult.getChildrenResults()
				.get(NodeId.of("task-2"))
				.getStatus());
		Assertions.assertEquals(AgenticNodeStatus.FAILURE, selectorResult.getChildrenResults()
				.get(NodeId.of("task-3"))
				.getStatus());
	}

	/**
	 * Test: Conditional node executing child when condition is true
	 *
	 * Tree Structure:
	 * <pre>
	 * [COND] conditional-1 (condition = true) (SUCCESS)
	 * └─ [T] task-1 (executed) (SUCCESS)
	 * └─ Result: "Conditional task executed" (String)
	 * </pre>
	 *
	 * Flow:
	 * 1. Evaluate condition → TRUE
	 * 2. Execute child task-1 → SUCCESS
	 * 3. Return child's status (SUCCESS)
	 */
	@Test
	void testConditionalNode_ConditionTrue() {
		final var conditionalNode = ConditionalNode.builder()
				.nodeId(NodeId.of("conditional-1"))
				.conditionPredicate(context -> true) // Condition is always true
				.child(TaskNode.builder()
						.nodeId(NodeId.of("task-1"))
						.taskFunction(context -> {
							log.info("Task 1 executing - condition was true");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
									"Conditional task executed", String.class);
						})
						.build())
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("conditional-true"))
				.rootNode(conditionalNode)
				.workflowName("Conditional True Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-6"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert conditional executed child (condition was true)
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());
		Assertions.assertInstanceOf(SimpleAgenticExecutionResult.class, result);

		// Verify task was saved to execution memory
		final var taskMemory = context.findExecutionMemoryByNodeId(NodeId.of("task-1"));
		Assertions.assertTrue(taskMemory.isPresent());
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, taskMemory.get()
				.getStatus());
	}

	/**
	 * Test: Conditional node skipping child when condition is false
	 *
	 * Tree Structure:
	 * <pre>
	 * [COND] conditional-1 (condition = false) (SKIPPED)
	 * └─ [T] task-1 (not executed)
	 * </pre>
	 *
	 * Flow:
	 * 1. Evaluate condition → FALSE
	 * 2. Skip child execution
	 * 3. Return SKIPPED status
	 */
	@Test
	void testConditionalNode_ConditionFalse() {
		final var conditionalNode = ConditionalNode.builder()
				.nodeId(NodeId.of("conditional-1"))
				.conditionPredicate(context -> false) // Condition is always false
				.child(TaskNode.builder()
						.nodeId(NodeId.of("task-1"))
						.taskFunction(context -> {
							log.info("Task 1 should NOT execute");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						})
						.build())
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("conditional-false"))
				.rootNode(conditionalNode)
				.workflowName("Conditional False Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-7"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert conditional skipped child (condition was false)
		Assertions.assertEquals(AgenticNodeStatus.SKIPPED, result.getStatus());
		Assertions.assertInstanceOf(SimpleAgenticExecutionResult.class, result);

		// Verify task was never saved to execution memory
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("task-1"))
				.isPresent());
	}

	/**
	 * Test: If-else node taking the true branch
	 *
	 * Tree Structure:
	 * <pre>
	 * [COND-OR] conditional-or-else-1 (condition = true) (SUCCESS)
	 * ├─ [T] true-task (executed) (SUCCESS)
	 * │ └─ Result: "True branch" (String)
	 * └─ [T] false-task (not executed)
	 * </pre>
	 *
	 * Flow:
	 * 1. Evaluate condition → TRUE
	 * 2. Execute true-task → SUCCESS
	 * 3. Skip false-task
	 * 4. Return true-task's status
	 */
	@Test
	void testConditionalOrElseNode_TrueBranch() {
		final var conditionalOrElseNode = ConditionalOrElseNode.builder()
				.nodeId(NodeId.of("conditional-or-else-1"))
				.conditionPredicate(context -> true)
				.child(TaskNode.builder()
						.nodeId(NodeId.of("true-task"))
						.taskFunction(context -> {
							log.info("True branch executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "True branch",
									String.class);
						})
						.build())
				.orElseChild(TaskNode.builder()
						.nodeId(NodeId.of("false-task"))
						.taskFunction(context -> {
							log.info("False branch should NOT execute");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "False branch",
									String.class);
						})
						.build())
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("conditional-or-else-true"))
				.rootNode(conditionalOrElseNode)
				.workflowName("Conditional OrElse True Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-8"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert true branch was executed
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify only true-task was saved to execution memory
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("true-task"))
				.isPresent());
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("false-task"))
				.isPresent());

		SimpleAgenticExecutionResult<?> trueTaskResult = (SimpleAgenticExecutionResult<?>) context
				.findExecutionMemoryByNodeId(NodeId.of("true-task"))
				.get();
		Assertions.assertEquals("True branch", trueTaskResult.getResult());
	}

	/**
	 * Test: If-else node taking the false branch
	 *
	 * Tree Structure:
	 * <pre>
	 * [COND-OR] conditional-or-else-1 (condition = false) (SUCCESS)
	 * ├─ [T] true-task (not executed)
	 * └─ [T] false-task (executed) (SUCCESS)
	 * └─ Result: "False branch" (String)
	 * </pre>
	 *
	 * Flow:
	 * 1. Evaluate condition → FALSE
	 * 2. Skip true-task
	 * 3. Execute false-task → SUCCESS
	 * 4. Return false-task's status
	 */
	@Test
	void testConditionalOrElseNode_FalseBranch() {
		final var conditionalOrElseNode = ConditionalOrElseNode.builder()
				.nodeId(NodeId.of("conditional-or-else-1"))
				.conditionPredicate(context -> false)
				.child(TaskNode.builder()
						.nodeId(NodeId.of("true-task"))
						.taskFunction(context -> {
							log.info("True branch should NOT execute");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "True branch",
									String.class);
						})
						.build())
				.orElseChild(TaskNode.builder()
						.nodeId(NodeId.of("false-task"))
						.taskFunction(context -> {
							log.info("False branch executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "False branch",
									String.class);
						})
						.build())
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("conditional-or-else-false"))
				.rootNode(conditionalOrElseNode)
				.workflowName("Conditional OrElse False Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-9"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert false branch was executed
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify only false-task was saved to execution memory
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("true-task"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("false-task"))
				.isPresent());

		SimpleAgenticExecutionResult<?> falseTaskResult = (SimpleAgenticExecutionResult<?>) context
				.findExecutionMemoryByNodeId(NodeId.of("false-task"))
				.get();
		Assertions.assertEquals("False branch", falseTaskResult.getResult());
	}

	// ========================================
	// INTERMEDIATE TESTS - Combined behaviors
	// ========================================

	/**
	 * Test: Sequences containing other sequences (hierarchical structure)
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] task-1 (SUCCESS)
	 * ├─ [S] nested-seq-1 (SUCCESS)
	 * │ ├─ [T] task-2 (SUCCESS)
	 * │ │ └─ Result: 100 (Integer)
	 * │ └─ [T] task-3 (SUCCESS)
	 * └─ [T] task-4 (SUCCESS)
	 * </pre>
	 *
	 * Flow:
	 * 1. Task-1 executes → SUCCESS
	 * 2. Nested-seq-1 begins:
	 * - Task-2 executes → SUCCESS (stores 100)
	 * - Task-3 executes → SUCCESS
	 * - Nested-seq-1 → SUCCESS
	 * 3. Task-4 executes → SUCCESS
	 * 4. Root sequence → SUCCESS
	 */
	@Test
	void testNestedSequences() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("nested-sequences"))
				.sequence("root", seq -> seq.task("task-1", context -> {
					log.info("Root task-1 executing");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
				})
						.sequence(NodeId.of("nested-seq-1"), nestedSeq -> nestedSeq.task("task-2", context -> {
							log.info("Nested task-2 executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, 100, Integer.class);
						})
								.task("task-3", context -> {
									log.info("Nested task-3 executing");
									return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
								}))
						.task("task-4", context -> {
							log.info("Root task-4 executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						}))
				.workflowName("Nested Sequences Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-10"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());
		Assertions.assertInstanceOf(SequenceNodeExecutionResult.class, result);

		SequenceNodeExecutionResult rootResult = (SequenceNodeExecutionResult) result;
		Assertions.assertEquals(3, rootResult.getChildrenResults()
				.size()); // task-1, nested-seq-1, task-4

		// Assert nested sequence result
		final var nestedSeqResult = rootResult.getChildrenResults()
				.get(NodeId.of("nested-seq-1"));
		Assertions.assertNotNull(nestedSeqResult);
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, nestedSeqResult.getStatus());
		Assertions.assertInstanceOf(SequenceNodeExecutionResult.class, nestedSeqResult);

		SequenceNodeExecutionResult nestedSequence = (SequenceNodeExecutionResult) nestedSeqResult;
		Assertions.assertEquals(2, nestedSequence.getChildrenResults()
				.size()); // task-2, task-3

		// Verify task-2 result in execution memory
		final var task2Memory = context.findExecutionMemoryByNodeId(NodeId.of("task-2"));
		Assertions.assertTrue(task2Memory.isPresent());
		SimpleAgenticExecutionResult<?> task2Result = (SimpleAgenticExecutionResult<?>) task2Memory.get();
		Assertions.assertEquals(Integer.class, task2Result.getClassType());
		Assertions.assertEquals(100, task2Result.getResult());
	}

	/**
	 * Test: Conditional execution within a sequence based on previous task result
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] task-1 (SUCCESS)
	 * │ └─ Result: "enable-task-3" (String)
	 * ├─ [COND] conditional-1 (condition checks task-1 result)
	 * │ └─ [T] task-2 (executed because condition = true) (SUCCESS)
	 * └─ [T] task-3 (SUCCESS)
	 * </pre>
	 *
	 * Flow:
	 * 1. Task-1 executes → SUCCESS with "enable-task-3"
	 * 2. Conditional-1 evaluates:
	 * - Read task-1 result from memory
	 * - Check if result == "enable-task-3" → TRUE
	 * - Execute task-2 → SUCCESS
	 * 3. Task-3 executes → SUCCESS
	 */
	@Test
	void testSequenceWithConditional() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("sequence-with-conditional"))
				.sequence("root", seq -> seq.task("task-1", context -> {
					log.info("Task 1 executing");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "enable-task-3", String.class);
				})
						.node(ConditionalNode.builder()
								.nodeId(NodeId.of("conditional-1"))
								.conditionPredicate(context -> {
									// Check if task-1 result is "enable-task-3"
									final var task1Result = context.findExecutionMemoryByNodeId(NodeId.of("task-1"));
									if (task1Result.isPresent() && task1Result
											.get() instanceof SimpleAgenticExecutionResult<?>) {
										SimpleAgenticExecutionResult<?> result = (SimpleAgenticExecutionResult<?>) task1Result
												.get();
										return "enable-task-3".equals(result.getResult());
									}
									return false;
								})
								.child(TaskNode.builder()
										.nodeId(NodeId.of("task-2"))
										.taskFunction(context -> {
											log.info("Task 2 executing (conditional was true)");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
										})
										.build())
								.build())
						.task("task-3", context -> {
							log.info("Task 3 executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						}))
				.workflowName("Sequence With Conditional Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-11"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify all tasks were executed
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("task-1"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("task-2"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("task-3"))
				.isPresent());
	}

	/**
	 * Test: Selector fallback pattern with multiple failures before success
	 *
	 * Tree Structure:
	 * <pre>
	 * [SEL] selector-1 (SUCCESS)
	 * ├─ [T] strategy-1 (FAILURE) ✗
	 * ├─ [T] strategy-2 (FAILURE) ✗
	 * └─ [T] strategy-3 (SUCCESS) ✓ ← Fallback succeeded
	 * └─ Result: "Fallback strategy" (String)
	 * </pre>
	 *
	 * Flow:
	 * 1. Strategy-1 executes → FAILURE
	 * 2. Strategy-2 executes → FAILURE
	 * 3. Strategy-3 executes → SUCCESS (fallback works!)
	 * 4. Selector stops and returns SUCCESS
	 */
	@Test
	void testSelectorWithMixedResults() {
		final var selectorNode = SelectorNode.builder()
				.nodeId(NodeId.of("selector-1"))
				.children(new AgenticNode[] { TaskNode.builder()
						.nodeId(NodeId.of("strategy-1"))
						.taskFunction(context -> {
							log.info("Strategy 1 - FAILURE");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
						})
						.build(), TaskNode.builder()
								.nodeId(NodeId.of("strategy-2"))
								.taskFunction(context -> {
									log.info("Strategy 2 - FAILURE");
									return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
								})
								.build(), TaskNode.builder()
										.nodeId(NodeId.of("strategy-3"))
										.taskFunction(context -> {
											log.info("Strategy 3 - SUCCESS (fallback succeeded)");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
													"Fallback strategy", String.class);
										})
										.build() })
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("selector-mixed-results"))
				.rootNode(selectorNode)
				.workflowName("Selector Mixed Results Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-12"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert selector succeeded with fallback
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());
		Assertions.assertInstanceOf(SelectorNodeExecutionResult.class, result);

		SelectorNodeExecutionResult selectorResult = (SelectorNodeExecutionResult) result;
		Assertions.assertEquals(3, selectorResult.getChildrenResults()
				.size());

		// Verify strategy-3 succeeded
		final var strategy3Memory = context.findExecutionMemoryByNodeId(NodeId.of("strategy-3"));
		Assertions.assertTrue(strategy3Memory.isPresent());
		SimpleAgenticExecutionResult<?> strategy3Result = (SimpleAgenticExecutionResult<?>) strategy3Memory.get();
		Assertions.assertEquals("Fallback strategy", strategy3Result.getResult());
	}

	/**
	 * Test: Conditional predicates reading data from execution memory
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] data-task (SUCCESS)
	 * │ └─ Result: 42 (Integer)
	 * └─ [COND] conditional-check (reads data-task, checks if > 40)
	 * └─ [T] conditional-task (executed because 42 > 40) (SUCCESS)
	 * └─ Result: "Threshold exceeded" (String)
	 * </pre>
	 *
	 * Flow:
	 * 1. Data-task executes → SUCCESS with value 42
	 * 2. Conditional-check evaluates:
	 * - Read data-task result from memory → 42
	 * - Check if 42 > 40 → TRUE
	 * - Execute conditional-task → SUCCESS
	 * 3. Sequence completes successfully
	 */
	@Test
	void testConditionalAccessingExecutionMemory() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("conditional-with-memory"))
				.sequence("root", seq -> seq.task("data-task", context -> {
					log.info("Data task - storing value 42");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, 42, Integer.class);
				})
						.node(ConditionalNode.builder()
								.nodeId(NodeId.of("conditional-check"))
								.conditionPredicate(context -> {
									// Check if data-task result > 40
									final var dataResult = context.findExecutionMemoryByNodeId(NodeId.of("data-task"));
									if (dataResult.isPresent() && dataResult
											.get() instanceof SimpleAgenticExecutionResult<?>) {
										SimpleAgenticExecutionResult<?> result = (SimpleAgenticExecutionResult<?>) dataResult
												.get();
										if (result.getResult() instanceof Integer) {
											return (Integer) result.getResult() > 40;
										}
									}
									return false;
								})
								.child(TaskNode.builder()
										.nodeId(NodeId.of("conditional-task"))
										.taskFunction(context -> {
											log.info("Conditional task executing (value was > 40)");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
													"Threshold exceeded", String.class);
										})
										.build())
								.build()))
				.workflowName("Conditional Accessing Memory Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-13"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify conditional task was executed (because 42 > 40)
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("conditional-task"))
				.isPresent());
	}

	/**
	 * Test: If-else branching within a sequence, based on previous task result
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] check-task (SUCCESS)
	 * │ └─ Result: false (Boolean)
	 * ├─ [COND-OR] conditional-or-else (checks check-task result)
	 * │ ├─ [T] true-branch (not executed)
	 * │ └─ [T] false-branch (executed) (SUCCESS)
	 * │ └─ Result: "Else branch" (String)
	 * └─ [T] final-task (SUCCESS)
	 * </pre>
	 *
	 * Flow:
	 * 1. Check-task executes → SUCCESS with false
	 * 2. Conditional-or-else evaluates:
	 * - Read check-task result → false
	 * - Condition = false
	 * - Skip true-branch
	 * - Execute false-branch → SUCCESS
	 * 3. Final-task executes → SUCCESS
	 */
	@Test
	void testSequenceWithConditionalOrElse() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("sequence-with-conditional-or-else"))
				.sequence("root", seq -> seq.task("check-task", context -> {
					log.info("Check task - returning false flag");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, false, Boolean.class);
				})
						.node(ConditionalOrElseNode.builder()
								.nodeId(NodeId.of("conditional-or-else"))
								.conditionPredicate(context -> {
									final var checkResult = context.findExecutionMemoryByNodeId(NodeId.of(
											"check-task"));
									if (checkResult.isPresent() && checkResult
											.get() instanceof SimpleAgenticExecutionResult<?>) {
										SimpleAgenticExecutionResult<?> result = (SimpleAgenticExecutionResult<?>) checkResult
												.get();
										if (result.getResult() instanceof Boolean) {
											return (Boolean) result.getResult();
										}
									}
									return false;
								})
								.child(TaskNode.builder()
										.nodeId(NodeId.of("true-branch"))
										.taskFunction(context -> {
											log.info("True branch (should NOT execute)");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
										})
										.build())
								.orElseChild(TaskNode.builder()
										.nodeId(NodeId.of("false-branch"))
										.taskFunction(context -> {
											log.info("False branch executing");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
													"Else branch", String.class);
										})
										.build())
								.build())
						.task("final-task", context -> {
							log.info("Final task executing");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						}))
				.workflowName("Sequence With ConditionalOrElse Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-14"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify only false-branch was executed
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("true-branch"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("false-branch"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("final-task"))
				.isPresent());
	}

	// ========================================
	// COMPLEX TESTS - Full behavior tree patterns
	// ========================================

	/**
	 * Test: Service fallback pattern (primary fails, backup succeeds)
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] init-task (SUCCESS)
	 * ├─ [SEL] fallback-selector (SUCCESS)
	 * │ ├─ [T] primary-service (FAILURE) ✗
	 * │ └─ [T] backup-service (SUCCESS) ✓
	 * │ └─ Result: "Backup data" (String)
	 * └─ [T] process-task (SUCCESS)
	 * └─ Processes data from backup-service
	 * </pre>
	 *
	 * Flow:
	 * 1. Init-task executes → SUCCESS
	 * 2. Fallback-selector begins:
	 * - Primary-service executes → FAILURE
	 * - Backup-service executes → SUCCESS (fallback!)
	 * - Selector → SUCCESS
	 * 3. Process-task executes → SUCCESS
	 * - Reads backup-service result from memory
	 * - Processes the backup data
	 *
	 * Use Case: Database connection fallback, API endpoint redundancy, service resilience
	 */
	@Test
	void testSequenceWithSelector_FallbackPattern() {
		final var selectorNode = SelectorNode.builder()
				.nodeId(NodeId.of("fallback-selector"))
				.children(new AgenticNode[] { TaskNode.builder()
						.nodeId(NodeId.of("primary-service"))
						.taskFunction(context -> {
							log.info("Primary service - simulating failure");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
						})
						.build(), TaskNode.builder()
								.nodeId(NodeId.of("backup-service"))
								.taskFunction(context -> {
									log.info("Backup service - SUCCESS");
									return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "Backup data",
											String.class);
								})
								.build() })
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("sequence-with-selector"))
				.sequence("root", seq -> seq.task("init-task", context -> {
					log.info("Initialization task");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
				})
						.node(selectorNode)
						.task("process-task", context -> {
							// Process result from fallback selector
							final var backupResult = context.findExecutionMemoryByNodeId(NodeId.of("backup-service"));
							Assertions.assertTrue(backupResult.isPresent());
							log.info("Processing data from backup service");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						}))
				.workflowName("Sequence With Selector Fallback Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-15"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded with fallback
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify backup-service was used (primary failed)
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("primary-service"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("backup-service"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("process-task"))
				.isPresent());
	}

	/**
	 * Test: Selector choosing between different strategy implementations
	 *
	 * Tree Structure:
	 * <pre>
	 * [SEL] strategy-selector (SUCCESS)
	 * ├─ [T] strategy-1-single-step (FAILURE) ✗
	 * └─ [T] strategy-2-single-step (SUCCESS) ✓
	 * └─ Result: "Strategy 2 result" (String)
	 * </pre>
	 *
	 * Flow:
	 * 1. Strategy-1 executes → FAILURE
	 * 2. Strategy-2 executes → SUCCESS
	 * 3. Selector returns SUCCESS
	 *
	 * Use Case: Algorithm selection, optimization strategies, different processing approaches
	 */
	@Test
	void testSelectorWithSequences_TryMultipleStrategies() {
		final var strategy1Sequence = TaskNode.builder()
				.nodeId(NodeId.of("strategy-1-single-step"))
				.taskFunction(context -> {
					log.info("Strategy 1 - single step FAILURE");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
				})
				.build();

		// Strategy 2: Multi-step sequence (this will succeed)
		final var strategy2Sequence = TaskNode.builder()
				.nodeId(NodeId.of("strategy-2-single-step"))
				.taskFunction(context -> {
					log.info("Strategy 2 - single step SUCCESS");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "Strategy 2 result",
							String.class);
				})
				.build();

		final var selectorNode = SelectorNode.builder()
				.nodeId(NodeId.of("strategy-selector"))
				.children(new AgenticNode[] { strategy1Sequence, strategy2Sequence })
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("selector-with-sequences"))
				.rootNode(selectorNode)
				.workflowName("Selector With Sequences Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-16"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert selector succeeded with strategy 2
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());
		Assertions.assertInstanceOf(SelectorNodeExecutionResult.class, result);

		// Verify strategy 2 was successful
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("strategy-2-single-step"))
				.isPresent());
	}

	/**
	 * Test: Multi-level conditional logic (nested if-then-else)
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] level-task (SUCCESS)
	 * │ └─ Result: 3 (Integer)
	 * └─ [COND] level-check (checks if level >= 2)
	 * └─ [COND-OR] nested-conditional (checks if level >= 3)
	 * ├─ [T] high-level-task (executed, level=3) (SUCCESS)
	 * │ └─ Result: "High level" (String)
	 * └─ [T] medium-level-task (not executed)
	 * </pre>
	 *
	 * Flow:
	 * 1. Level-task executes → SUCCESS with value 3
	 * 2. Level-check evaluates:
	 * - Read level-task → 3
	 * - Check if 3 >= 2 → TRUE
	 * - Execute nested-conditional
	 * 3. Nested-conditional evaluates:
	 * - Read level-task → 3
	 * - Check if 3 >= 3 → TRUE
	 * - Execute high-level-task → SUCCESS
	 * - Skip medium-level-task
	 *
	 * Use Case: Multi-tier priority handling, graduated permissions, tiered processing
	 */
	@Test
	void testNestedConditionalsInSequence() {
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("nested-conditionals"))
				.sequence("root", seq -> seq.task("level-task", context -> {
					log.info("Level task - returning level 3");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, 3, Integer.class);
				})
						.node(ConditionalNode.builder()
								.nodeId(NodeId.of("level-check"))
								.conditionPredicate(context -> {
									final var levelResult = context.findExecutionMemoryByNodeId(NodeId.of(
											"level-task"));
									if (levelResult.isPresent() && levelResult
											.get() instanceof SimpleAgenticExecutionResult<?>) {
										SimpleAgenticExecutionResult<?> result = (SimpleAgenticExecutionResult<?>) levelResult
												.get();
										return result.getResult() instanceof Integer && (Integer) result.getResult()
												>= 2;
									}
									return false;
								})
								.child(ConditionalOrElseNode.builder()
										.nodeId(NodeId.of("nested-conditional"))
										.conditionPredicate(context -> {
											final var levelResult = context.findExecutionMemoryByNodeId(NodeId.of(
													"level-task"));
											if (levelResult.isPresent() && levelResult
													.get() instanceof SimpleAgenticExecutionResult<?>) {
												SimpleAgenticExecutionResult<?> result = (SimpleAgenticExecutionResult<?>) levelResult
														.get();
												return result.getResult() instanceof Integer && (Integer) result
														.getResult() >= 3;
											}
											return false;
										})
										.child(TaskNode.builder()
												.nodeId(NodeId.of("high-level-task"))
												.taskFunction(context -> {
													log.info("High level task (level >= 3)");
													return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
															"High level", String.class);
												})
												.build())
										.orElseChild(TaskNode.builder()
												.nodeId(NodeId.of("medium-level-task"))
												.taskFunction(context -> {
													log.info("Medium level task (2 <= level < 3)");
													return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
															"Medium level", String.class);
												})
												.build())
										.build())
								.build()))
				.workflowName("Nested Conditionals Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-17"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify high-level-task was executed (level = 3 >= 3)
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("high-level-task"))
				.isPresent());
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("medium-level-task"))
				.isPresent());

		SimpleAgenticExecutionResult<?> highLevelResult = (SimpleAgenticExecutionResult<?>) context
				.findExecutionMemoryByNodeId(NodeId.of("high-level-task"))
				.get();
		Assertions.assertEquals("High level", highLevelResult.getResult());
	}

	/**
	 * Test: Comprehensive workflow combining all node types in realistic pattern
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] init (SUCCESS)
	 * │ └─ Result: true (Boolean)
	 * ├─ [COND] check-enabled (checks init result)
	 * │ └─ [SEL] cache-or-fetch (SUCCESS)
	 * │ ├─ [T] check-cache (FAILURE - cache miss) ✗
	 * │ └─ [T] fetch-from-db (SUCCESS) ✓
	 * │ └─ Result: "DB data" (String)
	 * ├─ [COND-OR] validate-data (checks if fetch-from-db exists)
	 * │ ├─ [T] process-data (executed) (SUCCESS)
	 * │ │ └─ Result: "Processed" (String)
	 * │ └─ [T] handle-no-data (not executed)
	 * └─ [S] finalization (SUCCESS)
	 * ├─ [T] cleanup (SUCCESS)
	 * └─ [T] commit (SUCCESS)
	 * </pre>
	 *
	 * Flow:
	 * 1. Initialization: Init task → SUCCESS with true
	 * 2. Conditional Data Fetch:
	 * - Check-enabled condition → TRUE (init = true)
	 * - Execute cache-or-fetch selector:
	 * - Check-cache → FAILURE (cache miss)
	 * - Fetch-from-db → SUCCESS (fallback to DB)
	 * 3. Data Validation:
	 * - Validate-data checks if fetch-from-db exists → TRUE
	 * - Execute process-data → SUCCESS
	 * - Skip handle-no-data
	 * 4. Finalization:
	 * - Cleanup → SUCCESS
	 * - Commit → SUCCESS
	 *
	 * Use Case: Complete data processing pipeline with caching, fallback, validation, and cleanup
	 */
	@Test
	void testComplexWorkflow_AllNodeTypes() {
		// Complex workflow that combines all node types
		final var selectorNode = SelectorNode.builder()
				.nodeId(NodeId.of("cache-or-fetch"))
				.children(new AgenticNode[] { TaskNode.builder()
						.nodeId(NodeId.of("check-cache"))
						.taskFunction(context -> {
							log.info("Checking cache - MISS");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.FAILURE);
						})
						.build(), TaskNode.builder()
								.nodeId(NodeId.of("fetch-from-db"))
								.taskFunction(context -> {
									log.info("Fetching from database - SUCCESS");
									return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "DB data",
											String.class);
								})
								.build() })
				.build();

		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("complex-all-node-types"))
				.sequence("root", seq -> seq.task("init", context -> {
					log.info("Initialization");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, true, Boolean.class);
				})
						.node(ConditionalNode.builder()
								.nodeId(NodeId.of("check-enabled"))
								.conditionPredicate(context -> {
									final var initResult = context.findExecutionMemoryByNodeId(NodeId.of("init"));
									return initResult.isPresent() && initResult
											.get() instanceof SimpleAgenticExecutionResult<?> && Boolean.TRUE.equals(
													((SimpleAgenticExecutionResult<?>) initResult.get()).getResult());
								})
								.child(selectorNode)
								.build())
						.node(ConditionalOrElseNode.builder()
								.nodeId(NodeId.of("validate-data"))
								.conditionPredicate(context -> context.findExecutionMemoryByNodeId(NodeId.of(
										"fetch-from-db"))
										.isPresent())
								.child(TaskNode.builder()
										.nodeId(NodeId.of("process-data"))
										.taskFunction(context -> {
											log.info("Processing fetched data");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
													"Processed", String.class);
										})
										.build())
								.orElseChild(TaskNode.builder()
										.nodeId(NodeId.of("handle-no-data"))
										.taskFunction(context -> {
											log.info("Handling no data case");
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
										})
										.build())
								.build())
						.sequence(NodeId.of("finalization"), finalSeq -> finalSeq.task("cleanup", context -> {
							log.info("Cleanup task");
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
						})
								.task("commit", context -> {
									log.info("Commit task");
									return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS);
								})))
				.workflowName("Complex All Node Types Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-18"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify expected execution path
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("init"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("check-cache"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("fetch-from-db"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("process-data"))
				.isPresent());
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("handle-no-data"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("cleanup"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("commit"))
				.isPresent());
	}

	/**
	 * Test: Data transformation pipeline with conditional processing
	 *
	 * Tree Structure:
	 * <pre>
	 * [S] root (SUCCESS)
	 * ├─ [T] input-task (SUCCESS)
	 * │ └─ Result: 10 (Integer)
	 * ├─ [T] multiply-task (SUCCESS)
	 * │ └─ Reads input-task (10), returns: 20 (Integer)
	 * ├─ [COND-OR] threshold-check (checks if multiply-task > 15)
	 * │ ├─ [T] high-value-processing (executed, 20 > 15) (SUCCESS)
	 * │ │ └─ Reads multiply-task (20), returns: "High: 20" (String)
	 * │ └─ [T] low-value-processing (not executed)
	 * └─ [T] output-task (SUCCESS)
	 * └─ Reads high-value-processing, returns: "High: 20" (String)
	 * </pre>
	 *
	 * Data Flow:
	 * <pre>
	 * 10 → [multiply × 2] → 20 → [threshold check: 20 > 15?] → [high-value] → "High: 20"
	 * ↓ true
	 * high-value-processing
	 * </pre>
	 *
	 * Flow:
	 * 1. Input: Input-task generates value 10
	 * 2. Transform: Multiply-task reads 10 from memory, returns 20
	 * 3. Conditional Processing:
	 * - Threshold-check reads 20 from memory
	 * - Check if 20 > 15 → TRUE
	 * - Execute high-value-processing: Reads 20, returns "High: 20"
	 * - Skip low-value-processing
	 * 4. Output: Output-task reads high-value-processing result
	 *
	 * Use Case: ETL pipeline, data validation and routing, tiered processing based on data values
	 */
	@Test
	void testComplexWorkflow_DataFlow() {
		// Test data flowing through complex behavior tree
		final var workflow = AgenticWorkflowBuilder.create(WorkflowId.of("complex-data-flow"))
				.sequence("root", seq -> seq.task("input-task", context -> {
					log.info("Input task - generating data");
					return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, 10, Integer.class);
				})
						.task("multiply-task", context -> {
							final var inputResult = context.findExecutionMemoryByNodeId(NodeId.of("input-task"));
							Assertions.assertTrue(inputResult.isPresent());
							SimpleAgenticExecutionResult<?> input = (SimpleAgenticExecutionResult<?>) inputResult.get();
							Integer value = (Integer) input.getResult();
							log.info("Multiply task - {} * 2 = {}", value, value * 2);
							return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, value * 2,
									Integer.class);
						})
						.node(ConditionalOrElseNode.builder()
								.nodeId(NodeId.of("threshold-check"))
								.conditionPredicate(context -> {
									final var multiplyResult = context.findExecutionMemoryByNodeId(NodeId.of(
											"multiply-task"));
									if (multiplyResult.isPresent() && multiplyResult
											.get() instanceof SimpleAgenticExecutionResult<?>) {
										SimpleAgenticExecutionResult<?> result = (SimpleAgenticExecutionResult<?>) multiplyResult
												.get();
										return result.getResult() instanceof Integer && (Integer) result.getResult()
												> 15;
									}
									return false;
								})
								.child(TaskNode.builder()
										.nodeId(NodeId.of("high-value-processing"))
										.taskFunction(context -> {
											final var multiplyResult = context.findExecutionMemoryByNodeId(NodeId.of(
													"multiply-task"));
											SimpleAgenticExecutionResult<?> multiply = (SimpleAgenticExecutionResult<?>) multiplyResult
													.get();
											Integer value = (Integer) multiply.getResult();
											log.info("High value processing - value: {}", value);
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS,
													"High: " + value, String.class);
										})
										.build())
								.orElseChild(TaskNode.builder()
										.nodeId(NodeId.of("low-value-processing"))
										.taskFunction(context -> {
											final var multiplyResult = context.findExecutionMemoryByNodeId(NodeId.of(
													"multiply-task"));
											SimpleAgenticExecutionResult<?> multiply = (SimpleAgenticExecutionResult<?>) multiplyResult
													.get();
											Integer value = (Integer) multiply.getResult();
											log.info("Low value processing - value: {}", value);
											return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, "Low: "
													+ value, String.class);
										})
										.build())
								.build())
						.task("output-task", context -> {
							// Verify final result
							final var processingResult = context.findExecutionMemoryByNodeId(NodeId.of(
									"low-value-processing"));
							if (processingResult.isEmpty()) {
								final var highProcessingResult = context.findExecutionMemoryByNodeId(NodeId.of(
										"high-value-processing"));
								Assertions.assertTrue(highProcessingResult.isPresent());
								SimpleAgenticExecutionResult<?> highResult = (SimpleAgenticExecutionResult<?>) highProcessingResult
										.get();
								String finalResult = (String) highResult.getResult();
								log.info("Output task - final result: {}", finalResult);
								return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, finalResult,
										String.class);
							} else {
								SimpleAgenticExecutionResult<?> lowResult = (SimpleAgenticExecutionResult<?>) processingResult
										.get();
								String finalResult = (String) lowResult.getResult();
								log.info("Output task - final result: {}", finalResult);
								return new SimpleAgenticExecutionResult<>(AgenticNodeStatus.SUCCESS, finalResult,
										String.class);
							}
						}))
				.workflowName("Complex Data Flow Workflow")
				.build();

		final var context = AgenticExecutionContext.builder()
				.executionId(ExecutionId.of("exec-19"))
				.executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
				.build();

		final var result = workflow.execute(context);

		// Assert workflow succeeded
		Assertions.assertEquals(AgenticNodeStatus.SUCCESS, result.getStatus());

		// Verify data flow: 10 -> 20 -> "Low: 20" (since 20 is not > 15... wait, 20 > 15 is true!)
		// So it should be high-value-processing
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("input-task"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("multiply-task"))
				.isPresent());
		Assertions.assertFalse(context.findExecutionMemoryByNodeId(NodeId.of("low-value-processing"))
				.isPresent());
		Assertions.assertTrue(context.findExecutionMemoryByNodeId(NodeId.of("high-value-processing"))
				.isPresent());

		final var outputResult = context.findExecutionMemoryByNodeId(NodeId.of("output-task"));
		Assertions.assertTrue(outputResult.isPresent());
		SimpleAgenticExecutionResult<?> output = (SimpleAgenticExecutionResult<?>) outputResult.get();
		Assertions.assertEquals("High: 20", output.getResult());
	}
}
