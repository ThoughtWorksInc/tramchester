package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;

import static com.tramchester.graph.RouteCalculator.MAX_NUM_GRAPH_PATHS;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements Evaluator, PathExpander<String> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final ServiceHeuristics serviceHeuristics;
    private final NodeOperations nodeOperations;
    private final LocalTime queryTime;
    private final Set<Long> stations;

    private final Map<Long, Set<TramTime>> visited;

    // TODO CALC
    private final int pathLimit = 400;
    private final int maxJourneyMins = 170; // longest end to end is 163?

    public TramNetworkTraverser(ServiceHeuristics serviceHeuristics,
                                NodeOperations nodeOperations, LocalTime queryTime) {
        this.serviceHeuristics = serviceHeuristics;
        this.nodeOperations = nodeOperations;
        this.queryTime = queryTime;

        stations = new HashSet<>();
        visited = new HashMap<>();
    }

    public Iterable<WeightedPath> findPaths(Node startNode, Node endNode) {
        Traverser traverser = new MonoDirectionalTraversalDescription().
                relationships(TRAM_GOES_TO, Direction.OUTGOING).
                relationships(BOARD, Direction.OUTGOING).
                relationships(DEPART, Direction.OUTGOING).
                relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
                relationships(INTERCHANGE_DEPART, Direction.OUTGOING).
                relationships(WALKS_TO, Direction.OUTGOING).
                relationships(ENTER_PLATFORM, Direction.OUTGOING).
                relationships(LEAVE_PLATFORM, Direction.OUTGOING).
                relationships(TO_SERVICE, Direction.OUTGOING).
                relationships(TO_HOUR, Direction.OUTGOING).
                relationships(TO_MINUTE, Direction.OUTGOING).
                expand(this).
                evaluator(this).
                uniqueness(NONE).
                order(BranchOrderingPolicies.PREORDER_DEPTH_FIRST).
                traverse(startNode);

        ResourceIterator<Path> iterator = traverser.iterator();
        long endNodeId = endNode.getId();

        List<WeightedPath> results = new ArrayList<>();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            if (path.endNode().getId()==endNodeId) {
                results.add(calculateWeight(path));
            }
            if (results.size()>=MAX_NUM_GRAPH_PATHS) {
                break;
            }
        }

        Collections.sort(results, Comparator.comparingDouble(WeightedPath::weight));

        return results;
    }

    private WeightedPath calculateWeight(Path path) {
        Integer result = 0;
        for (Relationship relat: path.relationships()) {
            result = result + getCost(relat);
        }
        return new WeightedPathImpl(result.doubleValue(), path);

    }

    @Override
    public Evaluation evaluate(Path path) {
        Node endNode = path.endNode();
        long endNodeId = endNode.getId();

        // no journey longer than N stages
        if (path.length()>pathLimit) {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // visit each Station once
        if (nodeOperations.isStation(endNode)) {
            if (stations.contains(endNodeId)) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
            stations.add(endNodeId);
        }

        // is the service running today
        if (nodeOperations.isService(endNode)) {
            if (!serviceHeuristics.checkServiceDate(endNode, path).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        LocalTime currentElapsed = calculateElapsedTimeForPath(path);
        TramTime visitingTime = TramTime.of(currentElapsed);

        // already tried this node at this time?
        if (visited.containsKey(endNodeId)) {
            if (visited.get(endNodeId).contains(visitingTime)) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        } else {
            visited.put(endNodeId, new HashSet<>());
        }
        visited.get(endNodeId).add(visitingTime);

        // journey too long?
        if (TramTime.diffenceAsMinutes( TramTime.of(queryTime), visitingTime)>maxJourneyMins) {
            return Evaluation.INCLUDE_AND_PRUNE;
        }

        if (nodeOperations.isService(endNode)) {
            // service available to catch?
            if (!serviceHeuristics.checkServiceTime(path, endNode, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        if (nodeOperations.isHour(endNode)) {
            int hour = nodeOperations.getHour(endNode);
            if (!serviceHeuristics.interestedInHour(path, hour, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        if (nodeOperations.isTime(endNode)) {
            if (!serviceHeuristics.checkTime(path, endNode, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        return Evaluation.INCLUDE_AND_CONTINUE;
    }

    public LocalTime calculateElapsedTimeForPath(Path path) {

        Iterator<Relationship> relationshipIterator = path.reverseRelationships().iterator();
        int cost = 0;
        while(relationshipIterator.hasNext()) {
            Relationship relationship = relationshipIterator.next();

            cost = cost + getCost(relationship);
            if (relationship.isType(TRAM_GOES_TO)) {
                // TimeNode -> EndServiceNode
                Node timeNode = relationship.getStartNode();
                LocalTime lastSeenTimeNode = nodeOperations.getTime(timeNode);
                LocalTime localTime = lastSeenTimeNode.plusMinutes(cost);
                return localTime;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(format("Time for %s is %s", path.toString(), queryTime.plusMinutes(cost)));
        }
        return queryTime.plusMinutes(cost);
    }

    private int getCost(Relationship relationship) {
        return nodeOperations.getCost(relationship);
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<String> state) {

        Node endNode = path.endNode();
        long endNodeId = endNode.getId();

        Iterable<Relationship> outboundRelationships = endNode.getRelationships(OUTGOING);

        Relationship inbound = path.lastRelationship();
        if (inbound==null) {
            return outboundRelationships;
        }

        // TODO
        Label firstLabel = endNode.getLabels().iterator().next();
        TransportGraphBuilder.Labels nodeLabel = TransportGraphBuilder.Labels.valueOf(firstLabel.toString());
        if (nodeLabel==TransportGraphBuilder.Labels.SERVICE ||
                nodeLabel==TransportGraphBuilder.Labels.HOUR ||
                nodeLabel==TransportGraphBuilder.Labels.MINUTE ||
                nodeLabel==TransportGraphBuilder.Labels.STATION) {

            if (!outboundRelationships.iterator().hasNext()) {
                logger.warn("No outbound nodes found at node " + endNodeId);
            }
            return outboundRelationships;
        }

        boolean inboundWasBoarding = inbound.isType(BOARD) || inbound.isType(INTERCHANGE_BOARD);

        List<Relationship> excluded = new ArrayList<>();
        OrderedRelationships result = new OrderedRelationships(nodeLabel, inboundWasBoarding, inbound,
                serviceHeuristics);

        for(Relationship outbound : outboundRelationships) {
            if (serviceHeuristics.checkReboardAndSvcChanges(path, inbound, inboundWasBoarding, outbound).isValid()) {
                result.insert(outbound);
            } else {
                excluded.add(outbound);
            }
        }

        if (result.isEmpty() && logger.isDebugEnabled()) {
            logger.debug(format("No outbound from %s %s, arrived via %s %s, excluded was %s ",
                    endNode.getLabels(), endNode.getProperties(GraphStaticKeys.ID),
                    inbound.getStartNode().getLabels(), inbound.getStartNode().getProperties(GraphStaticKeys.ID),
                    excluded));
        }

        if (logger.isDebugEnabled()) {
            excluded.forEach(exclude -> logger.debug(format("At node %s excluded %s", endNode.getAllProperties(), exclude)));
            logger.debug(format("For node %s included %s and excluded %s", endNodeId, result.size(), excluded.size()));
        }

        return result;
    }

//    private Iterable<Relationship> allRelationships(Iterable<Relationship> outboundRelationships) {
//        List<Relationship> results = new ArrayList<>();
//        outboundRelationships.forEach(
//                outbound->results.add(outbound));
//        return results;
//    }

    @Override
    public PathExpander<String> reverse() {
        return null;
    }
}
