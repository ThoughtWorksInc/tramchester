package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.integration.testSupport.TramWithPostcodesEnabled;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.*;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PostcodeTramJourneyPlannerTest {

    // TODO WIP

    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private static final LocalDate when = TestEnv.testDay();
    private static TramWithPostcodesEnabled testConfig;
    private Transaction txn;
    private LocationJourneyPlannerTestFacade planner;
    private PostcodeRepository repository;
    private PostcodeLocation centralLocation;
    private static final TramTime planningTime = TramTime.of(11, 42);
    private int maxStages = 9;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new TramWithPostcodesEnabled();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        planner =  new LocationJourneyPlannerTestFacade(componentContainer.get(LocationJourneyPlanner.class), stationRepository, txn);
        repository = componentContainer.get(PostcodeRepository.class);
        centralLocation = repository.getPostcode(TestPostcodes.NearPiccadillyGardens.getId());
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    private static Stream<JourneyRequest> getRequest() {
        int maxJourneyDuration = testConfig.getMaxJourneyDuration();
        TramServiceDate date = new TramServiceDate(when);
        int maxChanges = 8;
        return Stream.of(
                new JourneyRequest(date, planningTime, false, maxChanges, maxJourneyDuration),
                new JourneyRequest(date, planningTime, true, maxChanges, maxJourneyDuration));
    }

    @ParameterizedTest
    @MethodSource("getRequest")
    void shouldHaveJourneyFromCentralPostcodeToBury(JourneyRequest request) {
        Set<Journey> journeySet =  planner.quickestRouteForLocation(centralLocation.getLatLong(), TramStations.Bury, request, maxStages);

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
        checkDepartBefore(journeySet, request.getArriveBy());
    }

    @ParameterizedTest
    @MethodSource("getRequest")
    void shouldHaveJourneyFromBuryToCentralPostcode(JourneyRequest request) {
        Set<Journey> journeySet =  planner.quickestRouteForLocation(TramStations.Bury, centralLocation.getLatLong(), request, maxStages);

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertEquals(TransportMode.Tram, journey.getStages().get(0).getMode()));
        checkDepartBefore(journeySet, request.getArriveBy());

    }

    @ParameterizedTest
    @MethodSource("getRequest")
    void shouldHavePostcodeToPostcodeJourney(JourneyRequest request) {
        PostcodeLocation buryPostcode = repository.getPostcode(TestPostcodes.CentralBury.getId());
        Set<Journey> journeySet = planner.quickestRouteForLocation(centralLocation.getLatLong(),
                buryPostcode.getLatLong(), request, maxStages);

        assertFalse(journeySet.isEmpty());
        journeySet.forEach(journey -> assertTrue(journey.getStages().size()>=3));
        // walk at start
        journeySet.forEach(journey -> assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode()));
        // walk at end
        journeySet.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            int lastIndex = stages.size()-1;
            assertEquals(TransportMode.Walk, stages.get(lastIndex).getMode());
        });
        // trams in the middle
        journeySet.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            for (int i = 1; i < stages.size()-1; i++) {
                assertEquals(TransportMode.Tram, stages.get(i).getMode());
            }
        });
        checkDepartBefore(journeySet, request.getArriveBy());
    }

    private void checkDepartBefore(Set<Journey> journeySet, boolean arriveBy) {
        if (arriveBy) {
            journeySet.forEach(journey -> journey.getStages().get(0).getFirstDepartureTime().isBefore(planningTime));
        }
    }
}
