# Agentic Workflow Test Documentation

This document provides visual diagrams for all behavior tree tests in the `AgenticWorkflowBuilderTest` suite.

**Legend:**
- `[S]` = SequenceNode (executes children sequentially, stops on first non-SUCCESS)
- `[SEL]` = SelectorNode (executes children until first SUCCESS, like OR/fallback)
- `[T]` = TaskNode (leaf node that executes a task)
- `[COND]` = ConditionalNode (executes child only if condition is true)
- `[COND-OR]` = ConditionalOrElseNode (if-else branching)
- `✓` = SUCCESS status
- `✗` = FAILURE status
- `⊘` = SKIPPED status

---

## SIMPLE TESTS - Basic Single-Level Behavior

### 1. testSimpleTaskExecution

**Purpose:** Test basic task node execution and result storage

```
[S] root (SUCCESS)
 └─ [T] task-1 (SUCCESS)
     └─ Result: "Task completed" (String)
```

**Flow:**
1. Execute single task
2. Task returns SUCCESS with result "Task completed"
3. Verify task result is stored in execution memory

**Assertions:**
- Workflow status = SUCCESS
- Task-1 status = SUCCESS
- Task-1 result = "Task completed"

---

### 2. testSimpleSequence_AllSuccess

**Purpose:** Test sequence executing all tasks when all succeed

```
[S] root (SUCCESS)
 ├─ [T] task-1 (SUCCESS)
 ├─ [T] task-2 (SUCCESS)
 │   └─ Result: "Result from task 2" (String)
 └─ [T] task-3 (SUCCESS)
```

**Flow:**
1. Task-1 executes → SUCCESS
2. Task-2 executes → SUCCESS with result "Result from task 2"
3. Task-3 executes → SUCCESS
4. Sequence completes successfully

**Assertions:**
- Workflow status = SUCCESS
- All 3 tasks executed (size = 3)
- All tasks have SUCCESS status
- Task-2 result stored correctly in memory

---

### 3. testSimpleSequence_StopOnFailure

**Purpose:** Test sequence stopping on first failure (short-circuit behavior)

```
[S] root (FAILURE)
 ├─ [T] task-1 (SUCCESS) ✓
 ├─ [T] task-2 (FAILURE) ✗ ← Stops here
 └─ [T] task-3 (not executed)
```

**Flow:**
1. Task-1 executes → SUCCESS
2. Task-2 executes → FAILURE
3. Sequence stops immediately (short-circuit)
4. Task-3 never executes

**Assertions:**
- Workflow status = FAILURE
- Only 2 tasks executed (task-3 skipped)
- Task-1 status = SUCCESS
- Task-2 status = FAILURE
- Task-3 not in execution memory

---

### 4. testSimpleSelector_FirstSuccess

**Purpose:** Test selector stopping at first successful child (OR/fallback logic)

```
[SEL] selector-1 (SUCCESS)
 ├─ [T] task-1 (FAILURE) ✗
 ├─ [T] task-2 (SUCCESS) ✓ ← Stops here
 │   └─ Result: "Selected task 2" (String)
 └─ [T] task-3 (not executed)
```

**Flow:**
1. Task-1 executes → FAILURE
2. Task-2 executes → SUCCESS (first success!)
3. Selector stops immediately (short-circuit)
4. Task-3 never executes

**Assertions:**
- Selector status = SUCCESS
- Only 2 tasks executed (task-3 skipped)
- Task-1 status = FAILURE
- Task-2 status = SUCCESS
- Task-3 not in children results

---

### 5. testSimpleSelector_AllFail

**Purpose:** Test selector returning FAILURE when all children fail

```
[SEL] selector-1 (FAILURE)
 ├─ [T] task-1 (FAILURE) ✗
 ├─ [T] task-2 (FAILURE) ✗
 └─ [T] task-3 (FAILURE) ✗
     └─ All failed, selector returns FAILURE
```

