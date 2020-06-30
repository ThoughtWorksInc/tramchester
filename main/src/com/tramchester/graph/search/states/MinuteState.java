package com.tramchester.graph.search.states;

import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.search.JourneyState;
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

    private final String tripId;

    private MinuteState(TraversalState parent, Iterable<Relationship> relationships, String tripId, int cost) {
        super(parent, relationships, cost);
        this.tripId = tripId;
    }

    public static TraversalState fromHourOnTrip(HourState hourState, String tripId, Node node, int cost) {
        Iterable<Relationship> relationships = filterBySingleTripId(hourState.nodeOperations,
                node.getRelationships(OUTGOING, TRAM_GOES_TO, BUS_GOES_TO),
                tripId);
        return new MinuteState(hourState, relationships, tripId, cost);
    }

    public static TraversalState fromHour(HourState hourState, Node node, int cost) {
        String newTripId = getTrip(node);
        Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TRAM_GOES_TO, BUS_GOES_TO);
        return new MinuteState(hourState, relationships, newTripId, cost);
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "tripId='" + tripId + '\'' +
                ", parent=" + parent +
                '}';
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

        // towards final destination, just follow this one
        for (Relationship depart : allDeparts) {
            if (destinationStationIds.contains(depart.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                // we've arrived
                return new RouteStationState(this, Collections.singleton(depart), node.getId(), tripId, cost);
            }
        }

        List<Relationship> routeStationOutbound = filterByTripId(node.getRelationships(OUTGOING, TO_SERVICE), tripId);
        boolean tripFinishedHere = routeStationOutbound.isEmpty();

        // add outgoing to platforms
        if (config.getChangeAtInterchangeOnly()) {
            Iterable<Relationship> interchanges = node.getRelationships(OUTGOING, INTERCHANGE_DEPART);
            interchanges.forEach(routeStationOutbound::add);
        } else {
            allDeparts.forEach(routeStationOutbound::add);
        }

        if (tripFinishedHere) {
            // service finished here so don't pass in trip ID
            return new RouteStationState(this, routeStationOutbound, node.getId(), cost, false);
        } else {
            return new RouteStationState(this, routeStationOutbound, node.getId(), tripId, cost);
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
