package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Stage;
import com.tramchester.services.DateTimeService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class RouteCalculatorTest {

    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private DateTimeService dateTimeService;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        dateTimeService = dependencies.get(DateTimeService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void testJourneyFromAltyToAirport() throws Exception {
        int minutes = dateTimeService.getMinutesFromMidnight("11:43:00");
        Set<Journey> results = calculator.calculateRoute(Stations.Altrincham, Stations.ManAirport, minutes, DaysOfWeek.Sunday);

        assertEquals(1, results.size());    // results is iterator
        for (Journey result : results) {
            List<Stage> stages = result.getStages();
            assertEquals(2, stages.size());
            Stage firstStage = stages.get(0);
            assertEquals(Stations.Altrincham, firstStage.getFirstStation());
            assertEquals(Stations.Cornbrook, firstStage.getLastStation());
            Stage secondStage = stages.get(1);
            assertEquals(Stations.Cornbrook, secondStage.getFirstStation());
            assertEquals(Stations.ManAirport, secondStage.getLastStation());
        }
    }

}