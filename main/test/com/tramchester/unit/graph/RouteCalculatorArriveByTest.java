package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteCalculatorArriveBy;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RouteCalculatorArriveByTest extends EasyMockSupport {

    private RouteCalculatorArriveBy routeCalculatorArriveBy;
    private RouteCalculator routeCalculator;
    private RouteCostCalculator costCalculator;
    private int costBetweenStartDest;
    private TramchesterConfig config;
    private Transaction txn;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        txn = createStrictMock(Transaction.class);
        costCalculator = createStrictMock(RouteCostCalculator.class);
        routeCalculator = createStrictMock(RouteCalculator.class);
        config = createStrictMock(TramchesterConfig.class);
        routeCalculatorArriveBy = new RouteCalculatorArriveBy(costCalculator, routeCalculator, config);
        costBetweenStartDest = 15;
    }

    @Test
    void shouldArriveByTramNoWalk() {
        TramTime arriveBy = TramTime.of(14,35);
        LocalDate localDate = TestEnv.testDay();

        Station start = TramStations.of(TramStations.Bury);
        Station destinationId = TramStations.of(TramStations.Cornbrook);
        TramServiceDate serviceDate = TramServiceDate.of(localDate);

        Stream<Journey> journeyStream = Stream.empty();

        //EasyMock.expect(config.getTransportModes()).andReturn(Arrays.asList(GTFSTransportationType.tram));

        EasyMock.expect(costCalculator.getApproxCostBetween(txn, start, destinationId)).andReturn(costBetweenStartDest);
        TramTime requiredDepartTime = arriveBy.minusMinutes(costBetweenStartDest).minusMinutes(17); // 17 = 34/2
        JourneyRequest updatedWithComputedDepartTime = new JourneyRequest(serviceDate, requiredDepartTime, true,
                5, 120);
        EasyMock.expect(routeCalculator.calculateRoute(txn, start, destinationId, updatedWithComputedDepartTime)).andReturn(journeyStream);
        EasyMock.expect(config.getMaxWait()).andReturn(34);

        replayAll();
        JourneyRequest originalRequest = new JourneyRequest(serviceDate, arriveBy, false, 5, 120);
        Stream<Journey> result = routeCalculatorArriveBy.calculateRoute(txn, start, destinationId, originalRequest);
        verifyAll();
        assertSame(journeyStream, result);
    }

}