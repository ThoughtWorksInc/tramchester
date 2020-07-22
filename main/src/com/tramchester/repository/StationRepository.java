package com.tramchester.repository;

import com.tramchester.domain.HasId;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.Optional;
import java.util.Set;

public interface StationRepository {
    Set<Station> getStations();
    Station getStationById(IdFor<Station> stationId);
    boolean hasStationId(IdFor<Station> stationId);

    // live data association
    Optional<Station> getTramStationByName(String name);

    Set<RouteStation> getRouteStations();

    RouteStation getRouteStationById(IdFor<RouteStation> routeStationId);
    RouteStation getRouteStationById(Station startStation, Route route);
}
