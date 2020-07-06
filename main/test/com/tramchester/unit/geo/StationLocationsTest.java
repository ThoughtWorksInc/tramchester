package com.tramchester.unit.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StationLocationsTest {

    private StationLocations stationLocations;

    @BeforeEach
    void onceBeforeEachTest() {
        CoordinateTransforms coordinateTransforms = new CoordinateTransforms();
        stationLocations = new StationLocations(coordinateTransforms);
    }

    @Test
    void shouldHaveGridPositionBehaviours() {
        GridPosition gridPositionA = new GridPosition(3,4);
        assertEquals(3, gridPositionA.getEastings());
        assertEquals(4, gridPositionA.getNorthings());

        GridPosition origin = new GridPosition(0,0);
        assertEquals(5, GridPosition.distanceTo(origin, gridPositionA));
        assertEquals(5, GridPosition.distanceTo(gridPositionA, origin));

        assertFalse(GridPosition.withinDistEasting(origin, gridPositionA, 2));
        assertTrue(GridPosition.withinDistEasting(origin, gridPositionA, 3));
        assertTrue(GridPosition.withinDistEasting(origin, gridPositionA, 4));

        assertFalse(GridPosition.withinDistNorthing(origin, gridPositionA, 2));
        assertTrue(GridPosition.withinDistNorthing(origin, gridPositionA, 4));
        assertTrue(GridPosition.withinDistNorthing(origin, gridPositionA, 5));
    }

    @Test
    void shouldGetLatLongForStation() throws TransformException {
        Station stationA = new Station("id456", "area", "nameB", TestEnv.nearPiccGardens, true);
        Station stationB = new Station("id789", "area", "nameC", TestEnv.nearShudehill, true);
        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);

        LatLong resultA = stationLocations.getStationPosition(stationA);
        assertEquals(TestEnv.nearPiccGardens.getLat(), resultA.getLat(), 0.00001);
        assertEquals(TestEnv.nearPiccGardens.getLon(), resultA.getLon(), 0.00001);

        LatLong resultB = stationLocations.getStationPosition(stationB);
        assertEquals(TestEnv.nearShudehill.getLat(), resultB.getLat(), 0.00001);
        assertEquals(TestEnv.nearShudehill.getLon(), resultB.getLon(), 0.00001);

    }

    @Test
    void shouldFindNearbyStation() {
        LatLong place = TestEnv.nearAltrincham;
        Station stationA = new Station("id123", "area", "nameA", place, true);
        Station stationB = new Station("id456", "area", "nameB", TestEnv.nearPiccGardens, true);
        Station stationC = new Station("id789", "area", "nameC", TestEnv.nearShudehill, true);
        LatLong closePlace = new LatLong(place.getLat()+0.008, place.getLon()+0.008);
        Station stationD = new Station("idABC", "area", "name", closePlace, true);

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);
        stationLocations.addStation(stationD);

        HasGridPosition gridA = stationLocations.getStationGridPosition(stationA);
        HasGridPosition gridB = stationLocations.getStationGridPosition(stationD);

        int rangeInKM = 1;

        // validate within range on crude measure, but out of range on calculated position
        assertTrue(GridPosition.withinDistNorthing(gridA, gridB, 1000));
        assertTrue(GridPosition.withinDistEasting(gridA, gridB, 1000));
        long distance = GridPosition.distanceTo(gridA, gridB);
        assertTrue(distance > Math.round(rangeInKM*1000) );

        List<Station> results = stationLocations.nearestStationsSorted(place, 3, rangeInKM);

        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    void shouldOrderClosestFirst() {
        Station stationA = new Station("id123", "area", "nameA", TestEnv.nearAltrincham, true);
        Station stationB = new Station("id456", "area", "nameB", TestEnv.nearPiccGardens, true);
        Station stationC = new Station("id789", "area", "nameC", TestEnv.nearShudehill, true);

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);

        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 3, 20);
        assertEquals(3, results.size());
        assertEquals(stationA, results.get(0));
        assertEquals(stationB, results.get(1));
        assertEquals(stationC, results.get(2));
    }

    @Test
    void shouldRespectLimitOnNumberResults() {
        Station stationA = new Station("id123", "area", "nameA", TestEnv.nearAltrincham, true);
        Station stationB = new Station("id456", "area", "nameB", TestEnv.nearPiccGardens, true);
        Station stationC = new Station("id789", "area", "nameC", TestEnv.nearShudehill, true);

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);

        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 1, 20);
        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    void shouldFindNearbyStationRespectingRange() {
        Station testStation = new Station("id123", "area", "name", TestEnv.nearAltrincham, true);
        stationLocations.addStation(testStation);

        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearPiccGardens, 3, 1);
        assertEquals(0, results.size());

        List<Station> further = stationLocations.nearestStationsSorted(TestEnv.nearPiccGardens, 3, 20);
        assertEquals(1, further.size());
        assertEquals(testStation, further.get(0));
    }

    @Test
    void shouldCaptureBoundingAreaForStations() {
        Station testStationA = new Station("id123", "area", "name", TestEnv.nearAltrincham, true);
        Station testStationB = new Station("id456", "area", "name", TestEnv.nearShudehill, true);
        Station testStationC = new Station("id789", "area", "nameB", TestEnv.nearPiccGardens, true);

        stationLocations.addStation(testStationA);
        stationLocations.addStation(testStationB);
        stationLocations.addStation(testStationC);

        HasGridPosition posA = stationLocations.getStationGridPosition(testStationA);
        HasGridPosition posB = stationLocations.getStationGridPosition(testStationB);

        BoundingBox bounds = stationLocations.getBounds();

        // bottom left
        assertEquals(posA.getEastings(), bounds.getMinEastings());
        assertEquals(posA.getNorthings(), bounds.getMinNorthings());
        // top right
        assertEquals(posB.getEastings(), bounds.getMaxEasting());
        assertEquals(posB.getNorthings(), bounds.getMaxNorthings());
    }
}
