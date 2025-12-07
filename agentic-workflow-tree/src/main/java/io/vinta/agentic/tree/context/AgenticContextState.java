package io.vinta.agentic.tree.context;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.With;

@Getter
@With
public class AgenticContextState<K, V> {
	private final Map<K, V> stateData;

	public AgenticContextState() {
		this.stateData = new ConcurrentHashMap<>();
	}

	public AgenticContextState(Map<K, V> stateData) {
		this.stateData = Optional.ofNullable(stateData)
				.map(ConcurrentHashMap::new)
				.orElse(new ConcurrentHashMap<>());
	}

}