package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import static com.tramchester.testSupport.reference.BusStations.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusRouteCostCalculatorTest {
    private static ComponentContainer componentContainer;

    private Transaction txn;
    private RouteCostCalculator routeCost;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeCost = componentContainer.get(RouteCostCalculator.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        Assertions.assertEquals(42, getApproxCostBetween(AltrinchamInterchange, StockportBusStation));
        Assertions.assertEquals(41, getApproxCostBetween(StockportBusStation, AltrinchamInterchange));
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        Assertions.assertEquals(53, getApproxCostBetween(ShudehillInterchange, AltrinchamInterchange));
        Assertions.assertEquals(60, getApproxCostBetween(AltrinchamInterchange, ShudehillInterchange));
    }

    @BusTest
    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        Assertions.assertEquals(43, getApproxCostBetween(ShudehillInterchange, StockportBusStation));
        Assertions.assertEquals(42, getApproxCostBetween(StockportBusStation, ShudehillInterchange));
    }

    private int getApproxCostBetween(BusStations start, BusStations end) {
        return routeCost.getApproxCostBetween(txn,
                TestStation.real(stationRepository,start), TestStation.real(stationRepository,end));
    }

}
