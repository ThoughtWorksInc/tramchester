package com.tramchester.domain.input;

import com.tramchester.domain.Location;
import com.tramchester.domain.TramTime;

import java.time.LocalTime;

public class Stop {
    private final Location station;
    private final TramTime arrivalTime;
    private final TramTime departureTime;
    private final String stopId;
    private final String routeId;
    private final String serviceId;

    public Stop(String stopId, Location station, TramTime arrivalTime, TramTime departureTime, String routeId, String serviceId) {

        this.stopId = stopId.intern();
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.station = station;
        this.routeId = routeId.intern();
        this.serviceId = serviceId.intern();
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartureTime() {
        return departureTime;
    }

    public Location getStation() {
        return station;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "station=" + station +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", stopId='" + stopId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", routeId='" + routeId + '\'' +
                '}';
    }

    @Deprecated
    public int getDepartureMinFromMidnight() {
        return departureTime.minutesOfDay();
    }

    @Deprecated
    public int getArriveMinsFromMidnight() {
        return arrivalTime.minutesOfDay();
    }

    public String getId() {
        return stopId;
    }
}
