package com.tramchester.graph.search.states;

import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateJustBoarded extends TraversalState {

    public static class Builder {
        private final SortsPositions sortsPositions;
        private final Set<String> destinationStationIds;

        public Builder(SortsPositions sortsPositions, Set<String> destinationStationIds) {
            this.sortsPositions = sortsPositions;
            this.destinationStationIds = destinationStationIds;
        }

        public TraversalState fromPlatformState(PlatformState platformState, Node node, int cost) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM), platformState);
            node.getRelationships(OUTGOING, TO_SERVICE).forEach(outbounds::add);
            return new RouteStationStateJustBoarded(platformState, outbounds, cost);
        }

        public TraversalState fromBusStation(BusStationState busStationState, Node node, int cost) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING,
                    DEPART, INTERCHANGE_DEPART), busStationState);
            outbounds.addAll(orderSvcRelationshipsForBus(node));
            return new RouteStationStateJustBoarded(busStationState, outbounds, cost);
        }

        private Collection<Relationship> orderSvcRelationshipsForBus(Node node) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);

            Set<SortsPositions.HasStationId<Relationship>> relationships = new HashSet<>();
            toServices.forEach(svcRelationship -> relationships.add(new RelationshipFacade(svcRelationship)));

            return sortsPositions.sortedByNearTo(destinationStationIds, relationships);
        }
    }

    @Override
    public String toString() {
        return "RouteStationStateJustBoarded{} " + super.toString();
    }

    private RouteStationStateJustBoarded(TraversalState traversalState, List<Relationship> outbounds, int cost) {
        super(traversalState, outbounds, cost);
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return builders.service.fromRouteStation(this, nextNode, cost);
        }

        // if one to one relationship between platforms and route stations, or bus stations and route stations,
        // no longer holds then this will throw
        throw new RuntimeException(format("Unexpected node type: %s state :%s ", nodeLabel, this));
    }


    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;
        private final String stationId;

        private RelationshipFacade(Relationship relationship) {
            this.relationship = relationship;
            this.stationId =  relationship.getProperty(GraphStaticKeys.TOWARDS_STATION_ID).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RelationshipFacade that = (RelationshipFacade) o;
            return stationId.equals(that.stationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stationId);
        }

        @Override
        public String getStationId() {
            return stationId;
        }

        @Override
        public Relationship getContained() {
            return relationship;
        }
    }
}