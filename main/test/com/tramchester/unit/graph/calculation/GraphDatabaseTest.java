package com.tramchester.unit.graph.calculation;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDatabaseTest {

    private static SimpleGraphConfig config;

    @BeforeEach
    void beforeEachTest() throws IOException {
        config = new SimpleGraphConfig();
        TestEnv.deleteDBIfPresent(config);
    }

    @AfterEach
    void afterEachTest() throws IOException {
        TestEnv.deleteDBIfPresent(config);
    }

    @Test
    void shouldStartDatabase() {
        ProvidesLocalNow providesNow = new ProvidesLocalNow();
        DataSourceRepository transportData = new TramTransportDataForTestFactory.TramTransportDataForTest(providesNow);
        GraphDatabase graphDatabase = new GraphDatabase(config, transportData);

        graphDatabase.start();
        assertTrue(graphDatabase.isAvailable(5000));
        graphDatabase.stop();
    }

    private static class SimpleGraphConfig extends IntegrationTestConfig {

        public SimpleGraphConfig() {
            super(new GraphDBTestConfig("unitTest", "graphDatabaseTest.db"));
        }

        @Override
        protected List<DataSourceConfig> getDataSourceFORTESTING() {
            TFGMTestDataSourceConfig tfgmTestDataSourceConfig = new TFGMTestDataSourceConfig("data/tram",
                    GTFSTransportationType.tram, TransportMode.Tram);
            return Collections.singletonList(tfgmTestDataSourceConfig);
        }

        @Override
        public int getNumberQueries() { return 1; }

        @Override
        public int getQueryInterval() {
            return 6;
        }
    }
}
