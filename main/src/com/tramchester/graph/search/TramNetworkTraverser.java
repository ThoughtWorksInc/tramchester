package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.PreviousSuccessfulVisits;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.states.ImmuatableTraversalState;
import com.tramchester.graph.search.states.NotStartedState;
import com.tramchester.graph.search.states.TraversalState;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements PathExpander<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final GraphDatabase graphDatabaseService;
    private final ServiceHeuristics serviceHeuristics;
    private final NodeContentsRepository nodeContentsRepository;
    private final NodeTypeRepository nodeTypeRepository;
    private final TramTime queryTime;
    private final Set<Long> destinationNodeIds;
    private final Set<Station> endStations;
    private final TramchesterConfig config;
    private final ServiceReasons reasons;
    private final SortsPositions sortsPosition;
    private final TransportData transportData;

    public TramNetworkTraverser(GraphDatabase graphDatabaseService, TransportData transportData, ServiceHeuristics serviceHeuristics,
                                SortsPositions sortsPosition, NodeContentsRepository nodeContentsRepository,
                                Set<Station> endStations, TramchesterConfig config, NodeTypeRepository nodeTypeRepository,
                                Set<Long> destinationNodeIds, ServiceReasons reasons) {
        this.graphDatabaseService = graphDatabaseService;
        this.transportData = transportData;
        this.serviceHeuristics = serviceHeuristics;
        this.sortsPosition = sortsPosition;
        this.nodeContentsRepository = nodeContentsRepository;
        this.queryTime = serviceHeuristics.getQueryTime();
        this.destinationNodeIds = destinationNodeIds;
        this.endStations = endStations;
        this.config = config;
        this.nodeTypeRepository = nodeTypeRepository;
        this.reasons = reasons;
    }

    public Stream<Path> findPaths(Transaction txn, Node startNode, PreviousSuccessfulVisits previousSuccessfulVisit) {

        final TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluator(serviceHeuristics,
                destinationNodeIds, nodeTypeRepository, reasons, previousSuccessfulVisit, config );

        LatLong destinationLatLon = sortsPosition.midPointFrom(endStations);
        final NotStartedState traversalState = new NotStartedState(sortsPosition, nodeContentsRepository,
                destinationNodeIds, endStations, destinationLatLon, config);
        final InitialBranchState<JourneyState> initialJourneyState = JourneyState.initialState(queryTime, traversalState);

        logger.info("Create traversal");

        TraversalDescription traversalDesc =
                graphDatabaseService.traversalDescription(txn).
                relationships(TRAM_GOES_TO, Direction.OUTGOING).
                relationships(BUS_GOES_TO, Direction.OUTGOING).
                relationships(TRAIN_GOES_TO, Direction.OUTGOING).
                relationships(FERRY_GOES_TO, Direction.OUTGOING).
                relationships(SUBWAY_GOES_TO, Direction.OUTGOING).
                relationships(BOARD, Direction.OUTGOING).
                relationships(DEPART, Direction.OUTGOING).
                relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
                relationships(INTERCHANGE_DEPART, Direction.OUTGOING).
                relationships(WALKS_TO, Direction.OUTGOING).
                relationships(WALKS_FROM, Direction.OUTGOING).
                relationships(ENTER_PLATFORM, Direction.OUTGOING).
                relationships(LEAVE_PLATFORM, Direction.OUTGOING).
                relationships(TO_SERVICE, Direction.OUTGOING).
                relationships(TO_HOUR, Direction.OUTGOING).
                relationships(TO_MINUTE, Direction.OUTGOING).
                relationships(BUS_NEIGHBOUR, Direction.OUTGOING).
                relationships(TRAM_NEIGHBOUR, Direction.OUTGOING).
                relationships(TRAIN_NEIGHBOUR, Direction.OUTGOING).
                expand(this, initialJourneyState).
                evaluator(tramRouteEvaluator).
                uniqueness(NONE).
                order(BranchOrderingPolicies.PREORDER_BREADTH_FIRST); // TODO Breadth first hits shortest trips sooner??

        Traverser traverse = traversalDesc.traverse(startNode);
        Spliterator<Path> spliterator = traverse.spliterator();

        Stream<Path> stream = StreamSupport.stream(spliterator, false);

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() -> {
            reasons.reportReasons(txn, transportData);
            tramRouteEvaluator.dispose();
            traversalState.dispose();
        });

        logger.info("Return traversal stream");
        return stream.filter(path -> destinationNodeIds.contains(path.endNode().getId()));
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<JourneyState> graphState) {
        ImmutableJourneyState currentState = graphState.getState();
        ImmuatableTraversalState traversalState = currentState.getTraversalState();

        Node endNode = path.endNode();
        JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentState);

        int cost = 0;
        if (path.lastRelationship()!=null) {
            cost = nodeContentsRepository.getCost(path.lastRelationship());
            if (cost>0) {
                int total = traversalState.getTotalCost() + cost;
                journeyStateForChildren.updateJourneyClock(total);
            }
        }

        Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.from(endNode.getLabels());

        TraversalState traversalStateForChildren = traversalState.nextState(labels, endNode,
                journeyStateForChildren, cost);

        journeyStateForChildren.updateTraversalState(traversalStateForChildren);
        graphState.setState(journeyStateForChildren);

        return traversalStateForChildren.getOutbounds();
    }

    @Override
    public PathExpander<JourneyState> reverse() {
        return null;
    }


}
