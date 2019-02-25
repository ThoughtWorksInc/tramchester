package com.tramchester.graph.Nodes;


import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class StationNode implements TramNode {
    private final String id;
    private final String name;

    public StationNode(Node node) {
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
        this.name = node.getProperty(GraphStaticKeys.Station.NAME).toString();
    }

    private StationNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static StationNode TestOnly(String id, String name) {
        return new StationNode(id,name);
    }

    @Override
    public String toString() {
        return "StationNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isStation() {
        return true;
    }

    @Override
    public boolean isRouteStation() {
        return false;
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public boolean isPlatform() {
        return false;
    }

    @Override
    public boolean isService() {
        return false;
    }

    @Override
    public boolean isHour() {
        return false;
    }

    @Override
    public boolean isMinute() {
        return false;
    }

}
