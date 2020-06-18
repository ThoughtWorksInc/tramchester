package com.tramchester.graph;

import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public interface NodeContentsRepository  {

    String getServiceId(Node node);
    TramTime getTime(Node node);
    int getHour(Node node);

    String getTrip(Relationship relationship);
    String getTrips(Relationship relationship);

    int getCost(Relationship lastRelationship);
    void deleteFromCostCache(Relationship relationship);
}