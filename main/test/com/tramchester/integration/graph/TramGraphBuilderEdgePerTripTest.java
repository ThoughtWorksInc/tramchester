package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Service;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.Nodes.ServiceNode;
import com.tramchester.graph.Relationships.*;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TramGraphBuilderEdgePerTripTest {
    private static Dependencies dependencies;

    private TransportDataFromFiles transportData;
    private Transaction transaction;
    private GraphQuery graphQuery;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @Before
    public void beforeEachTestRuns() {
        graphQuery = dependencies.get(GraphQuery.class);
        transportData = dependencies.get(TransportDataFromFiles.class);
        GraphDatabaseService service = dependencies.get(GraphDatabaseService.class);
        transaction = service.beginTx();
    }

    @After
    public void afterEachTestRuns() {
        transaction.close();
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldHaveCorrectOutboundsAtMediaCity() {

        String mediaCityUKId = Stations.MediaCityUK.getId();
        List<TransportRelationship> outbounds = getOutboundRouteStationRelationships(
                mediaCityUKId + RouteCodesForTesting.ECCLES_TO_ASH );
        outbounds.addAll(getOutboundRouteStationRelationships(
                mediaCityUKId + RouteCodesForTesting.ASH_TO_ECCLES ));

        List<ServiceRelationship> graphServicesRelationships = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof ServiceRelationship) graphServicesRelationships.add((ServiceRelationship) out);
        });

        Set<String> graphSvcIds = graphServicesRelationships.stream().map(ServiceRelationship::getServiceId).collect(Collectors.toSet());

        // check number of outbound services matches services in transport data files
        Set<String> fileSvcIds = transportData.getTripsFor(mediaCityUKId).stream().
                filter(trip -> transportData.getServiceById(trip.getServiceId()).isRunning())
                .map(Trip::getServiceId).
                collect(Collectors.toSet());
        fileSvcIds.removeAll(graphSvcIds);

        assertEquals(0, fileSvcIds.size());
    }

    private List<TransportRelationship> getOutboundRouteStationRelationships(String routeStationId) {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }

    @Test
    public void shouldRepdroduceIssueWithWeekendsAtDeansgateToAshton() {

        List<TransportRelationship> outbounds = getOutboundRouteStationRelationships(Stations.Deansgate.getId()
                + RouteCodesForTesting.ECCLES_TO_ASH);
        List<ServiceNode> serviceNodes = outbounds.stream().filter(TransportRelationship::isServiceLink).
                map(relationship -> (ServiceNode)relationship.getEndNode()).collect(Collectors.toList());

        List<ServiceNode> sundays = serviceNodes.stream().filter(node -> node.getDaysServiceRuns()[6]).collect(Collectors.toList());
        assertTrue(sundays.size()>0);
    }

    @Test
    public void shouldRepdroduceIssueWithWeekendsAtMediaCity() {

        String mediaCityUKId = Stations.MediaCityUK.getId();
        Station mediaCity = transportData.getStation(mediaCityUKId).get();
        List<ServiceNode> serviceNodes = new ArrayList<>();

        for(String route : mediaCity.getRoutes()) {
            serviceNodes.addAll(getOutboundRouteStationRelationships(mediaCityUKId + route).stream().
                    filter(TransportRelationship::isServiceLink).
                    map(relationship -> (ServiceNode)relationship.getEndNode()).
                    collect(Collectors.toList()));
        }

        // 6 == index of sunday in array
        List<ServiceNode> sundays = serviceNodes.stream().filter(node -> node.getDaysServiceRuns()[6]).collect(Collectors.toList());
        assertTrue(sundays.size()>0);

        Set<String> callingServices = transportData.getTripsFor(mediaCityUKId).stream().map(Trip::getServiceId).collect(Collectors.toSet());
        Set<Service> services = callingServices.stream().
                map(id -> transportData.getServiceById(id)).
                filter(Service::isRunning).
                filter(service -> service.getDays().get(DaysOfWeek.Sunday)).
                collect(Collectors.toSet());

        assertEquals(services.size(), sundays.size());

    }

    @Test
    public void shouldHaveCorrectRelationshipsAtCornbrook() {

        List<TransportRelationship> outbounds = getOutboundRouteStationRelationships(Stations.Cornbrook.getId()
                + RouteCodesForTesting.ALTY_TO_BURY);

        assertTrue(outbounds.size()>1);

        outbounds = getOutboundRouteStationRelationships(Stations.Cornbrook.getId()
                + RouteCodesForTesting.ASH_TO_ECCLES);

        assertTrue(outbounds.size()>1);

    }

    @Test
    public void shouldHaveCorrectInboundsAtMediaCity() {

        checkInboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ASH_TO_ECCLES);

        checkInboundConsistency(Stations.HarbourCity.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);

        checkInboundConsistency(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.Broadway.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    @Test
    public void shouldCheckOutboundSvcRelationships() {

        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.ALTY_TO_BURY);
        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.BURY_TO_ALTY);

        checkOutboundConsistency(Stations.Cornbrook.getId(), RouteCodesForTesting.BURY_TO_ALTY);
        checkOutboundConsistency(Stations.Cornbrook.getId(), RouteCodesForTesting.ALTY_TO_BURY);

        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.ECCLES_TO_ASH);

        checkOutboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
        checkOutboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ECCLES_TO_ASH);

        // consistent heading away from Media City ONLY, see below
        checkOutboundConsistency(Stations.HarbourCity.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkOutboundConsistency(Stations.Broadway.getId(), RouteCodesForTesting.ASH_TO_ECCLES);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    @Test
    public void shouldHaveCorrectGraphRelationshipsFromVeloparkNodeMonday8Am() {

        List<TransportRelationship> outbounds = getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodesForTesting.ASH_TO_ECCLES);

        List<ServiceRelationship> svcRelationshipsFromVeloPark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof ServiceRelationship) svcRelationshipsFromVeloPark.add((ServiceRelationship) out);
        });
        // filter by day and then direction/route
        assertFalse(svcRelationshipsFromVeloPark.isEmpty());
        List<ServiceNode> serviceNodes = svcRelationshipsFromVeloPark.stream().
                map(relationship -> (ServiceNode) relationship.getEndNode()).collect(Collectors.toList());
        serviceNodes.removeIf(svc -> !svc.getDaysServiceRuns()[0]); // monday
        assertFalse(serviceNodes.isEmpty());
        svcRelationshipsFromVeloPark.removeIf(svc -> !transportData.getServiceById(
                svc.getServiceId()).getRouteId().equals(RouteCodesForTesting.ASH_TO_ECCLES));

        assertFalse(svcRelationshipsFromVeloPark.isEmpty());

    }

    private void checkOutboundConsistency(String stationId, String routeId) {
        List<TransportRelationship> graphOutbounds = getOutboundRouteStationRelationships(
                stationId + routeId);

        assertTrue(graphOutbounds.size()>0);

        List<String> serviceRelatIds = graphOutbounds.stream().
                filter(TransportRelationship::isServiceLink).
                map(relationship -> (ServiceRelationship) relationship).
                map(ServiceRelationship::getServiceId).
                collect(Collectors.toList());

        Set<Trip> fileCallingTrips = transportData.getServices().stream().
                filter(svc -> svc.getRouteId().equals(routeId)).
                filter(Service::isRunning).
                map(Service::getTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.callsAt(stationId)).
                collect(Collectors.toSet());

        Set<String> fileSvcIdFromTrips = fileCallingTrips.stream().
                map(Trip::getServiceId).
                collect(Collectors.toSet());

        // each svc should be one outbound, no dups, so use list not set of ids
        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size());
        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));

    }

    private void checkInboundConsistency(String stationId, String routeId) {
        List<TransportRelationship> inbounds = graphQuery.getRouteStationRelationships(stationId + routeId, Direction.INCOMING);

        List<BoardRelationship> graphBoardAtStation = new LinkedList<>();
        List<TramGoesToRelationship> graphTramsIntoStation = new LinkedList<>();
        inbounds.forEach(in -> {
            if (in instanceof BoardRelationship) graphBoardAtStation.add((BoardRelationship) in);
            if (in instanceof TramGoesToRelationship) graphTramsIntoStation.add((TramGoesToRelationship) in);
        });
        assertEquals(1, graphBoardAtStation.size());

        SortedSet<String> graphInboundSvcIds = new TreeSet<>();
        graphInboundSvcIds.addAll(graphTramsIntoStation.stream().
                map(GoesToRelationship::getServiceId).
                collect(Collectors.toSet()));

        Set<Trip> callingTrips = transportData.getServices().stream().
                filter(svc -> svc.isRunning()).
                filter(svc -> svc.getRouteId().equals(routeId)).
                map(Service::getTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.callsAt(stationId)). // calls at , but not starts at because no inbound for these
                filter(trip -> !trip.getStops().get(0).getStation().getId().equals(stationId)).
                collect(Collectors.toSet());

        SortedSet<String> svcIdsFromCallingTrips = new TreeSet<>();
        svcIdsFromCallingTrips.addAll(callingTrips.stream().
                map(Trip::getServiceId).
                collect(Collectors.toSet()));

        assertEquals(svcIdsFromCallingTrips, graphInboundSvcIds);

        Set<String> graphInboundTripIds = graphTramsIntoStation.stream().map(GoesToRelationship::getTripId).collect(Collectors.toSet());

        assertEquals(graphTramsIntoStation.size(), graphInboundTripIds.size()); // should have an inbound link per trip

        Set<String> tripIdsFromFile = callingTrips.stream().map(Trip::getTripId).collect(Collectors.toSet());

        tripIdsFromFile.removeAll(graphInboundTripIds);
        assertEquals(0, tripIdsFromFile.size());
    }
}
