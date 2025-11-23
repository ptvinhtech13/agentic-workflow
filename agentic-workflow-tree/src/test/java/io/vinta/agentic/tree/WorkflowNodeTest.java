package io.vinta.agentic.tree;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WorkflowNodeTest {

	@Test
	void testWorkflowNodeCreation() {
		WorkflowNode node = new WorkflowNode("1", "TestNode");

		assertEquals("1", node.getId());
		assertEquals("TestNode", node.getName());
	}

	@Test
	void testToString() {
		WorkflowNode node = new WorkflowNode("2", "SampleNode");
		String expected = "WorkflowNode{id='2', name='SampleNode'}";

		assertEquals(expected, node.toString());
	}
}
