package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TrainDataLoadSpike {

    private static Dependencies dependencies;
    private TransportData data;

    @BeforeAll
    static void beforeClass() throws IOException {
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();

        dependencies = new Dependencies();
        dependencies.initialise(testConfig);
    }

    @AfterAll
    static void afterClass() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        data = dependencies.get(TransportData.class);
    }

    @Test
    void shouldSenseCheckData() {
        assertFalse(data.getStations().isEmpty());
        assertFalse(data.getServices().isEmpty());
        assertFalse(data.getRoutes().isEmpty());
        assertFalse(data.getAgencies().isEmpty());
        assertFalse(data.getTrips().isEmpty());

        assertEquals(0, data.getStations().stream().filter(TransportMode::isTram).count());
        assertEquals(0, data.getStations().stream().filter(TransportMode::isBus).count());

        // TODO some staions in train data have 0,0 positions
//        data.getStations().forEach(station -> {
//            LatLong latlong = station.getLatLong();
//            assertNotEquals(0, latlong.getLat(), station.getId());
//            assertNotEquals(0, latlong.getLon(), station.getId());
//        });

    }
}
