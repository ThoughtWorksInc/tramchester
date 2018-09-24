package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.SortedSet;

import static org.junit.Assert.assertEquals;

public class TripTest {

    @Test
    public void shouldModelCircularTripsCorrectly() throws TramchesterException {
        Trip trip = new Trip("tripId","headSign", "svcId", "routeId");

        Location stationA = new Station("statA","areaA", "stopNameA", new LatLong(1.0, -1.0), false);
        Location stationB = new Station("statB","areaA", "stopNameB", new LatLong(2.0, -2.0), false);

        String routeId = "routeId";
        String serviceId = "serviceId";

        Stop firstStop = new Stop("statA1", stationA, TramTime.create(10, 00), TramTime.create(10, 01), routeId, serviceId);
        Stop secondStop = new Stop("statB1", stationB, TramTime.create(10, 05), TramTime.create(10, 06), routeId, serviceId);
        Stop thirdStop = new Stop("statA1", stationA, TramTime.create(10, 10), TramTime.create(10, 10), routeId, serviceId);

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        int am10Minutes = 10 * 60;
        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(am10Minutes, 30));

        // service times
        assertEquals(1, times.size());
        ServiceTime time = times.first();
        assertEquals(TramTime.create(10, 01), time.getDepartureTime());
        assertEquals(TramTime.create(10, 05), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes+1, time.getFromMidnightLeaves());

        // services times
        times = trip.getServiceTimes("statB", "statA", new TimeWindow(am10Minutes, 30));
        assertEquals(1, times.size());
        time = times.first();
        assertEquals(TramTime.create(10, 06), time.getDepartureTime());
        assertEquals(TramTime.create(10, 10), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes+6, time.getFromMidnightLeaves());

        // earliest departs
        assertEquals(am10Minutes+1, trip.earliestDepartFor("statA","statB", new TimeWindow(am10Minutes, 30)).get().getFromMidnightLeaves());
        assertEquals(am10Minutes+6, trip.earliestDepartFor("statB","statA", new TimeWindow(am10Minutes, 30)).get().getFromMidnightLeaves());
        assertEquals(am10Minutes+1, trip.earliestDepartFor("statA","statA", new TimeWindow(am10Minutes, 30)).get().getFromMidnightLeaves());
    }
}