**Flow:**
1. Task-1 executes → FAILURE
2. Task-2 executes → FAILURE
3. Task-3 executes → FAILURE
4. All children failed → Selector returns FAILURE

**Assertions:**
- Selector status = FAILURE
- All 3 tasks executed (tried all options)
- All tasks have FAILURE status

---

### 6. testConditionalNode_ConditionTrue

**Purpose:** Test conditional node executing child when condition is true

```
[COND] conditional-1 (condition = true) (SUCCESS)
 └─ [T] task-1 (executed) (SUCCESS)
     └─ Result: "Conditional task executed" (String)
```

**Flow:**
1. Evaluate condition → TRUE
2. Execute child task-1 → SUCCESS
3. Return child's status (SUCCESS)

**Assertions:**
- Conditional status = SUCCESS
- Task-1 executed and stored in memory
- Task-1 status = SUCCESS

---

### 7. testConditionalNode_ConditionFalse

**Purpose:** Test conditional node skipping child when condition is false

```
[COND] conditional-1 (condition = false) (SKIPPED)
 └─ [T] task-1 (not executed)
```

**Flow:**
1. Evaluate condition → FALSE
2. Skip child execution
3. Return SKIPPED status

**Assertions:**
- Conditional status = SKIPPED
- Task-1 never executed
- Task-1 not in execution memory

---

### 8. testConditionalOrElseNode_TrueBranch

**Purpose:** Test if-else node taking the true branch

```
[COND-OR] conditional-or-else-1 (condition = true) (SUCCESS)
 ├─ [T] true-task (executed) (SUCCESS)
 │   └─ Result: "True branch" (String)
 └─ [T] false-task (not executed)
```

**Flow:**
1. Evaluate condition → TRUE
2. Execute true-task → SUCCESS
3. Skip false-task
4. Return true-task's status

**Assertions:**
- ConditionalOrElse status = SUCCESS
- True-task executed and in memory
- False-task not executed, not in memory
- True-task result = "True branch"

---

### 9. testConditionalOrElseNode_FalseBranch

**Purpose:** Test if-else node taking the false branch

```
[COND-OR] conditional-or-else-1 (condition = false) (SUCCESS)
 ├─ [T] true-task (not executed)
 └─ [T] false-task (executed) (SUCCESS)
     └─ Result: "False branch" (String)
```

**Flow:**
1. Evaluate condition → FALSE
2. Skip true-task
3. Execute false-task → SUCCESS
4. Return false-task's status

**Assertions:**
- ConditionalOrElse status = SUCCESS
- False-task executed and in memory
- True-task not executed, not in memory
- False-task result = "False branch"

---

## INTERMEDIATE TESTS - Combined Behaviors

### 10. testNestedSequences

**Purpose:** Test sequences containing other sequences (hierarchical structure)

```
[S] root (SUCCESS)
 ├─ [T] task-1 (SUCCESS)
 ├─ [S] nested-seq-1 (SUCCESS)
 │   ├─ [T] task-2 (SUCCESS)
 │   │   └─ Result: 100 (Integer)
 │   └─ [T] task-3 (SUCCESS)
 └─ [T] task-4 (SUCCESS)
```

**Flow:**
1. Task-1 executes → SUCCESS
2. Nested-seq-1 begins:
   - Task-2 executes → SUCCESS (stores 100)
   - Task-3 executes → SUCCESS
   - Nested-seq-1 → SUCCESS
3. Task-4 executes → SUCCESS
4. Root sequence → SUCCESS

**Assertions:**
- Root sequence status = SUCCESS
- Root has 3 children (task-1, nested-seq-1, task-4)
- Nested sequence status = SUCCESS
- Nested sequence has 2 children (task-2, task-3)
- Task-2 result = 100 (Integer)

---

### 11. testSequenceWithConditional

**Purpose:** Test conditional execution within a sequence based on previous task result

