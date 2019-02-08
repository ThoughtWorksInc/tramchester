package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Service;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.*;
import com.tramchester.graph.Relationships.*;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ServiceHeuristicsTest extends EasyMockSupport {

    private LocalTime am10 = LocalTime.of(10,0);
    private LocalTime[] tramTimes = new LocalTime[] { am10,
            am10.plusMinutes(100),
            am10.plusMinutes(200),
            am10.plusMinutes(300),
            am10.plusMinutes(400) };
    private CachingCostEvaluator costEvaluator;
    private TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(30);
    private TramServiceDate date;
    private LocalTime NOT_USED_HERE = LocalTime.of(23,59);
    private NodeOperations nodeOperations;
    private Set<String> runningServices;

    @Before
    public void beforeEachTestRuns() {
        date = new TramServiceDate(LocalDate.now());
        costEvaluator = new CachingCostEvaluator();
        nodeOperations = new CachedNodeOperations();
        runningServices = new HashSet<>();
    }

    @Test
    public void shouldCheckNodeBasedOnServiceId() {
        LocalTime queryTime = LocalTime.of(9,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, date, runningServices);

        runningServices.add("serviceIdA");

        Node node = createMock(Node.class);
        EasyMock.expect(node.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(node.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdB");

        replayAll();
        ServiceReason result = serviceHeuristics.checkService(node);
        assertEquals(ServiceReason.IsValid, result);

        result = serviceHeuristics.checkService(node);
        assertEquals(ServiceReason.DoesNotRunOnQueryDate, result);
        verifyAll();
    }

    @Test
    public void shouldBeInterestedInCorrectHours() {
        LocalTime queryTime = LocalTime.of(9,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, date, runningServices);

        // querytime + costSoFar + maxWait (for board) = latest time could arrive here
        // querytime + costSoFar + 0 = earlier time could arrive here

        int costSoFar = 58;

        assertFalse(serviceHeuristics.interestedInHour(8, costSoFar));
        assertTrue(serviceHeuristics.interestedInHour(9, costSoFar));
        assertTrue(serviceHeuristics.interestedInHour(10, costSoFar));
        assertFalse(serviceHeuristics.interestedInHour(11, costSoFar));
    }

    @Test
    public void shouldBeInterestedInCorrectHoursCrossesNextHour() {
        LocalTime queryTime = LocalTime.of(9,50);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, date, runningServices);

        int costSoFar = 15;

        assertFalse(serviceHeuristics.interestedInHour(8, costSoFar));
        assertTrue(serviceHeuristics.interestedInHour(9, costSoFar));
        assertTrue(serviceHeuristics.interestedInHour(10, costSoFar));
        assertFalse(serviceHeuristics.interestedInHour(11, costSoFar));
    }

    @Test
    public void shouldBeInterestedInCorrectHoursOverMidnight() {
        int costSoFar = 15;

        LocalTime queryTime = LocalTime.of(23,10);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, date, runningServices);
        assertFalse(serviceHeuristics.interestedInHour(22, costSoFar));
        assertTrue(serviceHeuristics.interestedInHour(23, costSoFar));
        assertFalse(serviceHeuristics.interestedInHour(0, costSoFar));
        assertFalse(serviceHeuristics.interestedInHour(1, costSoFar));
    }

    @Test
    public void shouldBeInterestedInCorrectHoursOverMidnightLongerJourney() {
        int costSoFar = 51;

        LocalTime queryTime = LocalTime.of(23,10);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, date, runningServices);
        assertFalse(serviceHeuristics.interestedInHour(22, costSoFar));
        assertTrue(serviceHeuristics.interestedInHour(23, costSoFar));
        assertTrue(serviceHeuristics.interestedInHour(0, costSoFar));
        assertFalse(serviceHeuristics.interestedInHour(1, costSoFar));
    }

    @Test
    public void shouldHandleTimesWith30MinWait() throws TramchesterException {
        LocalTime am640 = LocalTime.of(6, 40); // 400

        ElapsedTime providerA = createNoMatchProvider(am640);
        ElapsedTime providerB = createNoMatchProvider(am640.plusMinutes(150));

        LocalTime journeyStart = LocalTime.of(10,0).minusMinutes(TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerC = createMatchProvider(am640.plusMinutes(180), journeyStart);
        ElapsedTime providerD = createMatchProvider(am640.plusMinutes(200), journeyStart);
        ElapsedTime providerE = createNoMatchProvider(am640.plusMinutes(220));
        ElapsedTime providerF = createNoMatchProvider(am640.plusMinutes(230));
        ElapsedTime providerG = createNoMatchProvider(am640.plusMinutes(250));

        ElapsedTime providerH = createMatchProvider(am640.plusMinutes(280), journeyStart.plusMinutes(100));
        ElapsedTime providerI = createNoMatchProvider(am640.plusMinutes(601));

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, date, runningServices);
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerA));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerB));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerC));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerD));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerE));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerF));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerG));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerH));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerI));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesWith15MinWait() throws TramchesterException {
        LocalTime am640 = LocalTime.of(6, 40); // 400

        ElapsedTime providerA = createNoMatchProvider(am640);
        ElapsedTime providerB = createNoMatchProvider(am640.plusMinutes(150));
        ElapsedTime providerC = createNoMatchProvider(am640.plusMinutes(180));
        LocalTime journeyStart = LocalTime.of(10,0).minusMinutes(TransportGraphBuilder.BOARDING_COST);

        ElapsedTime providerD = createMatchProvider(am640.plusMinutes(200), journeyStart);
        ElapsedTime providerE = createNoMatchProvider(am640.plusMinutes(220));
        ElapsedTime providerF = createNoMatchProvider(am640.plusMinutes(230));
        ElapsedTime providerG = createNoMatchProvider(am640.plusMinutes(250));
        ElapsedTime providerH = createNoMatchProvider(am640.plusMinutes(280));
        ElapsedTime providerI = createMatchProvider(am640.plusMinutes(190), journeyStart);
        ElapsedTime providerJ = createNoMatchProvider(am640.plusMinutes(601));

        replayAll();
        TramchesterConfig configuration = new NeedMaxWaitConfig(15);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, configuration, NOT_USED_HERE, date, runningServices);
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerA));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerB));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerC));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerD));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerE));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerF));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerG));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerH));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerI));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerJ));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesOneTime() throws TramchesterException {
        LocalTime am640 = LocalTime.of(6, 40); // 400

        LocalTime[] time = new LocalTime[] { LocalTime.of(7,30) };
        ElapsedTime providerA = createNoMatchProvider(am640);
        LocalTime journeyStart = am640.plusMinutes(50).minusMinutes(TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerB = createMatchProvider(am640.plusMinutes(20), journeyStart);
        ElapsedTime providerC = createNoMatchProvider(am640.plusMinutes(51));

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, date, runningServices);
        assertFalse(serviceHeuristics.operatesOnTime(time, providerA));
        assertTrue(serviceHeuristics.operatesOnTime(time, providerB));
        assertFalse(serviceHeuristics.operatesOnTime(time, providerC));
        verifyAll();
    }

    @Test
    public void shouldHandleTotalDurationOverWaitTime() throws TramchesterException {
        LocalTime[] time = new LocalTime[] { LocalTime.of(7,30) };

        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(LocalTime.of(7,20));
        EasyMock.expect(provider.startNotSet()).andReturn(false);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, date, runningServices);
        assertTrue(serviceHeuristics.operatesOnTime(time, provider));
        verifyAll();
    }

    @Test
    public void shouldCheckIfChangeOfServiceWithDepartAndThenBoard() {
        TramServiceDate startDate = new TramServiceDate("20141201");
        TramServiceDate endDate = new TramServiceDate("20151130");
        boolean[] days = new boolean[] {true,true,true,true,true,true,true};

        TransportRelationship board = BoardRelationship.TestOnly("boardsId", null, null);
        TransportRelationship depart = DepartRelationship.TestOnly("departsId", null, null);
        TransportRelationship change = InterchangeDepartsRelationship.TestOnly("interchangeId",null,null);

        String tripId = "";

        TramGoesToRelationship outA = TramGoesToRelationship.TestOnly("0042",10, days, tramTimes, "id1", startDate,
                endDate, "destA", null, null, tripId);

        TramGoesToRelationship outB = TramGoesToRelationship.TestOnly("0048", 5, days, tramTimes, "id2", startDate,
                endDate, "destB", null, null, tripId);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, startDate, runningServices);
        assertTrue(serviceHeuristics.sameService(board, outA));
        assertTrue(serviceHeuristics.sameService(depart, outA));
        assertTrue(serviceHeuristics.sameService(change, outA));
        assertTrue(serviceHeuristics.sameService(outA, outA));
        assertFalse(serviceHeuristics.sameService(outB, outA));
        verifyAll();
    }


    @Test
    public void shouldCheckTramServiceDate() {
        TramServiceDate startDate = new TramServiceDate(LocalDate.of(2016, 6, 1));
        TramServiceDate endDate = new TramServiceDate(LocalDate.of(2016, 6, 29));

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, startDate, runningServices);

        assertTrue(serviceHeuristics.operatesOnQueryDate(startDate, endDate,
                new TramServiceDate(LocalDate.of(2016, 6, 15))));
        assertTrue(serviceHeuristics.operatesOnQueryDate(startDate, endDate,
                new TramServiceDate(LocalDate.of(2016, 6, 1))));
        serviceHeuristics.operatesOnQueryDate(startDate, endDate,
                new TramServiceDate(LocalDate.of(2016, 6, 29)));
        assertFalse(serviceHeuristics.operatesOnQueryDate(startDate, endDate,
                new TramServiceDate(LocalDate.of(2016, 12, 15))));

    }

    @Test
    public void shouldCheckIfTramRunsOnADay() {

        checkDay(true,false,false,false,false,false,false, DaysOfWeek.Monday);
        checkDay(false,true,false,false,false,false,false,DaysOfWeek.Tuesday);
        checkDay(false,false,true,false,false,false,false,DaysOfWeek.Wednesday);
        checkDay(false,false,false,true,false,false,false,DaysOfWeek.Thursday);
        checkDay(false,false,false,false,true,false,false,DaysOfWeek.Friday);
        checkDay(false,false,false,false,false,true,false,DaysOfWeek.Saturday);
        checkDay(false, false, false, false, false, false, true, DaysOfWeek.Sunday);
    }

    private void checkDay(boolean d1, boolean d2, boolean d3, boolean d4,
                          boolean d5, boolean d6, boolean d7, DaysOfWeek day) {

        boolean[] days = new boolean[]{d1, d2, d3, d4, d5, d6, d7};
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, date, runningServices);
        assertTrue(serviceHeuristics.operatesOnDayOnWeekday(days, day));
    }

    private ElapsedTime createMatchProvider(LocalTime queryTime, LocalTime journeyStart) throws TramchesterException {
        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(queryTime);
        EasyMock.expect(provider.startNotSet()).andReturn(true);
        provider.setJourneyStart(journeyStart);
        EasyMock.expectLastCall();

        return provider;
    }

    private ElapsedTime createNoMatchProvider(LocalTime queryTime) throws TramchesterException {
        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andReturn(queryTime);
        return provider;
    }

    private class NeedMaxWaitConfig extends IntegrationTramTestConfig {
        private int maxWait;

        public NeedMaxWaitConfig(int maxWait) {
            this.maxWait = maxWait;
        }

        @Override
        public int getMaxWait() {
            return maxWait;
        }
    }
}
