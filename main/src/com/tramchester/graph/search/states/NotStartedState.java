package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;

import java.util.Set;

public class NotStartedState extends TraversalState {

    public NotStartedState(SortsPositions sortsPositions, NodeContentsRepository nodeOperations, Set<Long> destinationNodeIds,
                           Set<Station> destinationStation,
                           LatLong destinationLatLonHint, TramchesterConfig config) {
        super(sortsPositions, nodeOperations, destinationNodeIds, destinationStation, destinationLatLonHint, config);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    @Override
    public int getTotalCost() {
        return 0;
    }

    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node firstNode, JourneyState journeyState, int cost) {
        // should only be called for multi-mode stations
        return builders.noPlatformStation.from(this, firstNode, cost);
    }

    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node firstNode, JourneyState journeyState, int cost) {
        switch(nodeLabel) {
            case QUERY_NODE:
                return builders.walking.fromStart(this, firstNode, cost);
            case TRAM_STATION:
                return builders.tramStation.fromStart(this, firstNode, cost);
            case BUS_STATION:
            case TRAIN_STATION:
            case FERRY_STATION:
            case SUBWAY_STATION:
                return builders.noPlatformStation.from(this, firstNode, cost);
        }
        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }

}
