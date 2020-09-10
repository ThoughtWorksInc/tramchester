package com.tramchester.testSupport;

import com.tramchester.domain.*;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.StationRepository;
import org.opengis.referencing.operation.TransformException;

import java.util.List;
import java.util.Set;

public class TestStation extends Station {

    private final TransportMode mode;
    private boolean platformsAdded;
    private boolean routesAdded;

    public TestStation(String id, String area, String stationName, LatLong latLong, GridPosition gridPosition, TransportMode mode) {
        super(IdFor.createId(id), area, stationName, latLong, gridPosition);
        this.mode = mode;
        platformsAdded = false;
        routesAdded = false;
    }

    public static Station forTest(String id, String area, String stationName, LatLong latLong, TransportMode mode) throws TransformException {
        return new TestStation(id, area, stationName, latLong, CoordinateTransforms.getGridPosition(latLong), mode);
    }

    private void guardPlatformsAddedIntent() {
        if (!platformsAdded) {
            throw new RuntimeException("Use real Station");
        }
    }

    private void guardRoutesAddedIntent() {
        if (!routesAdded) {
            throw new RuntimeException("Use real Station");
        }
    }

    @Override
    public void addPlatform(Platform platform) {
        super.addPlatform(platform);
        platformsAdded = true;
    }

    @Override
    public void addRoute(Route route) {
        super.addRoute(route);
        routesAdded = true;
    }

    @Override
    public TransportMode getTransportMode() {
        return mode;
    }

    @Override
    public boolean hasPlatforms() {
        guardPlatformsAddedIntent();
        return super.hasPlatforms();
    }

    @Override
    public List<Platform> getPlatforms() {
        guardPlatformsAddedIntent();
        return super.getPlatforms();
    }

    @Override
    public List<Platform> getPlatformsForRoute(Route route) {
        throw new RuntimeException("Use real Station");
    }

    @Override
    public Set<Route> getRoutes() {
        guardRoutesAddedIntent();
        return super.getRoutes();
    }

    public static Station real(StationRepository repository, TestStations hasId) {
        return repository.getStationById(hasId.getId());
    }

}
