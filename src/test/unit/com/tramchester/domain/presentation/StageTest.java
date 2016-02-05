package com.tramchester.domain.presentation;


import com.tramchester.domain.RawTravelStage;
import com.tramchester.domain.Station;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import org.junit.Test;

import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import static junit.framework.TestCase.assertEquals;

public class StageTest {

    private String tripId = "tripId";
    private int elapsedTime = 101;
    Station firstStation = new Station("firstStation", "area", "name", 1,1, true);

    @Test
    public void shouldGetDurationCorrectly() {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(8, 00), LocalTime.of(9, 15), "svcId", "headsign", tripId);
        Stage stage = new Stage(new RawTravelStage(firstStation, "route", TransportMode.Tram, "cssClass", elapsedTime), createSet(serviceTime));

        assertEquals(75, stage.getDuration());
    }

    @Test
    public void shouldGetFirstDepartureAndFirstArrival() {
        ServiceTime serviceTimeA = new ServiceTime(LocalTime.of(8, 00), LocalTime.of(9, 15), "svcId", "headsign", tripId);
        ServiceTime serviceTimeB = new ServiceTime(LocalTime.of(7, 00), LocalTime.of(7, 45), "svcId", "headsign", tripId);

        Stage stage = new Stage(new RawTravelStage(firstStation, "route", TransportMode.Tram, "cssClass", elapsedTime),
                createSet(serviceTimeA,serviceTimeB));

        assertEquals(LocalTime.of(7, 00), stage.getFirstDepartureTime());
        assertEquals(LocalTime.of(7, 45), stage.getExpectedArrivalTime());
    }

    private SortedSet<ServiceTime> createSet(ServiceTime... times) {
        SortedSet sortedSet = new TreeSet<ServiceTime>();
        for (ServiceTime time : times) {
            sortedSet.add(time);
        }
        return sortedSet;
    }

    @Test
    public void shouldGetEarliestDepartCorrectly() {
        ServiceTime serviceTimeA = new ServiceTime(LocalTime.of(8, 00), LocalTime.of(9, 15), "svcId", "headsign", tripId);
        ServiceTime serviceTimeB = new ServiceTime(LocalTime.of(7, 00), LocalTime.of(7, 45), "svcId", "headsign", tripId);

        Stage stage = new Stage(new RawTravelStage(firstStation, "route", TransportMode.Tram, "cssClass", elapsedTime),
                createSet(serviceTimeA,serviceTimeB));

        assertEquals(7*60, stage.findEarliestDepartureTime());
    }

    @Test
    public void shouldGetDepartForEarliestArrivalCorrectly() {
        ServiceTime leavesFirst = new ServiceTime(LocalTime.of(7, 00), LocalTime.of(9, 15), "svcId", "headsign",
                tripId);
        ServiceTime arrivesFirst = new ServiceTime(LocalTime.of(7, 10), LocalTime.of(9, 00), "svcId", "headsign",
                tripId);

        Stage stage = new Stage(new RawTravelStage(firstStation, "route", TransportMode.Tram, "cssClass", elapsedTime),
                createSet(leavesFirst,arrivesFirst));

        assertEquals(LocalTime.of(7,10), stage.getFirstDepartureTime());
    }

    @Test
    public void shouldGetDurationCorrectlyWhenAfterMidnight() {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(23, 50), LocalTime.of(0, 15), "svcId", "headsign", tripId);
        Stage stage = new Stage(new RawTravelStage(firstStation, "route", TransportMode.Tram, "cssClass", elapsedTime), createSet(serviceTime));

        assertEquals(25, stage.getDuration());
    }

    @Test
    public void shouldGetAttributesPassedIn() {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(23, 50), LocalTime.of(0, 15), "svcId", "headsign", tripId);

        RawTravelStage rawTravelStage = new RawTravelStage(firstStation, "route", TransportMode.Tram, "cssClass", elapsedTime);
        rawTravelStage.setLastStation(new Station("lastStation", "area", "name", -1,-1, true));
        rawTravelStage.setServiceId("svcId");
        Stage stage = new Stage(rawTravelStage, createSet(serviceTime));
        assertEquals("cssClass", stage.getDisplayClass());
        assertEquals(TransportMode.Tram, stage.getMode());
        assertEquals("route", stage.getRoute());
        assertEquals("firstStation", stage.getFirstStation());
        assertEquals("lastStation", stage.getLastStation());
        assertEquals("svcId", stage.getServiceId());
    }

    @Test
    public void shouldDisplayCorrectPrompt() {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(23, 50), LocalTime.of(0, 15), "svcId", "headsign", tripId);
        RawTravelStage rawTravelStage = new RawTravelStage(firstStation, "route", TransportMode.Tram, "cssClass", elapsedTime);

        Stage stage = new Stage(rawTravelStage, createSet(serviceTime));

        assertEquals(stage.getPrompt(),"Walk to");
    }

    @Test
    public void shouldGetStageSummary() throws TramchesterException {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(23, 50), LocalTime.of(0, 15), "svcId", "headsign", tripId);

        RawTravelStage rawTravelStageA = new RawTravelStage(firstStation, "routeName", TransportMode.Tram, "cssClass", elapsedTime);
        Stage stageA = new Stage(rawTravelStageA, createSet(serviceTime));
        String result = stageA.getSummary();

        assertEquals(result, "routeName Tram line");

        RawTravelStage rawTravelStageB = new RawTravelStage(firstStation, "routeName", TransportMode.Bus, "cssClass", elapsedTime);
        Stage stageB = new Stage(rawTravelStageB, createSet(serviceTime));
        result = stageB.getSummary();

        assertEquals(result, "routeName Bus route");
    }
}
