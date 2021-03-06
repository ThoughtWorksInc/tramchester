package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.List;

import static com.tramchester.testSupport.reference.TrainStations.Knutsford;
import static com.tramchester.testSupport.reference.TrainStations.Mobberley;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TrainRouteReachableTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private RouteRepository routeRepo;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepo = componentContainer.get(RouteRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        reachable = componentContainer.get(RouteReachable.class);
    }

    @Test
    void shouldHaveCorrectReachabilityOrInterchanges() {
        Route route = routeRepo.getRouteById(StringIdFor.createId("7685")); // NT:MAN->CTR

        IdSet<Station> reachable = reachable(route, Mobberley, Knutsford);
        assertEquals(1, reachable.size());
        assertTrue(reachable.contains(Knutsford.getId()));

        assertTrue(reachable(route, Knutsford, Mobberley).isEmpty());
    }

    @Test
    void shouldHaveAdjacentRoutesCorrectly() {
        assertEquals(2, getRoutes(Mobberley, Knutsford).size()); // stock to chester, man to chester
        assertEquals(1, getRoutes(Knutsford, Mobberley).size()); // chester to manchester
    }

    private List<Route> getRoutes(TrainStations start, TrainStations neighbour) {
        return reachable.getRoutesFromStartToNeighbour(getReal(start), getReal(neighbour));
    }

    private Station getReal(TrainStations stations) {
        return TrainStations.real(stationRepository, stations);
    }

    private IdSet<Station> reachable(Route route, TrainStations start, TrainStations dest) {
        return reachable.getRouteReachableWithInterchange(createRouteStation(route, getReal(start)), IdSet.singleton(dest.getId()));
    }

    private RouteStation createRouteStation(Route route, Station station) {
        return new RouteStation(station, route);
    }
}
