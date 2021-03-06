package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.KnownTramRoute;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.domain.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.RoutesForTesting.createTramRoute;
import static com.tramchester.testSupport.TestEnv.assertIdEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteCallingStationsTest {

    private static ComponentContainer componentContainer;
    private static RouteCallingStations repo;
    private static TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        repo = componentContainer.get(RouteCallingStations.class);
        transportData = componentContainer.get(TransportData.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveVictoriaToAirportCorrectStopsCityZone() {
        List<Station> stations = getStationsFor(VictoriaWythenshaweManchesterAirport);
        assertIdEquals(Victoria, stations.get(0));
        assertIdEquals(Shudehill, stations.get(1));
        assertIdEquals(MarketStreet, stations.get(2));
        assertIdEquals(StWerburghsRoad, stations.get(9));
    }

    @Test
    void shouldGetCorrectStationsForARouteAltToPicc() {
        List<Station> stations = getStationsFor(AltrinchamPiccadilly);
        assertIdEquals(Altrincham, stations.get(0));
        assertIdEquals(NavigationRoad, stations.get(1));
        assertIdEquals(Cornbrook, stations.get(9));
        assertIdEquals(Piccadilly, stations.get(13));
    }

    @Test
    void shouldGetCorrectStationsForARouteEcclesToAsh() {
        List<Station> stations = getStationsFor(EcclesManchesterAshtonUnderLyne);
        assertIdEquals(Eccles, stations.get(0));
        assertIdEquals(MediaCityUK, stations.get(5));
        assertIdEquals(Deansgate, stations.get(12));
        assertIdEquals(Ashton, stations.get(27));
    }

    @Test
    void shouldGetCorrectStationsForARouteAshToEccles() {
        List<Station> stations = getStationsFor(AshtonUnderLyneManchesterEccles);
        assertIdEquals(Ashton, stations.get(0));
        assertIdEquals(Eccles, stations.get(27));
        assertIdEquals(MediaCityUK, stations.get(22));
        assertIdEquals(Piccadilly, stations.get(12));
    }

    @Test
    void shouldHaveEndsOfLines() {
        IdSet<Station> foundEndOfLines = new IdSet<>();
        transportData.getRoutes().forEach(route -> {
            List<Station> stations = repo.getStationsFor(route);
            foundEndOfLines.add(stations.get(0).getId());
        });
        assertEquals(11, foundEndOfLines.size());
    }

    @Test
    void shouldGetCorrectNumberOfStationsForRoutes() {
        assertEquals(14, getStationsFor(AltrinchamPiccadilly).size());
        assertEquals(14, getStationsFor(PiccadillyAltrincham).size());

        // not 27, some journeys skip mediacity UK
        assertEquals(28, getStationsFor(AshtonUnderLyneManchesterEccles).size());
        assertEquals(28, getStationsFor(EcclesManchesterAshtonUnderLyne).size());

        assertEquals(33, getStationsFor(EastDidisburyManchesterShawandCromptonRochdale).size());
        assertEquals(33, getStationsFor(RochdaleShawandCromptonManchesterEastDidisbury).size());

        assertEquals(25, getStationsFor(ManchesterAirportWythenshaweVictoria).size());
        assertEquals(25, getStationsFor(VictoriaWythenshaweManchesterAirport).size());

        assertEquals(8, getStationsFor(TheTraffordCentreCornbrook).size());
        assertEquals(8, getStationsFor(CornbrookTheTraffordCentre).size());

        assertEquals(15, getStationsFor(PiccadillyBury).size());
        assertEquals(15, getStationsFor(BuryPiccadilly).size());

    }

    private List<Station> getStationsFor(KnownTramRoute knownRoute) {
        return repo.getStationsFor(createTramRoute(knownRoute));
    }

}
