package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationState extends TraversalState {
    private final long routeStationNodeId;
    private final boolean justBoarded;
    private final ExistingTrip maybeExistingTrip;

    public RouteStationState(TraversalState parent, Iterable<Relationship> relationships, long routeStationNodeId,
                             int cost, boolean justBoarded) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = justBoarded;
        maybeExistingTrip = ExistingTrip.none();
    }

    public RouteStationState(TraversalState parent, Iterable<Relationship> relationships,
                             long routeStationNodeId, String tripId, int cost) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = false;
        maybeExistingTrip = ExistingTrip.onTrip(tripId);
    }

    @Override
    public String toString() {
        return "RouteStationState{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", cost=" + super.getCurrentCost() +
                ", justBoarded=" + justBoarded +
                ", maybeExistingTrip='" + maybeExistingTrip + '\'' +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.PLATFORM) {
            return toPlatform(nextNode, journeyState, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return ServiceState.fromRouteStation(this, maybeExistingTrip, nextNode, cost);

        }
        if (config.getBus() && (nodeLabel == GraphBuilder.Labels.BUS_STATION)) {
            return toBusStation(nextNode, journeyState, cost);
        }

        throw new RuntimeException(format("Unexpected node type: %s state :%s ", nodeLabel, this));
    }

    private TraversalState toBusStation(Node busStationNode, JourneyState journeyState, int cost) {
        // no platforms in bus network, direct to station
        try {
            journeyState.leaveBus(getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to depart tram",e);
        }

        // if bus station then may have arrived
        long busStationNodeId = busStationNode.getId();
        if (busStationNodeId == destinationNodeId) {
            return new DestinationState(this, cost);
        }

        if (maybeExistingTrip.isOnTrip() || justBoarded) {
            return BusStationState.fromRouteStationOnTrip(this, routeStationNodeId, busStationNode, cost);
        } else {
            return BusStationState.fromRouteStation(this, busStationNode, cost);

        }
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        try {
            journeyState.leaveTram(getTotalCost());

            // TODO Push into PlatformState
            // if towards ONE destination just return that one relationship
            if (destinationStationIds.size()==1) {
                for (Relationship relationship : platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM)) {
                    if (destinationStationIds.contains(relationship.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                        return PlatformState.fromRouteStationTowardsDest(this, relationship, platformNode,  cost);
                    }
                }
            }

            if (maybeExistingTrip.isOnTrip() || justBoarded) {
                return PlatformState.fromRouteStationOnTrip(this, routeStationNodeId, platformNode, cost);

            } else {
                return PlatformState.fromRouteStation(this, platformNode, cost);

            }
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }
    }
}
