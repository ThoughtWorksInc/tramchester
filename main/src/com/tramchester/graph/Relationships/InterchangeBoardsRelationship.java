package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Relationship;

public class InterchangeBoardsRelationship extends TransportCostRelationship {

    public InterchangeBoardsRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Board;
    }

    @Override
    public boolean isBoarding() {
        return true;
    }

    @Override
    public boolean isInterchange() {
        return true;
    }

    @Override
    public int getCost() {
        return TransportGraphBuilder.INTERCHANGE_BOARD_COST;
    }

    @Override
    public String toString() {
        return "InterchangeBoardsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}
