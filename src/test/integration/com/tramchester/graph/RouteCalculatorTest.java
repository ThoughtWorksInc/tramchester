package com.tramchester.graph;

import com.tramchester.*;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.services.DateTimeService;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.util.*;

import static org.junit.Assert.*;

public class RouteCalculatorTest {

    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private DateTimeService dateTimeService;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
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
        TramServiceDate today = new TramServiceDate(LocalDate.now());

        Set<RawJourney> results = calculator.calculateRoute(Stations.Altrincham.getId(), Stations.ManAirport.getId(),
                minutes, today);

        assertTrue(results.size()>0);    // results is iterator
        for (RawJourney result : results) {
            List<TransportStage> stages = result.getStages();
            assertEquals(2, stages.size());
            RawVehicleStage firstStage = (RawVehicleStage) stages.get(0);
            assertEquals(Stations.Altrincham, firstStage.getFirstStation());
            assertEquals(Stations.TraffordBar, firstStage.getLastStation());
            assertEquals(TransportMode.Tram, firstStage.getMode());
            RawVehicleStage secondStage = (RawVehicleStage) stages.get(1);
            assertEquals(Stations.TraffordBar, secondStage.getFirstStation());
            assertEquals(Stations.ManAirport, secondStage.getLastStation());
            assertEquals(TransportMode.Tram, secondStage.getMode());
        }
    }

    @Test
    @Category({ClosureTest.class})
    public void shouldGetToRouteStopsAtVelopark() throws UnknownStationException {
        List<TransportRelationship> boarding = calculator.getOutboundStationRelationships(Stations.VeloPark.getId());
        //assertEquals(2*2, boarding.size()); // 2 platforms * 2 routes

        assertEquals(3*2, boarding.size()); // 2 platforms * 3 routes during closures
        assertTrue(boarding.get(0).isBoarding());  // we can get to either platform
        assertTrue(boarding.get(1).isBoarding());
        assertTrue(boarding.get(2).isBoarding());
    }

}