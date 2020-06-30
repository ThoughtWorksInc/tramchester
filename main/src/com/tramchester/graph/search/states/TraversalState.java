package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class TraversalState implements ImmuatableTraversalState {

    private final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    protected final TramchesterConfig config;
    protected final NodeContentsRepository nodeOperations;
    protected final long destinationNodeId;
    protected final TraversalState parent;
    protected final List<String> destinationStationIds;
    protected final RouteStationStateJustBoarded.Builder routeStationStateBuilder;


    private TraversalState child;

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

    // initial only
    protected TraversalState(SortsPositions sortsPositions, TraversalState parent, NodeContentsRepository nodeOperations,
                             Iterable<Relationship> outbounds, long destinationNodeId, List<String> destinationStationdId,
                             int costForLastEdge, TramchesterConfig config) {
        this.parent = parent;
        this.nodeOperations = nodeOperations;
        this.outbounds = outbounds;
        this.destinationNodeId = destinationNodeId;
        this.destinationStationIds = destinationStationdId;
        this.costForLastEdge = costForLastEdge;
        this.config = config;
        parentCost = 0;
        this.routeStationStateBuilder = new RouteStationStateJustBoarded.Builder(sortsPositions, destinationStationIds);
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        this.nodeOperations = parent.nodeOperations;
        this.destinationNodeId = parent.destinationNodeId;
        this.destinationStationIds = parent.destinationStationIds;
        this.config = parent.config;
        this.routeStationStateBuilder = parent.routeStationStateBuilder;

        this.parent = parent;
        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalCost();
    }

    protected abstract TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node,
                                                      JourneyState journeyState, int cost);

    public TraversalState nextState(Path path, GraphBuilder.Labels nodeLabel, Node node,
                             JourneyState journeyState, int cost) {
        child = createNextState(path, nodeLabel, node, journeyState, cost);
        return child;
    }

    public void dispose() {
        if (child!=null) {
            child.dispose();
            child = null;
        }
    }

    public Iterable<Relationship> getOutbounds() {
        return outbounds;
    }

    // TODO Return iterable instead
    protected static List<Relationship> filterExcludingEndNode(Iterable<Relationship> relationships, NodeId hasNodeId) {
        long nodeId = hasNodeId.nodeId();
        ArrayList<Relationship> results = new ArrayList<>();
        for (Relationship relationship: relationships) {
            if (relationship.getEndNode().getId() != nodeId) {
                results.add(relationship);
            }
        }
        return results;

//        return  StreamSupport.stream(relationships.spliterator(), false).
//                filter(relationship -> relationship.getEndNode().getId()!= nodeIdToSkip).
//                collect(Collectors.toList());
    }

    public int getTotalCost() {
        return parentCost + getCurrentCost();
    }

    protected int getCurrentCost() {
        return costForLastEdge;
    }

}
