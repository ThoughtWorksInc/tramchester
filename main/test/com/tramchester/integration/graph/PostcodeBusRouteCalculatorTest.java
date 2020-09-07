package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class PostcodeBusRouteCalculatorTest {
    // TODO this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static Dependencies dependencies;
    private static GraphDatabase database;
    private static TramchesterConfig testConfig;

    private final LocalDate day = TestEnv.testDay();
    private Transaction txn;
    private final TramTime time = TramTime.of(9,11);
    private LocationJourneyPlanner planner;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        testConfig = new BusWithPostcodesEnabled();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTx(TXN_TIMEOUT, TimeUnit.SECONDS);
        planner = dependencies.get(LocationJourneyPlanner.class);
        stationRepository = dependencies.get(StationRepository.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBus() {
        Set<Journey> journeys = planner.quickestRouteForLocation(txn, Postcodes.CentralBury.getLatLong(),
                Postcodes.NearPiccadily.getLatLong(), createRequest(3)).collect(Collectors.toSet());

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> assertEquals(3,journey.getStages().size()));
    }

    @NotNull
    private JourneyRequest createRequest(int maxChanges) {
        return new JourneyRequest(new TramServiceDate(day), time, false, maxChanges, testConfig.getMaxJourneyDuration());
    }

    @Test
    void shouldWalkFromPostcodeToNearbyStation() {

        JourneyRequest request = createRequest(3);
        Set<Journey> journeys = getJourneys(Postcodes.CentralBury, BuryInterchange, request);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(BusStations.BuryInterchange.getId(), getLastStage(journey).getLastStation().getId());
        });
    }


    private TransportStage getLastStage(Journey journey) {
        return journey.getStages().get(journey.getStages().size()-1);
    }

    @Test
    void shouldWalkFromBusStationToNearbyPostcode() {
        checkNearby(BuryInterchange, Postcodes.CentralBury);
        checkNearby(ShudehillInterchange, Postcodes.NearShudehill);
    }

    @Test
    void shouldWalkFromPostcodeToBusStationNearby() {
        checkNearby(Postcodes.CentralBury, BuryInterchange);
        checkNearby(Postcodes.NearShudehill, ShudehillInterchange);
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeSouthbound() {
        Set<Journey> journeys = getJourneys(BuryInterchange, Postcodes.NearShudehill, createRequest(3));

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(BuryInterchange.getId(), journey.getStages().get(0).getFirstStation().getId());
            assertEquals(Postcodes.NearShudehill.getLatLong(), getLastStage(journey).getLastStation().getLatLong());
        });

    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {
        Set<Journey> journeys = getJourneys(Postcodes.CentralBury, ShudehillInterchange, createRequest(5));

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(BusStations.ShudehillInterchange.getId(), getLastStage(journey).getLastStation().getId());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStationCentral() {
        JourneyRequest journeyRequest = createRequest(3);
        Set<Journey> journeys = getJourneys(Postcodes.NearPiccadily, BusStations.ShudehillInterchange, journeyRequest);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(BusStations.ShudehillInterchange.getId(), getLastStage(journey).getLastStation().getId());
        });
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeCentral() {
        JourneyRequest journeyRequest = createRequest(3);
        Set<Journey> journeys = getJourneys(BusStations.ShudehillInterchange, Postcodes.NearPiccadily, journeyRequest);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(TransportMode.Bus, journey.getStages().get(0).getMode());
            assertEquals(Postcodes.NearPiccadily.getId().forDTO(), getLastStage(journey).getLastStation().getId());
        });
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcodeNorthbound() {
        Set<Journey> journeys = getJourneys(BusStations.ShudehillInterchange, Postcodes.CentralBury, createRequest(5));

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(2, journey.getStages().size());
            assertEquals(TransportMode.Bus, journey.getStages().get(0).getMode());
            assertEquals(Postcodes.CentralBury.getId(), getLastStage(journey).getLastStation().getId());
        });
    }

    @NotNull
    private Set<Journey> getJourneys(BusStations start, PostcodeLocation end, JourneyRequest request) {
        return planner.
                quickestRouteForLocation(txn, TestStation.real(stationRepository, start), end.getLatLong(), request).
                collect(Collectors.toSet());
    }

    private Set<Journey> getJourneys(PostcodeLocation start, BusStations end, JourneyRequest request) {
        return planner.
                quickestRouteForLocation(txn, start.getLatLong(), TestStation.real(stationRepository,end), request).
                collect(Collectors.toSet());
    }

    private void checkNearby(PostcodeLocation start, BusStations end) {
        JourneyRequest request = createRequest(3);
        Set<Journey> journeys = getJourneys(start, end, request);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            TransportStage transportStage = journey.getStages().get(0);
            assertEquals(TransportMode.Walk, transportStage.getMode());
            assertEquals(start.getLatLong(), transportStage.getFirstStation().getLatLong());
            assertEquals(end.getId(), transportStage.getLastStation().getId());
        });
    }

    private void checkNearby(BusStations start, PostcodeLocation end) {
        JourneyRequest request = createRequest(3);
        Set<Journey> journeys = getJourneys(start, end, request);

        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size());
            assertEquals(TransportMode.Walk, journey.getStages().get(0).getMode());
            assertEquals(start.getId(), journey.getStages().get(0).getFirstStation().getId());
        });
    }

}