package com.tramchester.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class StationLocations implements StationLocationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);
    private final CoordinateTransforms coordinateTransforms;

    private long minEastings;
    private long maxEasting;
    private long minNorthings;
    private long maxNorthings;

    private final HashMap<Station, HasGridPosition> positions;

    public StationLocations(CoordinateTransforms coordinateTransforms) {
        this.coordinateTransforms = coordinateTransforms;
        positions = new HashMap<>();

        // bounding box for all stations
        minEastings = Long.MAX_VALUE;
        maxEasting = Long.MIN_VALUE;
        minNorthings = Long.MAX_VALUE;
        maxNorthings = Long.MIN_VALUE;
    }

    public void addStation(Station station) {
        LatLong position = station.getLatLong();
        try {
            HasGridPosition gridPosition = coordinateTransforms.getGridPosition(position);
            positions.put(station, gridPosition);
            updateBoundingBox(gridPosition);
            logger.debug("Added station " + station.getId() + " at grid " + gridPosition);
        } catch (TransformException e) {
            logger.error("Unable to store station as cannot convert location", e);
        }
    }

    @Override
    public LatLong getStationPosition(Station station) throws TransformException {
        HasGridPosition gridPostition = positions.get(station);
        return coordinateTransforms.getLatLong(gridPostition.getEastings(), gridPostition.getNorthings());
    }

    private void updateBoundingBox(HasGridPosition gridPosition) {
        long eastings = gridPosition.getEastings();
        long northings = gridPosition.getNorthings();

        if (eastings < minEastings) {
            minEastings = eastings;
        }
        if (eastings > maxEasting) {
            maxEasting = eastings;
        }
        if (northings < minNorthings) {
            minNorthings = northings;
        }
        if (northings > maxNorthings) {
            maxNorthings = northings;
        }
    }

    @Override
    public HasGridPosition getStationGridPosition(Station station) {
        return positions.get(station);
    }

    @Override
    public List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, double rangeInKM) {
        try {
            HasGridPosition gridPosition = coordinateTransforms.getGridPosition(latLong);
            return nearestStationsSorted(gridPosition, maxToFind, rangeInKM);
        } catch (TransformException e) {
            logger.error("Unable to convert latlong to grid position", e);
            return Collections.emptyList();
        }
    }

    @Override
    @NotNull
    public List<Station> nearestStationsSorted(@NotNull HasGridPosition gridPosition, int maxToFind, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);

        Stream<Map.Entry<Station, HasGridPosition>> unsorted = getNearbyStream(gridPosition, rangeInMeters);

        @NotNull List<Station> stationList;
        if (maxToFind > 1) {
            // only sort if more than one, as sorting expensive
            stationList = unsorted.sorted((a, b) -> compareDistances(gridPosition, a.getValue(), b.getValue())).
                    limit(maxToFind).
                    map(Map.Entry::getKey).collect(Collectors.toList());
            logger.debug(format("Found %s stations within %s meters of grid %s", stationList.size(), rangeInMeters, gridPosition));
        } else {
            stationList = unsorted.limit(maxToFind).map(Map.Entry::getKey).
                    collect(Collectors.toList());
        }

        return stationList;
    }

    @Override
    public Set<Station> nearestStationsUnsorted(Station station, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);
        return getNearbyStream(getStationGridPosition(station), rangeInMeters).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public List<Station> getNearestStationsTo(LatLong latLong, int numberOfNearest, double rangeInKM) {
        List<Station> result = nearestStationsSorted(latLong, numberOfNearest, rangeInKM);
        logger.info(format("Found %s stations close to %s", result.size(), latLong));
        return result;
    }

    @NotNull
    private Stream<Map.Entry<Station, HasGridPosition>> getNearbyStream(@NotNull HasGridPosition otherPosition, long rangeInMeters) {
        return getNearbyStreamSquare(otherPosition, rangeInMeters).
                        filter(entry -> GridPositions.withinDist(otherPosition, entry.getValue(), rangeInMeters));
    }

    @NotNull
    private Stream<Map.Entry<Station, HasGridPosition>> getNearbyStreamSquare(@NotNull HasGridPosition otherPosition, long rangeInMeters) {
        return positions.entrySet().stream().
                // crude filter initially
                        filter(entry -> GridPositions.withinDistEasting(otherPosition, entry.getValue(), rangeInMeters)).
                        filter(entry -> GridPositions.withinDistNorthing(otherPosition, entry.getValue(), rangeInMeters));
    }

    private int compareDistances(HasGridPosition origin, HasGridPosition first, HasGridPosition second) {
        long firstDist = GridPositions.distanceTo(origin, first);
        long secondDist = GridPositions.distanceTo(origin, second);
        return Long.compare(firstDist, secondDist);
    }

    public BoundingBox getBounds() {
        return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
    }

    public boolean hasAnyNearby(HasGridPosition hasGridPosition, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);
        return getNearbyStream(hasGridPosition, rangeInMeters).findAny().isPresent();
    }

    public Stream<BoundingBoxWithStations> getGroupedStations(long gridSize) {
        if (gridSize <= 0) {
            throw new RuntimeException("Invalid grid size of " + gridSize);
        }

        logger.info("Getting groupded stations for grid size " + gridSize);

        List<BoundingBox> boxes = new ArrayList<>();
        for (long x = minEastings; x <= maxEasting; x = x + gridSize) {
            for (long y = minNorthings; y <= maxNorthings; y = y + gridSize) {
                BoundingBox box = new BoundingBox(x, y, x + gridSize, y + gridSize);
                boxes.add(box);
            }
        }

        return boxes.stream().map(box -> new BoundingBoxWithStations(box, getStationsWithin(box))).
                filter(BoundingBoxWithStations::hasStations);
    }

    private Set<Station> getStationsWithin(BoundingBox box) {
        // TODO need more efficient way to do this?
        return positions.entrySet().stream().
                filter(entry -> box.contained(entry.getValue())).
                map(Map.Entry::getKey).
                collect(Collectors.toSet());
    }

}
