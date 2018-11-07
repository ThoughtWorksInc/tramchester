package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.TRAM_GOES_TO;
import static java.lang.String.format;

public class TimeBasedPathExpander implements PathExpander<GraphBranchState> {
    private static final Logger logger = LoggerFactory.getLogger(TimeBasedPathExpander.class);

    private final NodeFactory nodeFactory;
    private RelationshipFactory relationshipFactory;
    private ServiceHeuristics serviceHeuristics;

    public TimeBasedPathExpander(RelationshipFactory relationshipFactory,
                                 NodeFactory nodeFactory, ServiceHeuristics serviceHeuristics) {
        this.relationshipFactory = relationshipFactory;
        this.nodeFactory = nodeFactory;
        this.serviceHeuristics = serviceHeuristics;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<GraphBranchState> branchState) {

        Node endNode = path.endNode();
        Iterable<Relationship> relationships = endNode.getRelationships(Direction.OUTGOING);

        if (path.length()==0) { // start of journey
            serviceHeuristics.clear();
            return relationships;
        }

        TransportRelationship incoming =  relationshipFactory.getRelationship(path.lastRelationship());

        if (incoming.isBoarding() && (path.length()==1 || path.length()==2 )) {
            // need to reset to real journey start, 1 or 2 depending on wether modelling platforms
            serviceHeuristics.clear();
        }

        List<ServiceReason> servicesFilteredOut = new LinkedList<>();
        int servicesOutbound = 0;

        Set<Relationship> results = new HashSet<>();

        for (Relationship graphRelationship : relationships) {
            if (isGoesTo(graphRelationship)) {
                TransportRelationship outgoing = relationshipFactory.getRelationship(graphRelationship);
                GoesToRelationship goesToRelationship = (GoesToRelationship) outgoing;
                servicesOutbound++;
                ServiceReason serviceReason = null;
                try {
                    serviceReason = serviceHeuristics.checkServiceHeuristics(incoming,goesToRelationship, path);
                } catch (TramchesterException e) {
                    logger.error("Unable to check service heuristics",e);
                }

                if (serviceReason==ServiceReason.IsValid) {
                    results.add(graphRelationship);
                } else {
                    servicesFilteredOut.add(serviceReason);
                }
            }
            else {
                // just add the relationship
                results.add(graphRelationship);
            }
        }

        // all filtered out
        if ((servicesOutbound>0) && (servicesFilteredOut.size()==servicesOutbound)) {
            try {
                TramNode currentNode = nodeFactory.getNode(endNode);
                reportFilterReasons(currentNode, servicesFilteredOut, incoming);
            } catch (TramchesterException e) {
                logger.error("Unable to find node " +endNode,e);
            }
        }

        return results;
    }

    private boolean isGoesTo(Relationship graphRelationship) {
        return graphRelationship.isType(TRAM_GOES_TO);
    }

    private void reportFilterReasons(TramNode currentNode,
                                     List<ServiceReason> servicesFilteredOut,
                                     TransportRelationship incoming) {
        if (servicesFilteredOut.size()==0) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(format("Filtered:%s all services for node:%s inbound:%s",
                    servicesFilteredOut.size(), currentNode, incoming));
            StringBuilder output = new StringBuilder();
            servicesFilteredOut.forEach(reason -> output.append(reason).append(" "));
            logger.debug(output.toString());
        }
    }

    @Override
    public PathExpander<GraphBranchState> reverse() {
        return this;
    }

}

