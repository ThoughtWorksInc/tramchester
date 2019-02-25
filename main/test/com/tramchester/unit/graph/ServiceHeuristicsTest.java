package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
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
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ServiceHeuristicsTest extends EasyMockSupport {

    public static final int MAX_WAIT = 30;

    private LocalTime am10 = LocalTime.of(10,0);
    private LocalTime[] tramTimes = new LocalTime[] { am10,
            am10.plusMinutes(100),
            am10.plusMinutes(200),
            am10.plusMinutes(300),
            am10.plusMinutes(400) };
    private CachingCostEvaluator costEvaluator;
    private TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(MAX_WAIT);
    private LocalTime NOT_USED_HERE = LocalTime.of(23,59);
    private NodeOperations nodeOperations;
    private Set<String> runningServices;

    @Before
    public void beforeEachTestRuns() {
        costEvaluator = new CachingCostEvaluator();
        nodeOperations = new CachedNodeOperations();
        runningServices = new HashSet<>();
    }

    @Test
    public void shouldCheckNodeBasedOnServiceIdAndTimeMatches() {
        LocalTime queryTime = LocalTime.of(8,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        runningServices.add("serviceIdA");

        Node node = createMock(Node.class);
        LocalTime elapsed = queryTime.plusHours(1);
        EasyMock.expect(node.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(node.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME)).andReturn(LocalTime.of(9,0));
        EasyMock.expect(node.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME)).andReturn(LocalTime.of(9,30));

        EasyMock.expect(node.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdB");

        replayAll();
        ServiceReason result = serviceHeuristics.checkService(node, elapsed);
        assertEquals(ServiceReason.IsValid, result);

        result = serviceHeuristics.checkService(node, elapsed);
        assertEquals(ServiceReason.DoesNotRunOnQueryDate("xxx"), result);
        verifyAll();
    }

    @Test
    public void shouldCheckNodeBasedOnServiceIdAndTimeOverlaps() {
        LocalTime queryTime = LocalTime.of(8,1);
        LocalTime elaspsedTime = LocalTime.of(9,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        runningServices.add("serviceIdA");

        // no longer running at query time
        Node tooEarlyNode = createMock(Node.class);
        EasyMock.expect(tooEarlyNode.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(tooEarlyNode.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME)).andReturn(LocalTime.of(8,00));
        EasyMock.expect(tooEarlyNode.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME)).andReturn(LocalTime.of(8,30));

        // doesnt start running until after query time
        Node tooLateNode = createMock(Node.class);
        EasyMock.expect(tooLateNode.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(tooLateNode.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME)).andReturn(elaspsedTime.plusMinutes(MAX_WAIT+1));
        EasyMock.expect(tooLateNode.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME)).andReturn(elaspsedTime.plusMinutes(MAX_WAIT+30));

        // starts before query, but still running within max wait
        Node overlapStartsBefore = createMock(Node.class);
        EasyMock.expect(overlapStartsBefore.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(overlapStartsBefore.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME)).andReturn(LocalTime.of(8,50));
        EasyMock.expect(overlapStartsBefore.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME)).andReturn(LocalTime.of(9,20));

        // starts after query within max wait, finishes after max wait
        Node overlapStartsAfter = createMock(Node.class);
        EasyMock.expect(overlapStartsAfter.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(overlapStartsAfter.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME)).andReturn(LocalTime.of(9,20));
        EasyMock.expect(overlapStartsAfter.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME)).andReturn(LocalTime.of(9,45));

        // starts before query, finishes after max wait
        Node overlapStartsBeforeFinishesAfter = createMock(Node.class);
        EasyMock.expect(overlapStartsBeforeFinishesAfter.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(overlapStartsBeforeFinishesAfter.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME)).andReturn(LocalTime.of(8,45));
        EasyMock.expect(overlapStartsBeforeFinishesAfter.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME)).andReturn(LocalTime.of(9,20));

        // end is after midnight case
        Node endsAfterMidnight = createMock(Node.class);
        EasyMock.expect(endsAfterMidnight.getProperty(GraphStaticKeys.SERVICE_ID)).andReturn("serviceIdA");
        EasyMock.expect(endsAfterMidnight.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME)).andReturn(LocalTime.of(5,23));
        EasyMock.expect(endsAfterMidnight.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME)).andReturn(LocalTime.of(0,1));

        replayAll();
        assertEquals(ServiceReason.DoesNotOperateOnTime(elaspsedTime, "diag"),
                serviceHeuristics.checkService(tooEarlyNode, elaspsedTime));
        assertEquals(ServiceReason.DoesNotOperateOnTime(elaspsedTime, "diag"),
                serviceHeuristics.checkService(tooLateNode, elaspsedTime));

        assertEquals(ServiceReason.IsValid, serviceHeuristics.checkService(overlapStartsBefore, elaspsedTime));
        assertEquals(ServiceReason.IsValid, serviceHeuristics.checkService(overlapStartsAfter, elaspsedTime));
        assertEquals(ServiceReason.IsValid, serviceHeuristics.checkService(overlapStartsBeforeFinishesAfter, elaspsedTime));
        assertEquals(ServiceReason.IsValid, serviceHeuristics.checkService(endsAfterMidnight, elaspsedTime));

        verifyAll();
    }

    @Test
    public void shouldBeInterestedInCorrectHours() {
        LocalTime queryTime = LocalTime.of(9,1);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        // querytime + costSoFar + maxWait (for board) = latest time could arrive here
        // querytime + costSoFar + 0 = earlier time could arrive here

        int costSoFar = 58;
        LocalTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(8, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(9, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(10, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(11, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursCrossesNextHour() {
        LocalTime queryTime = LocalTime.of(9,50);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        int costSoFar = 15;
        LocalTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(8, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(9, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(10, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(11, elapsed).isValid());
    }

    @Test
    public void shouldBeInterestedInCorrectHoursOverMidnight() {
        LocalTime queryTime = LocalTime.of(23,10);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        int costSoFar = 15;
        LocalTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(22, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(23, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(0, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(1, elapsed).isValid());
    }

    @Test
    public void shouldCheckTimeAtNodeCorrectly() {
        LocalTime queryTime = LocalTime.of(7,00);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        LocalTime nodeTime = LocalTime.of(8, 00);

        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(60), nodeTime, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(30), nodeTime, true);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(45), nodeTime, true);

        // too early
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(29), nodeTime, false);
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(10), nodeTime, false);
        // too late
        checkForNodeTime(serviceHeuristics, queryTime.plusMinutes(61), nodeTime, false);

    }

    public void checkForNodeTime(ServiceHeuristics serviceHeuristics, LocalTime currentElapsed, LocalTime nodeTime, boolean expect) {
        resetAll();

        Node node = createMock(Node.class);
        EasyMock.expect(node.getId()).andStubReturn(42L); // ok IFF node time always same
        EasyMock.expect(node.getProperty(GraphStaticKeys.TIME)).andStubReturn(nodeTime);

        replayAll();
        assertEquals(expect, serviceHeuristics.checkTime(node, currentElapsed).isValid());
        verifyAll();
    }

    @Test
    public void checkForSvcChange() {

        LocalTime queryTime = LocalTime.of(9,10);

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        boolean inboundWasGoesTo = true;
        Relationship next = createMock(Relationship.class);
        EasyMock.expect(next.getProperty("service_id")).andStubReturn("inboundId");

        replayAll();
        assertTrue("id match", serviceHeuristics.checkForSvcChange(next, inboundWasGoesTo, false, "inboundId"));
        assertFalse("id mismatch", serviceHeuristics.checkForSvcChange(next, inboundWasGoesTo, false, "XXXX"));
        assertFalse("id mismatch", serviceHeuristics.checkForSvcChange(next, inboundWasGoesTo, true, "XXXX"));

        assertTrue("boarding", serviceHeuristics.checkForSvcChange(next, false, true, "XXXX"));


        try {
            serviceHeuristics.checkForSvcChange(next, false, false, "XXXX");
            fail("should throw");
        } catch (RuntimeException expected) {
            // expected
        }

        verifyAll();
    }

    @Test
    public void shouldBeInterestedInCorrectHoursOverMidnightLongerJourney() {
        LocalTime queryTime = LocalTime.of(23,10);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait,
                queryTime, runningServices);

        int costSoFar = 51;
        LocalTime elapsed = queryTime.plusMinutes(costSoFar);

        assertFalse(serviceHeuristics.interestedInHour(22, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(23, elapsed).isValid());
        assertTrue(serviceHeuristics.interestedInHour(0, elapsed).isValid());
        assertFalse(serviceHeuristics.interestedInHour(1, elapsed).isValid());
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
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, runningServices);
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
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, configuration, NOT_USED_HERE, runningServices);
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
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, runningServices);
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
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, runningServices);
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
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, nodeOperations, config30MinsWait, NOT_USED_HERE, runningServices);
        assertTrue(serviceHeuristics.sameService(board, outA).isValid());
        assertTrue(serviceHeuristics.sameService(depart, outA).isValid());
        assertTrue(serviceHeuristics.sameService(change, outA).isValid());
        assertTrue(serviceHeuristics.sameService(outA, outA).isValid());
        assertFalse(serviceHeuristics.sameService(outB, outA).isValid());
        verifyAll();
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