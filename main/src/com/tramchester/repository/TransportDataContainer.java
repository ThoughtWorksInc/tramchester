package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class TransportDataContainer implements TransportData, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataContainer.class);

    private final IdMap<Trip> trips = new IdMap<>();        // trip id -> trip
    private final IdMap<Station> stationsById = new IdMap<>();  // station id -> station
    private final IdMap<Service> services = new IdMap<>();  // service id -> service
    private final IdMap<Route> routes = new IdMap<>();      // route id -> route
    private final IdMap<Platform> platforms = new IdMap<>(); // platformId -> platform
    private final IdMap<RouteStation> routeStations = new IdMap<>(); // routeStationId - > RouteStation
    private final IdMap<Agency> agencies = new IdMap<>(); // agencyId -> agencies

    private final Map<String, Station> tramStationsByName = new HashMap<>();  // station id -> station
    private final Set<DataSourceInfo.NameAndVersion> nameAndVersions = new HashSet<>();
    private final Map<String, FeedInfo> feedInfoMap = new HashMap<>();

    @Override
    public void dispose() {
        trips.forEach(Trip::dispose);
        trips.clear();
        stationsById.clear();
        tramStationsByName.clear();
        services.clear();
        routes.clear();
        platforms.clear();
        routeStations.clear();
        agencies.clear();
        feedInfoMap.clear();
    }

    public void reportNumbers() {
        logger.info("From " + nameAndVersions.toString());
        logger.info(format("%s agencies", agencies.size()));
        logger.info(format("%s routes", routes.size()));
        logger.info(stationsById.size() + " stations " + platforms.size() + " platforms ");
        logger.info(format("%s route stations", routeStations.size()));
        logger.info(format("%s services", services.size()));
        logger.info(format("%s trips", trips.size()));
        logger.info(format("%s feedinfos", feedInfoMap.size()));
    }

    public Service getService(String serviceId) {
        return services.get(serviceId);
    }

    @Override
    public boolean hasStationId(String stationId) {
        return stationsById.hasId(stationId);
    }

    @Override
    public Station getStationById(String stationId) {
        if (!stationsById.hasId(stationId)) {
            String msg = "Unable to find station from ID " + stationId;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationsById.get(stationId);
    }

    @Override
    public Set<Station> getStations() {
        return stationsById.getValues();
    }

    @Override
    public Set<RouteStation> getRouteStations() {
        return routeStations.getValues();
    }

    @Override
    public RouteStation getRouteStationById(String routeStationId) {
        return routeStations.get(routeStationId);
    }

    @Override
    public Set<Service> getServices() {
        return services.getValues();
    }

    @Override
    public Set<Trip> getTrips() {
        return trips.getValues();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasRouteStationId(String routeStationId) {
        return routeStations.hasId(routeStationId);
    }

    public void addRouteStation(RouteStation routeStation) {
       routeStations.add(routeStation);
    }

    public boolean hasPlatformId(String platformId) {
        return platforms.hasId(platformId);
    }

    public Platform getPlatform(String platformId) {
        return platforms.get(platformId);
    }

    @Override
    public Set<Platform> getPlatforms() {
        return platforms.getValues();
    }

    @Override
    public Route getRouteById(String routeId) {
        return routes.get(routeId);
    }

    @Override
    public Set<Agency> getAgencies() {
        return agencies.getValues();
    }

    @Override
    public Service getServiceById(String serviceId) {
        return services.get(serviceId);
    }

    @Override
    public DataSourceInfo getDataSourceInfo() {
        return new DataSourceInfo(nameAndVersions);
    }

    @Override
    public boolean hasServiceId(String serviceId) {
        return services.hasId(serviceId);
    }

    public void addAgency(Agency agency) {
        agencies.add(agency);
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    public void addRouteToAgency(Agency agency, Route route) {
        agencies.get(agency.getId()).addRoute(route);
    }

    public void addStation(Station station) {
        stationsById.add(station);
        if (TransportMode.isTram(station)) {
            tramStationsByName.put(station.getName().toLowerCase(), station);
        }
    }

    public void addPlatform(Platform platform) {
        platforms.add(platform);
    }

    public void updateTimesForServices() {
        // Cannot do this until after all stops loaded into trips
        logger.info("Updating timings for services");
        services.forEach(Service::updateTimings);
    }

    public void addService(Service service) {
        services.add(service);
    }

    @Override
    public boolean hasTripId(String tripId) {
        return trips.hasId(tripId);
    }

    @Override
    public Trip getTripById(String tripId) {
        return trips.get(tripId);
    }

    @Override
    public Set<Route> getRoutes() {
        return routes.getValues();
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    @Override
    public Optional<Platform> getPlatformById(String platformId) {
        if (platforms.hasId(platformId)) {
            return Optional.of(platforms.get(platformId));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Station> getTramStationByName(String name) {
        String lowerCase = name.toLowerCase();
        if (tramStationsByName.containsKey(lowerCase)) {
            return Optional.of(tramStationsByName.get(lowerCase));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Set<Service> getServicesOnDate(TramServiceDate date) {
        return services.filter(item -> item.operatesOn(date.getDate()));
    }

    public boolean hasRouteId(String routeId) {
        return routes.hasId(routeId);
    }

    public void addNameAndVersion(DataSourceInfo.NameAndVersion nameAndVersion) {
        nameAndVersions.add(nameAndVersion);
    }

    @Override
    public Map<String, FeedInfo> getFeedInfos() {
        return feedInfoMap;
    }

    public void addFeedInfo(String name, FeedInfo feedInfo) {
        feedInfoMap.put(name, feedInfo);
    }

}