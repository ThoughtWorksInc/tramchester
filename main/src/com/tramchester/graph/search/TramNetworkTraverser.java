package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.*;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.states.ImmuatableTraversalState;
import com.tramchester.graph.search.states.NotStartedState;
import com.tramchester.graph.search.states.TraversalState;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
    private final Set<String> endStationIds;
    private final TramchesterConfig config;
    private final ServiceReasons reasons;
    private final SortsPositions sortsPosition;

    public TramNetworkTraverser(GraphDatabase graphDatabaseService, ServiceHeuristics serviceHeuristics,
                                SortsPositions sortsPosition, NodeContentsRepository nodeContentsRepository,
                                Set<String> endStationIds, TramchesterConfig config, NodeTypeRepository nodeTypeRepository, Set<Long> destinationNodeIds) {
        this.graphDatabaseService = graphDatabaseService;
        this.serviceHeuristics = serviceHeuristics;
        this.reasons = serviceHeuristics.getReasons();
        this.sortsPosition = sortsPosition;
        this.nodeContentsRepository = nodeContentsRepository;
        this.queryTime = serviceHeuristics.getQueryTime();
        this.destinationNodeIds = destinationNodeIds;
        this.endStationIds = endStationIds;
        this.config = config;
        this.nodeTypeRepository = nodeTypeRepository;
    }

    public Stream<Path> findPaths(Transaction txn, Node startNode) {

        final TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluator(serviceHeuristics,
                destinationNodeIds, nodeTypeRepository, reasons, config );

        final NotStartedState traversalState = new NotStartedState(sortsPosition, nodeContentsRepository,
                destinationNodeIds, endStationIds, config);
        final InitialBranchState<JourneyState> initialJourneyState = JourneyState.initialState(queryTime, traversalState);

        logger.info("Create traversal");

        TraversalDescription traversalDesc =
                graphDatabaseService.traversalDescription(txn).
                relationships(TRAM_GOES_TO, Direction.OUTGOING).
                relationships(BUS_GOES_TO, Direction.OUTGOING).
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
                expand(this, initialJourneyState).
                evaluator(tramRouteEvaluator).
                uniqueness(NONE).
                order(BranchOrderingPolicies.PREORDER_BREADTH_FIRST); // TODO Breadth first hits shortest trips sooner??

        Traverser traverse = traversalDesc.traverse(startNode);
        Spliterator<Path> spliterator = traverse.spliterator();

        Stream<Path> stream = StreamSupport.stream(spliterator, false);

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() -> {
            reasons.reportReasons();
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

        Label firstLabel = endNode.getLabels().iterator().next();
        GraphBuilder.Labels nodeLabel = GraphBuilder.Labels.valueOf(firstLabel.toString());

        TraversalState traversalStateForChildren = traversalState.nextState(path, nodeLabel, endNode,
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
