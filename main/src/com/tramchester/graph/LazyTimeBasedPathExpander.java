package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class LazyTimeBasedPathExpander implements PathExpander<Double> {
    private static final Logger logger = LoggerFactory.getLogger(LazyTimeBasedPathExpander.class);

    private final LocalTime queryTime;
    private final RelationshipFactory relationshipFactory;
    private final ServiceHeuristics serviceHeuristics;

    private final boolean edgePerService;
    private final NodeOperations nodeOperations;
    //private long endNodeId;
    //private final CostEvaluator<Double> cachingCostEvaluator;

    public LazyTimeBasedPathExpander(LocalTime queryTime, RelationshipFactory relationshipFactory, ServiceHeuristics serviceHeuristics,
                                     TramchesterConfig config, NodeOperations nodeOperations, CostEvaluator<Double> cachingCostEvaluator) {
        this.queryTime = queryTime;
        this.relationshipFactory = relationshipFactory;
        this.serviceHeuristics = serviceHeuristics;
        edgePerService = config.getEdgePerTrip();
        this.nodeOperations = nodeOperations;
        //this.cachingCostEvaluator = cachingCostEvaluator;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<Double> branchState) {

        if (!edgePerService) {
            return () -> new RelationshipIterable(path);
        }

        Node endNode = path.endNode();
        long endNodeId = endNode.getId();

        logger.debug(format("Expand for node %s, type %s, id %s", endNodeId, endNode.getLabels(),
                endNode.getProperties(GraphStaticKeys.ID)));

        // only pursue outbound edges from a service service runs today & within time
        if (nodeOperations.isService(endNode)) {
            if (!serviceHeuristics.checkServiceDate(endNode, path).isValid()) {
                logger.debug(format("Skip node %s, type %s, id %s", endNodeId, endNode.getLabels(),
                        endNode.getProperties(GraphStaticKeys.ID)));
                return Iterables.empty();
            }

            LocalTime currentElapsed = calculateElapsedTimeForPath(path);
            if (!serviceHeuristics.checkServiceTime(path, endNode, currentElapsed).isValid()) {
                logger.debug(format("Skip node %s, type %s, id %s", endNodeId, endNode.getLabels(),
                        endNode.getProperties(GraphStaticKeys.ID)));
                return Iterables.empty();
            }
        }

//        Optimisation
//        only follow hour nodes if match up with possible journeys
        if (nodeOperations.isHour(endNode)) {
            int hour = nodeOperations.getHour(endNode);
            LocalTime currentElapsed = calculateElapsedTimeForPath(path);
            if (!serviceHeuristics.interestedInHour(path, hour, currentElapsed).isValid()) {
                logger.debug(format("Skip node %s, type %s, id %s", endNodeId, endNode.getLabels(),
                        endNode.getProperties(GraphStaticKeys.ID)));
                return Iterables.empty();
            }
        }

        if (nodeOperations.isTime(endNode)) {
            LocalTime currentElapsed = calculateElapsedTimeForPath(path);
            if (!serviceHeuristics.checkTime(path, endNode, currentElapsed).isValid()) {
                logger.debug(format("Skip node %s, type %s, id %s", endNodeId, endNode.getLabels(),
                        endNode.getProperties(GraphStaticKeys.ID)));
                return Iterables.empty();
            }
        }

        return buildSimpleExpandsionList(path);
    }

    private Iterable<Relationship> buildSimpleExpandsionList(Path path) {
        logger.debug("Build list for: " + path);
        Node endNode = path.endNode();

        long endNodeId = endNode.getId();
        logger.debug(format("Build list for node %s, type %s, id %s", endNodeId, endNode.getLabels(),
                endNode.getProperties(GraphStaticKeys.ID)));

        Iterable<Relationship> outboundRelationships = endNode.getRelationships(OUTGOING);

        // TODO
        if (endNode.hasLabel(TransportGraphBuilder.Labels.SERVICE) ||
                endNode.hasLabel(TransportGraphBuilder.Labels.HOUR) ||
                endNode.hasLabel(TransportGraphBuilder.Labels.PLATFORM) ||
                endNode.hasLabel(TransportGraphBuilder.Labels.MINUTE) ||
                endNode.hasLabel(TransportGraphBuilder.Labels.STATION)) {

            logger.debug(format("Include all outbound for node %s", endNodeId));
            if (!outboundRelationships.iterator().hasNext()) {
                logger.warn("No outbound nodes found at node " + endNodeId);
            }
            return allRelationships(outboundRelationships);
        }

        Relationship inbound = path.lastRelationship();
        if (inbound==null) {
            logger.debug(format("Include all outbound for FIRST node %s", endNodeId));
            return outboundRelationships;
        }

        boolean inboundWasBoarding = inbound.isType(BOARD) || inbound.isType(INTERCHANGE_BOARD);

        List<Relationship> excluded = new ArrayList<>();
        List<Relationship> result = new ArrayList<>();

        for(Relationship outbound : outboundRelationships) {
            if (serviceHeuristics.checkReboardAndSvcChanges(path, inbound, inboundWasBoarding, outbound).isValid()) {
                result.add(outbound);
            } else {
                excluded.add(outbound);
            }
        }

        if (result.size()==0) {
            logger.debug(format("No outbound from %s %s, arrived via %s %s, excluded was %s ",
                    endNode.getLabels(), endNode.getProperties(GraphStaticKeys.ID),
                    inbound.getStartNode().getLabels(), inbound.getStartNode().getProperties(GraphStaticKeys.ID),
                    excluded));
        }

        if (logger.isDebugEnabled()) {
            excluded.forEach(exclude -> logger.debug(format("At node %s excluded %s", endNode.getAllProperties(), exclude)));
        }
        logger.debug(format("For node %s included %s and excluded %s", endNodeId, result.size(), excluded.size()));

        return result;
    }

    private Iterable<Relationship> allRelationships(Iterable<Relationship> outboundRelationships) {
        List<Relationship> results = new ArrayList<>();
        outboundRelationships.forEach(
                outbound->results.add(outbound));
        return results;
    }


    public LocalTime calculateElapsedTimeForPath(Path path) {

        Iterator<Relationship> relationshipIterator = path.reverseRelationships().iterator();
        int cost = 0;
        while(relationshipIterator.hasNext()) {
            Relationship relationship = relationshipIterator.next();

            cost = cost + (int) relationship.getProperty(GraphStaticKeys.COST);
            if (relationship.isType(TRAM_GOES_TO)) {
                Node timeNode = relationship.getStartNode();
                LocalTime lastSeenTimeNode = nodeOperations.getTime(timeNode);
                LocalTime localTime = lastSeenTimeNode.plusMinutes(cost);
                logger.debug(format("Time for %s is %s", path.toString(), localTime));
                return localTime;
            }
        }
        logger.debug(format("Time for %s is %s", path.toString(), queryTime.plusMinutes(cost)));
        return queryTime.plusMinutes(cost);

    }

    public class RelationshipIterable implements Iterator<Relationship> {
        private final Path path;
        private final Iterator<Relationship> relationships;
        private final boolean justBoarded;

        private Relationship next;

        public RelationshipIterable(Path path) {
            this.path = path;
            this.relationships = path.endNode().getRelationships(OUTGOING).iterator();
            Relationship inboundToLastNode = path.lastRelationship();
            if (inboundToLastNode!=null) {
                justBoarded = inboundToLastNode.isType(BOARD) || inboundToLastNode.isType(INTERCHANGE_BOARD);
            } else {
                justBoarded = false;
            }
        }

        @Override
        public boolean hasNext() {
            while (relationships.hasNext()) {
                next = relationships.next();
                if (next.isType(TRAM_GOES_TO)) {
                    if (interestedIn(next)) {
                        return true;
                    }
                } else {
                    if (!justBoarded) {
                        return true;
                    }
                    // so we just boarded a tram, don't attempt to immediately get off again
                    boolean departing = next.isType(DEPART) || next.isType(INTERCHANGE_DEPART);
                    if (!departing) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Relationship next() {
            return next;
        }

        private boolean interestedIn(Relationship graphRelationship) {
            // NOT called for edgePerService

            TransportRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);
            GoesToRelationship goesToRelationship = (GoesToRelationship) outgoing;

            try {
                TransportRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());
                ServiceReason serviceReason = serviceHeuristics.checkServiceHeuristics(incoming, goesToRelationship, path);
                return serviceReason.isValid();
            } catch (TramchesterException e) {
                logger.error("Unable to check service heuristics",e);
            }
            return false;
        }
    }

    @Override
    public PathExpander<Double> reverse() {
        return this;
    }

}

