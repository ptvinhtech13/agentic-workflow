# Quick Start Guide

Get up and running with Agentic Workflow Tree in 5 minutes!

## 📦 Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.vinta.agentic</groupId>
    <artifactId>agentic-workflow-tree</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 🚀 30-Second Example

```java
import io.vinta.agentic.tree.fluent.AgenticWorkflowBuilder;
import io.vinta.agentic.tree.context.AgenticExecutionContext;
import io.vinta.agentic.tree.execution.*;
import io.vinta.agentic.tree.identifier.*;
import io.vinta.agentic.tree.node.AgenticNodeStatus;

public class HelloWorld {
    public static void main(String[] args) {
        // Build workflow
        var workflow = AgenticWorkflowBuilder
            .create(WorkflowId.of("hello-workflow"))
            .sequence("main", seq -> seq
                .task("greet", ctx -> {
                    System.out.println("Hello, Agentic Workflow!");
                    return new SimpleAgenticExecutionResult<>(
                        AgenticNodeStatus.SUCCESS);
                })
            )
            .workflowName("Hello World")
            .build();

        // Execute
        var context = AgenticExecutionContext.builder()
            .executionId(ExecutionId.of("exec-1"))
            .executionMemoryRepository(new InMemoryAgenticExecutionMemoryRepository())
            .build();

        workflow.execute(context);
    }
}
```

**Output:**
```
Hello, Agentic Workflow!
```

## 🎯 Common Patterns

### Pattern 1: Sequential Tasks

```java
.sequence("process", seq -> seq
    .task("step-1", ctx -> doStep1())
    .task("step-2", ctx -> doStep2())
    .task("step-3", ctx -> doStep3())
)
```

### Pattern 2: Fallback / Retry

```java
SelectorNode.builder()
    .nodeId(NodeId.of("retry"))
    .children(new AgenticNode[]{
        primaryTask,
        backupTask,
        lastResortTask
    })
    .build()
```

### Pattern 3: Conditional Execution

```java
.conditional(
    NodeId.of("check"),
    ctx -> shouldExecute(),
    taskToExecute
)
```

### Pattern 4: If-Else

```java
.conditionalOrElse(
    NodeId.of("route"),
    ctx -> isHighPriority(),
    highPriorityTask,
    normalTask
)
```

### Pattern 5: Data Sharing

```java
.task("producer", ctx -> {
    var data = generateData();
    return new SimpleAgenticExecutionResult<>(
        AgenticNodeStatus.SUCCESS,
        data,
        String.class
    );
})
.task("consumer", ctx -> {
    var result = ctx.findExecutionMemoryByNodeId(
        NodeId.of("producer"));
    // Use the data
})
```

## 📚 Next Steps

1. Read the full [README.md](../README.md)
2. Check [TEST_DOCUMENTATION.md](TEST_DOCUMENTATION.md) for examples
3. Try the complex example in README
4. Build your own workflows!

## 💡 Tips

- Use descriptive node IDs
- Always handle errors
- Test your workflows
- Start simple, add complexity gradually

## 🔗 Resources

- [Full Documentation](../README.md)
- [API Reference](../README.md#-api-reference)
- [Best Practices](../README.md#-best-practices)
- [Test Examples](TEST_DOCUMENTATION.md)
