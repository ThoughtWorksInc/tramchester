package com.tramchester.domain;

import com.tramchester.domain.presentation.ServiceTime;
import org.junit.Test;

import java.time.LocalTime;
import java.util.SortedSet;

import static org.junit.Assert.assertEquals;

public class TripTest {

    @Test
    public void shouldModelCircularTripsCorrectly() {
        Trip trip = new Trip("tripId","headSign", "svcId");

        Station stationA = new Station("statA","areaA", "stopNameA", 1.0, -1.0, false);
        Station stationB = new Station("statB","areaA", "stopNameB", 2.0, -2.0, false);

        Stop firstStop = new Stop(stationA, LocalTime.of(10, 00), LocalTime.of(10, 01));
        Stop secondStop = new Stop(stationB, LocalTime.of(10, 05), LocalTime.of(10, 06));
        Stop thirdStop = new Stop(stationA, LocalTime.of(10, 10), LocalTime.of(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        int am10Minutes = 10 * 60;
        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(am10Minutes, 30));

        // service times
        assertEquals(1, times.size());
        ServiceTime time = times.first();
        assertEquals(LocalTime.of(10, 01), time.getDepartureTime());
        assertEquals(LocalTime.of(10, 05), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes+1, time.getFromMidnightLeaves());

        // services times
        times = trip.getServiceTimes("statB", "statA", new TimeWindow(am10Minutes, 30));
        assertEquals(1, times.size());
        time = times.first();
        assertEquals(LocalTime.of(10, 06), time.getDepartureTime());
        assertEquals(LocalTime.of(10, 10), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes+6, time.getFromMidnightLeaves());

        // earliest departs
        assertEquals(am10Minutes+1, trip.earliestDepartFor("statA","statB", new TimeWindow(am10Minutes, 30)));
        assertEquals(am10Minutes+6, trip.earliestDepartFor("statB","statA", new TimeWindow(am10Minutes, 30)));
        assertEquals(am10Minutes+1, trip.earliestDepartFor("statA","statA", new TimeWindow(am10Minutes, 30)));
    }
}