```
[S] root (SUCCESS)
 ├─ [T] task-1 (SUCCESS)
 │   └─ Result: "enable-task-3" (String)
 ├─ [COND] conditional-1 (condition checks task-1 result)
 │   └─ [T] task-2 (executed because condition = true) (SUCCESS)
 └─ [T] task-3 (SUCCESS)
```

**Flow:**
1. Task-1 executes → SUCCESS with "enable-task-3"
2. Conditional-1 evaluates:
   - Read task-1 result from memory
   - Check if result == "enable-task-3" → TRUE
   - Execute task-2 → SUCCESS
3. Task-3 executes → SUCCESS

**Assertions:**
- All 3 tasks executed and in memory
- Workflow status = SUCCESS
- Conditional correctly read and acted on task-1 result

---

### 12. testSelectorWithMixedResults

**Purpose:** Test selector fallback pattern with multiple failures before success

```
[SEL] selector-1 (SUCCESS)
 ├─ [T] strategy-1 (FAILURE) ✗
 ├─ [T] strategy-2 (FAILURE) ✗
 └─ [T] strategy-3 (SUCCESS) ✓ ← Fallback succeeded
     └─ Result: "Fallback strategy" (String)
```

**Flow:**
1. Strategy-1 executes → FAILURE
2. Strategy-2 executes → FAILURE
3. Strategy-3 executes → SUCCESS (fallback works!)
4. Selector stops and returns SUCCESS

**Assertions:**
- Selector status = SUCCESS
- All 3 strategies were tried
- Strategy-3 result = "Fallback strategy"

---

### 13. testConditionalAccessingExecutionMemory

**Purpose:** Test conditional predicates reading data from execution memory

```
[S] root (SUCCESS)
 ├─ [T] data-task (SUCCESS)
 │   └─ Result: 42 (Integer)
 └─ [COND] conditional-check (reads data-task, checks if > 40)
     └─ [T] conditional-task (executed because 42 > 40) (SUCCESS)
         └─ Result: "Threshold exceeded" (String)
```

**Flow:**
1. Data-task executes → SUCCESS with value 42
2. Conditional-check evaluates:
   - Read data-task result from memory → 42
   - Check if 42 > 40 → TRUE
   - Execute conditional-task → SUCCESS
3. Sequence completes successfully

**Assertions:**
- Both tasks executed and in memory
- Conditional correctly accessed execution memory
- Conditional-task executed (because threshold exceeded)

---

### 14. testSequenceWithConditionalOrElse

**Purpose:** Test if-else branching within a sequence, based on previous task result

```
[S] root (SUCCESS)
 ├─ [T] check-task (SUCCESS)
 │   └─ Result: false (Boolean)
 ├─ [COND-OR] conditional-or-else (checks check-task result)
 │   ├─ [T] true-branch (not executed)
 │   └─ [T] false-branch (executed) (SUCCESS)
 │       └─ Result: "Else branch" (String)
 └─ [T] final-task (SUCCESS)
```

**Flow:**
1. Check-task executes → SUCCESS with false
2. Conditional-or-else evaluates:
   - Read check-task result → false
   - Condition = false
   - Skip true-branch
   - Execute false-branch → SUCCESS
3. Final-task executes → SUCCESS

**Assertions:**
- True-branch not executed (not in memory)
- False-branch executed (in memory)
- Final-task executed
- Workflow status = SUCCESS

---

## COMPLEX TESTS - Full Behavior Tree Patterns

### 15. testSequenceWithSelector_FallbackPattern

**Purpose:** Test service fallback pattern (primary fails, backup succeeds)

```
[S] root (SUCCESS)
 ├─ [T] init-task (SUCCESS)
 ├─ [SEL] fallback-selector (SUCCESS)
 │   ├─ [T] primary-service (FAILURE) ✗
 │   └─ [T] backup-service (SUCCESS) ✓
 │       └─ Result: "Backup data" (String)
 └─ [T] process-task (SUCCESS)
     └─ Processes data from backup-service
```

**Flow:**
1. Init-task executes → SUCCESS
2. Fallback-selector begins:
   - Primary-service executes → FAILURE
   - Backup-service executes → SUCCESS (fallback!)
   - Selector → SUCCESS
