package com.tramchester.dataimport;


import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;

import java.io.IOException;
import java.util.List;

public class TransportDataReader {
    private final String path;

    public TransportDataReader(String path) {
        this.path = path;
    }

    public List<CalendarData> getCalendar() throws IOException {
        return new DataLoader<>(path + "calendar", new CalendarDataParser()).loadAll();
    }

    public List<StopTimeData> getStopTimes() throws IOException {
        return new DataLoader<>(path + "stop_times", new StopTimeDataParser()).loadAll();
    }

    public List<TripData> getTrips() throws IOException {
        return new DataLoader<>(path + "trips", new TripDataParser()).loadAll();
    }

    public List<StopData> getStops() throws IOException {
        return new DataLoader<>(path + "stops", new StopDataParser()).loadAll();
    }

    public List<RouteData> getRoutes() throws IOException {
        return new DataLoader<>(path + "routes", new RouteDataParser()).loadAll();
    }
}