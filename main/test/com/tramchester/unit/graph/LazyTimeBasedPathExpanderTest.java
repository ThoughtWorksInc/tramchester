package com.tramchester.unit.graph;


import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.*;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.MDC;

import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.BOARD;
import static com.tramchester.graph.TransportRelationshipTypes.INTERCHANGE_BOARD;
import static org.junit.Assert.assertEquals;

public class LazyTimeBasedPathExpanderTest extends EasyMockSupport {

    private Relationship departs;
    private Relationship boards;
    private Relationship goesToA;
    private Relationship goesToB;
    private RelationshipFactory mockRelationshipFactory;
    private ServiceHeuristics serviceHeuristics;
    private Node endNode;
    private BranchState<Double> branchState;
    private LocalTime queryTime;

    @Rule
    public TestName testName = new TestName();
    private Path path;
    private TramchesterConfig config = new NoEdgePerTripTestConfig();
    private NodeOperations mockNodeOperations;
    private CachingCostEvaluator mockCostEvaluator;

    @Before
    public void beforeEachTestRuns() {

        branchState = null;

        // for logging
        MDC.put("test", testName.getMethodName());

        departs = createMock(Relationship.class);
        EasyMock.expect(departs.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(false);

        boards = createMock(Relationship.class);
        EasyMock.expect(boards.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(false);

        goesToA = createMock(Relationship.class);
        EasyMock.expect(goesToA.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(true);

        goesToB = createMock(Relationship.class);
        EasyMock.expect(goesToB.isType(TransportRelationshipTypes.TRAM_GOES_TO)).andStubReturn(true);

        mockRelationshipFactory = createMock(RelationshipFactory.class);
        serviceHeuristics = createMock(ServiceHeuristics.class);
        endNode = createMock(Node.class);
        path = createMock(Path.class);

        mockNodeOperations = createMock(NodeOperations.class);
        mockCostEvaluator = createMock(CachingCostEvaluator.class);

        queryTime = LocalTime.now();
    }

    @Test
    public void shouldExpandPathsCorrectlyForInitialPath() {

        Set<Relationship> outgoingRelationships = createRelationships(boards, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);

        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(queryTime, mockRelationshipFactory,
                serviceHeuristics, config, mockNodeOperations, mockCostEvaluator);

        Iterable<Relationship> results = pathExpander.expand(path, branchState);

        int number = countResults(results);
        verifyAll();

        assertEquals(2, number);
    }

    @Test
    public void shouldExpandPathsCorrectlyForMatchingPath() throws TramchesterException {
        Set<Relationship> outgoingRelationships = createRelationships(boards, goesToA, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);

        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        TransportRelationship incoming = createMock(TransportRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andReturn(incoming);

        GoesToRelationship goesTo = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo, path)).
                andReturn(ServiceReason.IsValid);

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(queryTime, mockRelationshipFactory,
                serviceHeuristics,config, mockNodeOperations, mockCostEvaluator);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        int actual = countResults(results);

        verifyAll();

        assertEquals(3, actual);
    }

    @Test
    public void shouldExpandPathsCorrectlyForNonMatchingPath() throws TramchesterException {
        Set<Relationship> outgoingRelationships = createRelationships(boards, goesToA, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);
        //EasyMock.expect(path.length()).andStubReturn(1);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);
        EasyMock.expect(path.lastRelationship()).andReturn(lastRelationship);

        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        TransportRelationship incoming = createMock(TransportRelationship.class);

        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andReturn(incoming);
        GoesToRelationship goesTo = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo, path)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate("diag"));

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(queryTime, mockRelationshipFactory,
                serviceHeuristics,config, mockNodeOperations, mockCostEvaluator);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        int actual = countResults(results);

        verifyAll();

        assertEquals(2, actual);
    }

    @Test
    public void shouldExpandPathsCorrectlyForMixedMatchingPath() throws TramchesterException {
        Set<Relationship> outgoingRelationships = createRelationships(boards, goesToA, goesToB, departs);

        EasyMock.expect(path.endNode()).andStubReturn(endNode);
        EasyMock.expect(endNode.getRelationships(Direction.OUTGOING)).andReturn(outgoingRelationships);

        EasyMock.expect(path.length()).andStubReturn(1);

        Relationship lastRelationship = createMock(Relationship.class);
        EasyMock.expect(path.lastRelationship()).andStubReturn(lastRelationship);
        EasyMock.expect(lastRelationship.isType(BOARD)).andStubReturn(false);
        EasyMock.expect(lastRelationship.isType(INTERCHANGE_BOARD)).andStubReturn(false);

        TransportRelationship incoming = createMock(TransportRelationship.class);

        EasyMock.expect(mockRelationshipFactory.getRelationship(lastRelationship)).andStubReturn(incoming);
        GoesToRelationship goesTo1 = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToA)).andReturn(goesTo1);
        GoesToRelationship goesTo2 = createMock(GoesToRelationship.class);
        EasyMock.expect(mockRelationshipFactory.getRelationship(goesToB)).andReturn(goesTo2);

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo1, path)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate("diag"));

        EasyMock.expect(serviceHeuristics.checkServiceHeuristics(incoming, goesTo2, path)).
                andReturn(ServiceReason.IsValid);

        replayAll();
        PathExpander<Double> pathExpander = new LazyTimeBasedPathExpander(queryTime, mockRelationshipFactory,
                serviceHeuristics,config, mockNodeOperations, mockCostEvaluator);
        Iterable<Relationship> results = pathExpander.expand(path, branchState);
        int actual = countResults(results);
        verifyAll();

        assertEquals(3, actual);
    }

    private Set<Relationship> createRelationships(Relationship... relats) {
        Set<Relationship> relationships = new HashSet<>();
        Collections.addAll(relationships, relats);
        return relationships;
    }

    private int countResults(Iterable<Relationship> results) {
        int count = 0;
        for(Relationship ignored : results) {
            count++;
        }
        return count;
    }

    private class NoEdgePerTripTestConfig extends TestConfig {
        @Override
        public java.nio.file.Path getDataFolder() {
            return null;
        }

        @Override
        public boolean getEdgePerTrip() {
            return false;
        }

    }
}