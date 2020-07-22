package com.tramchester.repository;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.FeedInfo;

import java.util.stream.Stream;

public class TransportDataStreams {
    final Stream<StopData> stops;
    final Stream<RouteData> routes;
    final Stream<TripData> trips;
    final Stream<StopTimeData> stopTimes;
    final Stream<CalendarData> calendars;
    final Stream<FeedInfo> feedInfo;
    final Stream<CalendarDateData> calendarsDates;
    private final DataSourceConfig config;
    final Stream<AgencyData> agencies;
    final private DataSourceInfo.NameAndVersion nameAndVersion;

    public TransportDataStreams(DataSourceInfo.NameAndVersion nameAndVersion, Stream<AgencyData> agencies, Stream<StopData> stops,
                                Stream<RouteData> routes, Stream<TripData> trips, Stream<StopTimeData> stopTimes,
                                Stream<CalendarData> calendars,
                                Stream<FeedInfo> feedInfo, Stream<CalendarDateData> calendarsDates,
                                DataSourceConfig config) {
        this.nameAndVersion = nameAndVersion;
        this.agencies = agencies;
        this.stops = stops;
        this.routes = routes;
        this.trips = trips;
        this.stopTimes = stopTimes;
        this.calendars = calendars;
        this.feedInfo = feedInfo;
        this.calendarsDates = calendarsDates;
        this.config = config;
    }

    public void closeAll() {
        stops.close();
        routes.close();
        trips.close();
        stopTimes.close();
        calendars.close();
        feedInfo.close();
        calendarsDates.close();
        agencies.close();
    }

    public DataSourceInfo.NameAndVersion getNameAndVersion() {
        return nameAndVersion;
    }

    public DataSourceConfig getConfig() {
        return config;
    }
}