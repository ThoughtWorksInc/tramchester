package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.*;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteCalculator implements TramRouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private static final int BUSES_MAX_PATH_LENGTH = 1000;
    private static final int TRAMS_MAX_PATH_LENGTH = 400;

    private final MapPathToStages pathToStages;
    private final TramchesterConfig config;
    private final NodeContentsRepository nodeOperations;
    private final NodeTypeRepository nodeTypeRepository;

    private final TransportData transportData;
    private final TramReachabilityRepository tramReachabilityRepository;
    private final CreateQueryTimes createQueryTimes;
    private final GraphDatabase graphDatabaseService;
    private final ProvidesLocalNow providesLocalNow;
    private final GraphQuery graphQuery;
    private final int maxPathLength;
    private final SortsPositions sortsPosition;

    public RouteCalculator(TransportData transportData, NodeContentsRepository nodeOperations, MapPathToStages pathToStages,
                           TramchesterConfig config, TramReachabilityRepository tramReachabilityRepository,
                           CreateQueryTimes createQueryTimes, GraphDatabase graphDatabaseService,
                           ProvidesLocalNow providesLocalNow, GraphQuery graphQuery, NodeTypeRepository nodeTypeRepository,
                           SortsPositions sortsPosition) {
        this.transportData = transportData;
        this.nodeOperations = nodeOperations;
        this.pathToStages = pathToStages;
        this.config = config;
        this.tramReachabilityRepository = tramReachabilityRepository;
        this.createQueryTimes = createQueryTimes;
        this.graphDatabaseService = graphDatabaseService;
        this.providesLocalNow = providesLocalNow;
        this.graphQuery = graphQuery;
        this.nodeTypeRepository = nodeTypeRepository;

        maxPathLength = config.getBus() ? BUSES_MAX_PATH_LENGTH : TRAMS_MAX_PATH_LENGTH;
        this.sortsPosition = sortsPosition;
    }

    @Override
    public Stream<Journey> calculateRoute(Transaction txn, Station startStation, Station destination, JourneyRequest journeyRequest) {
        logger.info(format("Finding shortest path for %s --> %s for %s", startStation, destination, journeyRequest));

        Node startNode = getStationNodeSafe(txn, startStation);
        Node endNode = getStationNodeSafe(txn, destination);

        Set<Station> destinations = Collections.singleton(destination);

        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinations, false);
    }

    private Node getStationNodeSafe(Transaction txn, Station station) {
        Node stationNode = graphQuery.getStationNode(txn, station);
        if (stationNode==null) {
            throw new RuntimeException("Unable to find station node based on " + station);
        }
        return stationNode;
    }

    public Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Station start, Node endOfWalk, Set<Station> desinationStations,
                                                   JourneyRequest journeyRequest)
    {
        Node startNode = getStationNodeSafe(txn, start);
        return getJourneyStream(txn, startNode, endOfWalk, journeyRequest, desinationStations, false);
    }

    @Override
    public Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Node startOfWalkNode, Station destination,
                                                     JourneyRequest journeyRequest) {
        Node endNode = getStationNodeSafe(txn, destination);
        Set<Station> destinations = Collections.singleton(destination);
        return getJourneyStream(txn, startOfWalkNode, endNode, journeyRequest, destinations, true);
    }

    public Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Node startNode, Node endNode,
                                                           Set<Station> destinationStations, JourneyRequest journeyRequest) {
        return getJourneyStream(txn, startNode, endNode, journeyRequest, destinationStations, true);
    }

    private Stream<Journey> getJourneyStream(Transaction txn, Node startNode, Node endNode, JourneyRequest journeyRequest,
                                             Set<Station> destinations, boolean walkAtStart) {

        RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(journeyRequest.getDate()));

        logger.info("Found " + runningServicesIds.count() + " running services for " +journeyRequest.getDate());

        List<TramTime> queryTimes = createQueryTimes.generate(journeyRequest.getTime(), walkAtStart);
        Set<Long> destinationNodeIds = Collections.singleton(endNode.getId());
        Set<String> endStationIds = destinations.stream().map(Station::getId).collect(Collectors.toSet());

        // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
        PreviousSuccessfulVisits previousSuccessfulVisit = new PreviousSuccessfulVisits(nodeTypeRepository);

        return queryTimes.stream().
                map(queryTime -> createHeuristics(journeyRequest, runningServicesIds, queryTime, destinations)).
                flatMap(serviceHeuristics -> {
                    return findShortestPath(txn, startNode, destinationNodeIds, serviceHeuristics,
                            endStationIds, previousSuccessfulVisit, new ServiceReasons(journeyRequest, serviceHeuristics.getQueryTime(), providesLocalNow));
                }).
                map(path -> new Journey(pathToStages.mapDirect(path.getPath(), path.getQueryTime(), journeyRequest), path.getQueryTime()));
    }

    public Stream<JourneysForBox> calculateRoutes(Set<Station> destinations, JourneyRequest journeyRequest,
                                                  List<BoundingBoxWithStations> grouped) {
        logger.info("Finding routes for bounding boxes");

        final RunningServices runningServicesIds = new RunningServices(transportData.getServicesOnDate(journeyRequest.getDate()));
        final TramTime time = journeyRequest.getTime();

        Set<Long> destinationNodeIds;
        try(Transaction txn = graphDatabaseService.beginTx()) {
           destinationNodeIds = destinations.stream().
                    map(station -> getStationNodeSafe(txn, station)).
                    map(Entity::getId).
                    collect(Collectors.toSet());
        }
        Set<String> endStationIds = destinations.stream().map(Station::getId).collect(Collectors.toSet());

        final ServiceHeuristics serviceHeuristics = createHeuristics(journeyRequest, runningServicesIds, time, destinations);

        return grouped.stream().map(box -> {
            // can only be shared as same date and same set of destinations, will eliminate previously seen paths/results
            PreviousSuccessfulVisits previousSuccessfulVisit = new PreviousSuccessfulVisits(nodeTypeRepository);

            logger.info(format("Finding shortest path for %s --> %s for %s", box, destinations, journeyRequest));
            Set<Station> startingStations = box.getStaions();

            try(Transaction txn = graphDatabaseService.beginTx()) {
                Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> getStationNodeSafe(txn, start)).
                        flatMap(startNode -> {
                            ServiceReasons reasons = new ServiceReasons(journeyRequest, time, providesLocalNow);
                            return findShortestPath(txn, startNode, destinationNodeIds, serviceHeuristics, endStationIds, previousSuccessfulVisit, reasons);
                        }).
                        map(path -> new Journey(pathToStages.mapDirect(path.getPath(), path.getQueryTime(), journeyRequest), path.getQueryTime()));

                List<Journey> collect = journeys.collect(Collectors.toList());

                // yielding
                return new JourneysForBox(box, collect);
            }
        });

    }

    private Stream<TimedPath> findShortestPath(Transaction txn, final Node startNode, Set<Long> destinationNodeIds,
                                               final ServiceHeuristics serviceHeuristics,
                                               final Set<String> endStationIds, PreviousSuccessfulVisits previousSuccessfulVisit, ServiceReasons reasons) {

        TramNetworkTraverser tramNetworkTraverser = new TramNetworkTraverser(graphDatabaseService, serviceHeuristics,
                sortsPosition, nodeOperations, endStationIds, config, nodeTypeRepository, destinationNodeIds, reasons);

        return tramNetworkTraverser.findPaths(txn, startNode, previousSuccessfulVisit).map(path -> new TimedPath(path, serviceHeuristics.getQueryTime()));
    }

    @NotNull
    private ServiceHeuristics createHeuristics(JourneyRequest journeyRequest, RunningServices runningServicesIds,
                                               TramTime time, Set<Station> destinations) {
        return new ServiceHeuristics(transportData, nodeOperations, tramReachabilityRepository, config,
                time, runningServicesIds, destinations,
                maxPathLength, journeyRequest);
    }

    private static class TimedPath {
        private final Path path;
        private final TramTime queryTime;

        public TimedPath(Path path, TramTime queryTime) {
            this.path = path;
            this.queryTime = queryTime;
        }

        public Path getPath() {
            return path;
        }

        public TramTime getQueryTime() {
            return queryTime;
        }
    }
}

