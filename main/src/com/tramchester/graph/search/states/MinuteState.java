package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.search.JourneyState;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.TRIP_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class MinuteState extends TraversalState {

    private final boolean interchangesOnly;

    public static class Builder {

        private final TramchesterConfig config;

        public Builder(TramchesterConfig config) {
            this.config = config;
        }

        public TraversalState fromHour(HourState hourState, Node node, int cost, ExistingTrip maybeExistingTrip) {
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TRAM_GOES_TO, BUS_GOES_TO);

            if (maybeExistingTrip.isOnTrip()) {
                String existingTripId = maybeExistingTrip.getTripId();
                Iterable<Relationship> filterBySingleTripId = filterBySingleTripId(hourState.nodeOperations,
                        relationships, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, existingTripId, cost, config.getChangeAtInterchangeOnly());
            } else {
                // starting a brand new journey
                String newTripId = getTrip(node);
                return new MinuteState(hourState, relationships, newTripId, cost, config.getChangeAtInterchangeOnly());
            }
        }
    }

    private final String tripId;

    private MinuteState(TraversalState parent, Iterable<Relationship> relationships, String tripId, int cost, boolean interchangesOnly) {
        super(parent, relationships, cost);
        this.tripId = tripId;
        this.interchangesOnly = interchangesOnly;
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "interchangesOnly=" + interchangesOnly +
                ", tripId='" + tripId + '\'' +
                "} " + super.toString();
    }

    private static String getTrip(Node endNode) {
        if (!endNode.hasProperty(TRIP_ID)) {
            return "";
        }
        return endNode.getProperty(TRIP_ID).toString().intern();
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            return toRouteStation(node, cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    private TraversalState toRouteStation(Node node, int cost) {
        Iterable<Relationship> allDeparts = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);

        // if towards dest then always follow whether interchange-only enabled or not
        List<Relationship> towardsDestination = getTowardsDestination(allDeparts);
        if (!towardsDestination.isEmpty()) {
            // we've nearly arrived
            return builders.routeStation.fromMinuteState(this, node, cost, towardsDestination, tripId);
        }

        // outbound service relationships that continue the current trip
        List<Relationship> routeStationOutbounds = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE), tripId);
        boolean tripFinishedHere = routeStationOutbounds.isEmpty(); // i.e. no outbound from RS for this tripId

        // now add outgoing to platforms
        if (interchangesOnly) {
            Iterable<Relationship> interchanges = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
            interchanges.forEach(routeStationOutbounds::add);
        } else {
            allDeparts.forEach(routeStationOutbounds::add);
        }

        if (tripFinishedHere) {
            // for a change of trip id we need to get off vehicle, then back on to another service
            return builders.routeStationEndTrip.fromMinuteState(this, cost, routeStationOutbounds);
        } else {
            return builders.routeStation.fromMinuteState(this, node, cost, routeStationOutbounds, tripId);
        }
    }

    private List<Relationship> filterByTripId(Iterable<Relationship> relationships, String tripId) {
        List<Relationship> results = new ArrayList<>();
        relationships.forEach(relationship -> {
            String trips = nodeOperations.getTrips(relationship);
            if (trips.contains(tripId)) {
                results.add(relationship);
            }
        });
        return results;
    }

    private static List<Relationship> filterBySingleTripId(NodeContentsRepository nodeOperations,
                                                           Iterable<Relationship> relationships, String tripId) {
        List<Relationship> results = new ArrayList<>();
        relationships.forEach(relationship -> {
            String trip = nodeOperations.getTrip(relationship);
            if (trip.equals(tripId)) {
                results.add(relationship);
            }
        });
        return results;
    }


}