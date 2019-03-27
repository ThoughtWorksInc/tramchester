package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class HourNode extends TramNode {
    private final String id;
    private final String name;

    public HourNode(Node node) {
        this.name = node.getProperty(GraphStaticKeys.HOUR).toString();
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isHour() {
        return true;
    }

}
