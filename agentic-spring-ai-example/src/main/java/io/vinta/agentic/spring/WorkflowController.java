package io.vinta.agentic.spring;

import io.vinta.agentic.tree.WorkflowNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

	@GetMapping("/node/{id}/{name}")
	public WorkflowNode createNode(@PathVariable String id, @PathVariable String name) {
		return new WorkflowNode(id, name);
	}

	@GetMapping("/health")
	public String health() {
		return "Workflow service is running!";
	}
}