3. Process-task executes → SUCCESS
   - Reads backup-service result from memory
   - Processes the backup data

**Assertions:**
- Workflow succeeded using backup service
- Primary-service attempted (in memory with FAILURE)
- Backup-service succeeded (in memory with SUCCESS)
- Process-task successfully used backup data

**Use Case:** Database connection fallback, API endpoint redundancy, service resilience

---

### 16. testSelectorWithSequences_TryMultipleStrategies

**Purpose:** Test selector choosing between different strategy implementations

```
[SEL] strategy-selector (SUCCESS)
 ├─ [T] strategy-1-single-step (FAILURE) ✗
 └─ [T] strategy-2-single-step (SUCCESS) ✓
     └─ Result: "Strategy 2 result" (String)
```

**Flow:**
1. Strategy-1 executes → FAILURE
2. Strategy-2 executes → SUCCESS
3. Selector returns SUCCESS

**Assertions:**
- Selector succeeded with strategy-2
- Strategy-2 in memory with result

**Use Case:** Algorithm selection, optimization strategies, different processing approaches

---

### 17. testNestedConditionalsInSequence

**Purpose:** Test multi-level conditional logic (nested if-then-else)

```
[S] root (SUCCESS)
 ├─ [T] level-task (SUCCESS)
 │   └─ Result: 3 (Integer)
 └─ [COND] level-check (checks if level >= 2)
     └─ [COND-OR] nested-conditional (checks if level >= 3)
         ├─ [T] high-level-task (executed, level=3) (SUCCESS)
         │   └─ Result: "High level" (String)
         └─ [T] medium-level-task (not executed)
```

**Flow:**
1. Level-task executes → SUCCESS with value 3
2. Level-check evaluates:
   - Read level-task → 3
   - Check if 3 >= 2 → TRUE
   - Execute nested-conditional
3. Nested-conditional evaluates:
   - Read level-task → 3
   - Check if 3 >= 3 → TRUE
   - Execute high-level-task → SUCCESS
   - Skip medium-level-task

**Assertions:**
- High-level-task executed (level 3 >= 3)
- Medium-level-task not executed
- High-level-task result = "High level"

**Use Case:** Multi-tier priority handling, graduated permissions, tiered processing

---

### 18. testComplexWorkflow_AllNodeTypes

**Purpose:** Comprehensive workflow combining all node types in realistic pattern

```
[S] root (SUCCESS)
 ├─ [T] init (SUCCESS)
 │   └─ Result: true (Boolean)
 ├─ [COND] check-enabled (checks init result)
 │   └─ [SEL] cache-or-fetch (SUCCESS)
 │       ├─ [T] check-cache (FAILURE - cache miss) ✗
 │       └─ [T] fetch-from-db (SUCCESS) ✓
 │           └─ Result: "DB data" (String)
 ├─ [COND-OR] validate-data (checks if fetch-from-db exists)
 │   ├─ [T] process-data (executed) (SUCCESS)
 │   │   └─ Result: "Processed" (String)
 │   └─ [T] handle-no-data (not executed)
 └─ [S] finalization (SUCCESS)
     ├─ [T] cleanup (SUCCESS)
     └─ [T] commit (SUCCESS)
```

**Flow:**
1. **Initialization:** Init task → SUCCESS with true
2. **Conditional Data Fetch:**
   - Check-enabled condition → TRUE (init = true)
   - Execute cache-or-fetch selector:
     - Check-cache → FAILURE (cache miss)
     - Fetch-from-db → SUCCESS (fallback to DB)
3. **Data Validation:**
   - Validate-data checks if fetch-from-db exists → TRUE
   - Execute process-data → SUCCESS
   - Skip handle-no-data
4. **Finalization:**
   - Cleanup → SUCCESS
   - Commit → SUCCESS

