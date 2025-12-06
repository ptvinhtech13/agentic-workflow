package io.vinta.agentic.tree.runtime;

public interface AgenticCumulativeData<T> {
    String getNodeId();

    Class<T> getClassRuntimeData();

    T getRuntimeData();
}