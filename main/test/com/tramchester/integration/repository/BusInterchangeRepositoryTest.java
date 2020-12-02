package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusInterchangeRepositoryTest {
    private static Dependencies dependencies;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = dependencies.get(InterchangeRepository.class);
    }

    @Test
    void shouldFindTramInterchanges() {
        for (IdFor<Station> interchange : TramInterchanges.stations()) {
            assertTrue(repository.isInterchange(interchange));
        }
    }

    @BusTest
    @Test
    void shouldFindBusInterchanges() {

        Collection<Station> interchanges = repository.getBusInterchanges();

        for (Station interchange : interchanges) {
            assertFalse(TransportMode.isTram(interchange));
        }

        assertFalse(interchanges.isEmpty());
        IdSet<Station> interchangeIds = interchanges.stream().collect(IdSet.collector());
        assertTrue(interchangeIds.contains(BusStations.AltrinchamInterchange.getId()));
    }

//    @BusTest
//    @Test
//    void shouldFindSharedStationsUsedByMultipleAgencies() {
//
//        Set<Station> shared = repository.getBusMultiAgencyStations();
//
//        assertFalse(shared.isEmpty());
//
//        assertTrue(shared.contains(BusStations.AltrinchamInterchange));
//        assertTrue(shared.contains(BusStations.StockportBusStation));
//        assertTrue(shared.contains(BusStations.ShudehillInterchange));
//
//        StationRepository stationRepos = dependencies.get(StationRepository.class);
//        assertNotEquals(stationRepos.getStations().size(), shared.size());
//    }

}