**Assertions:**
- Complete execution path verified:
  - ✓ init
  - ✓ check-cache (failed)
  - ✓ fetch-from-db (succeeded)
  - ✓ process-data (executed)
  - ✗ handle-no-data (skipped)
  - ✓ cleanup
  - ✓ commit

**Use Case:** Complete data processing pipeline with caching, fallback, validation, and cleanup

---

### 19. testComplexWorkflow_DataFlow

**Purpose:** Test data transformation pipeline with conditional processing

```
[S] root (SUCCESS)
 ├─ [T] input-task (SUCCESS)
 │   └─ Result: 10 (Integer)
 ├─ [T] multiply-task (SUCCESS)
 │   └─ Reads input-task (10), returns: 20 (Integer)
 ├─ [COND-OR] threshold-check (checks if multiply-task > 15)
 │   ├─ [T] high-value-processing (executed, 20 > 15) (SUCCESS)
 │   │   └─ Reads multiply-task (20), returns: "High: 20" (String)
 │   └─ [T] low-value-processing (not executed)
 └─ [T] output-task (SUCCESS)
     └─ Reads high-value-processing, returns: "High: 20" (String)
```

**Data Flow Diagram:**
```
10 → [multiply × 2] → 20 → [threshold check: 20 > 15?] → [high-value] → "High: 20"
                                                  ↓ true
                                            high-value-processing
```

**Flow:**
1. **Input:** Input-task generates value 10
2. **Transform:** Multiply-task reads 10 from memory, returns 20
3. **Conditional Processing:**
   - Threshold-check reads 20 from memory
   - Check if 20 > 15 → TRUE
   - Execute high-value-processing:
     - Reads 20 from memory
     - Returns "High: 20"
   - Skip low-value-processing
4. **Output:** Output-task:
   - Checks low-value-processing → not present
   - Reads high-value-processing → "High: 20"
   - Returns final result "High: 20"

**Assertions:**
- Data transformation: 10 → 20 → "High: 20"
- High-value-processing executed
- Low-value-processing skipped
- Final output = "High: 20"

**Use Case:** ETL pipeline, data validation and routing, tiered processing based on data values

---

## Summary Statistics

| Category | Count | Node Types Used |
|----------|-------|----------------|
| **Simple Tests** | 9 | Sequence, Selector, Task, Conditional, ConditionalOrElse |
| **Intermediate Tests** | 6 | Nested Sequences, Conditionals + Memory |
| **Complex Tests** | 4 | All types combined with real-world patterns |
| **Total Tests** | **19** | **100% Pass Rate** |

## Test Coverage Matrix

| Node Type | Simple | Intermediate | Complex | Total Tests |
|-----------|--------|--------------|---------|-------------|
| **SequenceNode** | 3 | 4 | 4 | 11 |
| **SelectorNode** | 2 | 1 | 3 | 6 |
| **TaskNode** | 9 | 6 | 4 | 19 (all) |
| **ConditionalNode** | 2 | 2 | 2 | 6 |
| **ConditionalOrElseNode** | 2 | 1 | 2 | 5 |
| **Execution Memory Access** | 1 | 4 | 4 | 9 |
| **Data Flow** | 2 | 3 | 2 | 7 |

## Common Patterns Tested

1. **Sequence Short-Circuit**: Stop on first failure ✓
2. **Selector Fallback**: Try options until one succeeds ✓
3. **Conditional Execution**: Execute based on predicates ✓
4. **If-Else Branching**: Choose between two paths ✓
5. **Execution Memory**: Share data between nodes ✓
6. **Nested Structures**: Hierarchical node composition ✓
7. **Service Resilience**: Primary/backup patterns ✓
8. **Data Transformation**: Pipeline processing ✓
9. **Multi-tier Logic**: Nested conditionals ✓
10. **Status Propagation**: SUCCESS, FAILURE, SKIPPED ✓

---

**Test File Location:**
`agentic-workflow-tree/src/test/java/io/vinta/agentic/tree/AgenticWorkflowBuilderTest.java`

**All tests pass with 100% success rate** ✅
