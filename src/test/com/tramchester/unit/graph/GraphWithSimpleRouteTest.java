package com.tramchester.unit.graph;

import com.tramchester.DiagramCreator;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.*;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.PathToTransportRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

public class GraphWithSimpleRouteTest {

    private static final String TMP_DB = "tmp.db";

    private static TransportDataForTest transportData;
    private static RouteCalculator calculator;
    private static GraphDatabaseService graphDBService;
    private static RelationshipFactory relationshipFactory;
    private static NodeFactory nodeFactory;
    private TramServiceDate queryDate;
    private List<LocalTime> queryTimes;
    private Station firstStation;

    // TODO Use dependency init instead, this is messy
    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException, TramchesterException {
        transportData = new TransportDataForTest();

        File dbFile = new File(TMP_DB);
        FileUtils.deleteDirectory(dbFile);
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
        graphDBService = graphDatabaseFactory.newEmbeddedDatabase(dbFile);
        SpatialDatabaseService spatialDatabaseService = new SpatialDatabaseService(graphDBService);

        nodeFactory = new NodeFactory();
        relationshipFactory = new RelationshipFactory(nodeFactory);

        NodeOperations nodeOperations = new CachedNodeOperations();

        TramchesterConfig configuration = new IntegrationTramTestConfig();

        TransportGraphBuilder builder = new TransportGraphBuilder(graphDBService, transportData, relationshipFactory,
                spatialDatabaseService, configuration);
        builder.buildGraph();

        RouteCodeToClassMapper routeIdToClass = new RouteCodeToClassMapper();
        PathToTransportRelationship pathToRelationships  = new PathToTransportRelationship(relationshipFactory);
        MapTransportRelationshipsToStages relationshipsToStages = new MapTransportRelationshipsToStages(routeIdToClass, transportData, transportData);

        CostEvaluator<Double> costEvaluator = new CachingCostEvaluator();

        MapPathToStages mapper = new MapPathToStages(pathToRelationships, relationshipsToStages);
        calculator = new RouteCalculator(graphDBService, transportData, nodeFactory, relationshipFactory,
                spatialDatabaseService, nodeOperations, mapper, costEvaluator, configuration);
    }

    @Before
    public void beforeEachTestRuns() {
        queryDate = new TramServiceDate("20140630");
        // note: trams only run at specific times so still only getPlatformById one journey in results
        //queryTimes = Arrays.asList(new Integer[]{minutesPastMidnight, minutesPastMidnight+6});
        firstStation = transportData.getStation(TransportDataForTest.FIRST_STATION).get();
        queryTimes = Collections.singletonList(LocalTime.of(7, 57));
    }

    @Test
    public void shouldTestSimpleJourneyIsPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.SECOND_STATION, queryTimes, queryDate);
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.SECOND_STATION);
    }

    @Test
    public void shouldTestSimpleJourneyIsPossibleToInterchange() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.INTERCHANGE, queryTimes, queryDate);
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.INTERCHANGE);
    }

    @Test
    public void shouldTestSimpleJourneyIsNotPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.INTERCHANGE, Collections.singletonList(LocalTime.of(9, 0)), queryDate);
        assertEquals(0, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitIsPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.LAST_STATION, queryTimes, queryDate);
        assertEquals(1, journeys.size());
        assertFirstAndLast(journeys, TransportDataForTest.FIRST_STATION, TransportDataForTest.LAST_STATION);
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.STATION_FOUR, queryTimes, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestJourneyWithLocationBasedStart() {
        LatLong origin = new LatLong(180.001, 270.001);
        int walkCost = 1;
        List<StationWalk> stationWalks = Collections.singletonList(new StationWalk(firstStation, walkCost));

        Station endStation = transportData.getStation(TransportDataForTest.SECOND_STATION).get();
        Set<RawJourney> journeys = calculator.calculateRoute(origin, stationWalks, endStation, queryTimes, queryDate);

        assertEquals(1, journeys.size());
    }

    @Test
    public void createDiagramOfTestNetwork() throws IOException {
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphDBService, Integer.MAX_VALUE);
        creator.create("test_network.dot", TransportDataForTest.FIRST_STATION);
    }

    @Ignore("WIP")
    @Test
    public void shouldFindJourneyAreaToArea() {
        AreaDTO areaA = new AreaDTO("areaA");
        AreaDTO areaB = new AreaDTO("areaB");

        Set<RawJourney> journeys =  calculator.calculateRoute(areaA, areaB, queryTimes, queryDate);
        assertEquals(1, journeys.size());

    }

    private void assertFirstAndLast(Set<RawJourney> journeys, String firstStation, String secondStation) {
        RawJourney journey = (RawJourney)journeys.toArray()[0];
        List<RawStage> stages = journey.getStages();
        RawVehicleStage vehicleStage = (RawVehicleStage) stages.get(0);
        assertEquals(firstStation,vehicleStage.getFirstStation().getId());
        assertEquals(secondStation,vehicleStage.getLastStation().getId());
    }
}
