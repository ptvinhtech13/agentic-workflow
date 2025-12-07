package io.vinta.agentic.tree.configuration;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@With
@Builder
public class AgenticNodeConfiguration {
	private final String name;
	private final String description;
	private final Map<String, Object> settings;
}