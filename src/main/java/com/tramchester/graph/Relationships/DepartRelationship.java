package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class DepartRelationship extends TransportCostRelationship {
    public DepartRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public static DepartRelationship TestOnly(int cost, String id, TramNode firstNode, TramNode endNode) {
        return new DepartRelationship(cost,id, firstNode, endNode);
    }

    private DepartRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id,startNode, endNode);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Depart;
    }

    @Override
    public boolean isGoesTo() {
        return false;
    }

    @Override
    public boolean isBoarding() {
        return false;
    }

    @Override
    public boolean isDepartTram() {
        return true;
    }

    @Override
    public boolean isInterchange() {
        return false;
    }

    @Override
    public boolean isWalk() {
        return false;
    }

    @Override
    public String toString() {
        return "DepartRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}
