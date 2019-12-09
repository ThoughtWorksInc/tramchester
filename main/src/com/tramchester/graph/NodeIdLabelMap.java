package com.tramchester.graph;

import java.util.HashMap;
import java.util.Map;

public class NodeIdLabelMap {
    private Map<Long, TransportGraphBuilder.Labels> map;

    public NodeIdLabelMap() {
        map = new HashMap<>();
    }

    public TransportGraphBuilder.Labels getLabel(long id) {
        return map.get(id);
    }

    public void put(long id, TransportGraphBuilder.Labels label) {
        map.put(id, label);
    }
}
