package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.liveUpdates.LineAndDirection;
import com.tramchester.domain.liveUpdates.LineDirection;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.CentralZoneStations;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.mappers.RouteToLineMapper;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TramStations.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RouteToLineMapperTest {
    private static Dependencies dependencies;
    private StationRepository repository;
    private RouteToLineMapper mapper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        dependencies.close();
    }

    @BeforeEach
    void beforeAnyTestsRun() {
        repository = dependencies.get(TransportData.class);
        mapper = dependencies.get(RouteToLineMapper.class);
    }

    @Test
    void shouldMapEcclesLineStation() {
        IdFor<RouteStation> routeStationId = RouteStation.formId(of(TramStations.SalfordQuay), RoutesForTesting.ASH_TO_ECCLES);
        RouteStation routeStation = repository.getRouteStationById(routeStationId);
        LineAndDirection result = mapper.map(routeStation);

        assertEquals(Lines.Eccles, result.getLine());
        assertEquals(LineDirection.Outgoing, result.getDirection());
    }

    @Test
    void shouldMapAirportLineStation() {
        IdFor<RouteStation> routeStationIdA = RouteStation.formId(of(TramStations.PeelHall), RoutesForTesting.VIC_TO_AIR);
        RouteStation routeStationA = repository.getRouteStationById(routeStationIdA);
        LineAndDirection resultA = mapper.map(routeStationA);

        assertEquals(Lines.Airport, resultA.getLine());
        assertEquals(LineDirection.Outgoing, resultA.getDirection());

        IdFor<RouteStation> routeStationIdB = RouteStation.formId(of(TramStations.PeelHall), RoutesForTesting.AIR_TO_VIC);
        RouteStation routeStationB = repository.getRouteStationById(routeStationIdB);
        LineAndDirection resultB = mapper.map(routeStationB);

        assertEquals(Lines.Airport, resultB.getLine());
        assertEquals(LineDirection.Incoming, resultB.getDirection());
    }

    @Test
    void shouldMapCentralStationsAsExpected() {
        IdSet<Station> centralStationIds = Arrays.stream(CentralZoneStations.values()).
                map(CentralZoneStations::getId).collect(IdSet.idCollector());

        Set<RouteStation> centralRouteStations = repository.getRouteStations().stream().
                filter(routeStation -> centralStationIds.contains(routeStation.getStationId())).
                collect(Collectors.toSet());

        centralRouteStations.forEach(routeStation -> {
            LineAndDirection result = mapper.map(routeStation);
            assertEquals(CentralZoneStations.map.get(routeStation.getStationId()).getLine(), result.getLine());
        });
    }

    @Test
    void shouldMapEachRouteStationToALine() {
        Set<RouteStation> routeStations = repository.getRouteStations();

        for (RouteStation routeStation : routeStations) {
            LineAndDirection lineAndDirection = mapper.map(routeStation);
            assertNotEquals(LineAndDirection.Unknown, lineAndDirection, routeStation.toString());
        }
    }

}