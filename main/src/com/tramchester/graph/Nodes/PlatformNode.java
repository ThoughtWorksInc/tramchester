package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class PlatformNode extends TramNode {
    private final String id;
    private final String name;

    private PlatformNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static PlatformNode TestOnly(String id, String name) {
        return new PlatformNode(id,name);
    }

    public PlatformNode(Node node) {
        this.name = node.getProperty(GraphStaticKeys.Station.NAME).toString();
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
    public boolean isPlatform() {
        return true;
    }

    @Override
    public String toString() {
        return "PlatformNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

}
