package com.tramchester.dataimport.datacleanse;

import com.tramchester.Dependencies;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.TransportDataFetcher;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.services.DateTimeService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


public class DataCleanser {
    private static final Logger logger = LoggerFactory.getLogger(DataCleanser.class);
    public static final String DATE_FORMAT = "YYYMMdd";

    private TransportDataReader transportDataReader;
    private TransportDataWriterFactory transportDataWriterFactory;
    private TransportDataFetcher fetcher;

    public static void main(String[] args) throws Exception {
        String path = "data/tram/";
        TransportDataReader reader = new TransportDataReader(path + "gtdf-out/");
        TransportDataWriterFactory writer = new TransportDataWriterFactory(path);
        FetchDataFromUrl fetcher = new FetchDataFromUrl(path, "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip");

        DataCleanser dataCleanser = new DataCleanser(fetcher, reader, writer);
        dataCleanser.run(Arrays.asList(Dependencies.METROLINK));

        FileUtils.deleteDirectory(new File(path + "/gtdf-out/"));
        FileUtils.forceDelete(new File(path + "/data.zip"));
    }

    public DataCleanser(TransportDataFetcher fetcher, TransportDataReader reader, TransportDataWriterFactory factory) {
        this.fetcher = fetcher;
        this.transportDataReader = reader;
        this.transportDataWriterFactory = factory;
    }

    public void run(List<String> agencies) throws IOException {
        fetcher.fetchData();

        List<String> routeCodes = cleanseRoutes(agencies);

        ServicesAndTrips servicesAndTrips = cleanseTrips(routeCodes);

        Set<String> stopIds = cleanseStoptimes(servicesAndTrips.getTripIds());

        cleanseStops(stopIds);

        cleanseCalendar(servicesAndTrips.getServiceIds());

        cleanFeedInfo();

    }

    public void cleanseCalendar(Set<String> services) throws IOException {
        logger.info("**** Start cleansing calendar.");

        Stream<CalendarData> calendar = transportDataReader.getCalendar();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("calendar");
        calendar.filter(calendarData -> services.contains(calendarData.getServiceId()) && calendarData.runsAtLeastADay())
                .forEach(calendarData -> writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        calendarData.getServiceId(),
                        runsOnDay(calendarData.isMonday()),
                        runsOnDay(calendarData.isTuesday()),
                        runsOnDay(calendarData.isWednesday()),
                        runsOnDay(calendarData.isThursday()),
                        runsOnDay(calendarData.isFriday()),
                        runsOnDay(calendarData.isSaturday()),
                        runsOnDay(calendarData.isSunday()),
                        calendarData.getStartDate().toString(DATE_FORMAT),
                        calendarData.getEndDate().toString(DATE_FORMAT))));

        writer.close();
        logger.info("**** End cleansing calendar.\n\n");
    }

    public Set<String> cleanseStoptimes(Set<String> tripIds) throws IOException {
        logger.info("**** Start cleansing stop times.");
        Set<String> stopIds = new HashSet<>();

        Stream<StopTimeData> stopTimes = transportDataReader.getStopTimes();
        TransportDataWriter writer = transportDataWriterFactory.getWriter("stop_times");

        stopTimes.filter(stopTime -> tripIds.contains(stopTime.getTripId()))
                .forEach(stopTime -> {
                    writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s",
                            stopTime.getTripId(),
                            DateTimeService.formatTime(stopTime.getArrivalTime()),
                            DateTimeService.formatTime(stopTime.getDepartureTime()),
                            stopTime.getStopId(),
                            stopTime.getStopSequence(),
                            stopTime.getPickupType(),
                            stopTime.getDropOffType()));
                    stopIds.add(stopTime.getStopId());
                });

        writer.close();
        logger.info("**** End cleansing stop times.\n");
        return stopIds;
    }

    public ServicesAndTrips cleanseTrips(List<String> routeCodes) throws IOException {
        logger.info("**** Start cleansing trips.");
        Set<String> uniqueSvcIds = new HashSet<>();
        Set<String> tripIds = new HashSet<>();
        Stream<TripData> trips = transportDataReader.getTrips();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("trips");

        trips.filter(trip -> routeCodes.contains(trip.getRouteId())).forEach(trip -> {
            writer.writeLine(String.format("%s,%s,%s,%s",
                    trip.getRouteId(),
                    trip.getServiceId(),
                    trip.getTripId(),
                    trip.getTripHeadsign()));
            tripIds.add(trip.getTripId());
            uniqueSvcIds.add(trip.getServiceId());
        });
        writer.close();
        logger.info("**** End cleansing trips.\n\n");
        return new ServicesAndTrips(uniqueSvcIds, tripIds);
    }

    public void cleanseStops(Set<String> stopIds) throws IOException {
        logger.info("**** Start cleansing stops.");
        Stream<StopData> stops = transportDataReader.getStops();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("stops");

        stops.filter(stop -> stopIds.contains(stop.getId())).forEach(stop ->
                writer.writeLine(String.format("%s,%s,%s,%s,%s",
                        stop.getId(),
                        stop.getCode(),
                        stop.getName(),
                        stop.getLatitude(),
                        stop.getLongitude())));

        writer.close();
        logger.info("**** End cleansing stops.\n\n");
    }

    public List<String> cleanseRoutes(List<String> agencyCodes) throws IOException {
        logger.info("**** Start cleansing routes.");
        List<String> routeCodes = new LinkedList<>();
        Stream<RouteData> routes = transportDataReader.getRoutes();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("routes");

        routes.filter(route -> agencyCodes.contains(route.getAgency())).forEach(route ->
        {
            String id = route.getId();
            writer.writeLine(String.format("%s,%s,%s,%s,0",
                    id,
                    route.getAgency(),
                    route.getCode(),
                    route.getName()));
            routeCodes.add(id);
            logger.info("Added route " + id);
        });
        writer.close();
        logger.info("**** End cleansing routes.\n\n");
        return routeCodes;
    }

    private String runsOnDay(boolean day) {
        return day ? "1" : "0";
    }

    public void cleanFeedInfo() throws IOException {
        logger.info("**** Start cleansing feed info.");
        Stream<FeedInfo> feedInfo = transportDataReader.getFeedInfo();

        TransportDataWriter writer = transportDataWriterFactory.getWriter("feed_info");

        feedInfo.skip(1).forEach(info -> writer.writeLine(String.format("%s,%s,%s,%s,%s,%s,%s",
                info.getPublisherName(),
                info.getPublisherUrl(),
                info.getTimezone(),
                info.getLang(),
                info.validFrom(),
                info.validUntil(),
                info.getVersion())));
        writer.close();
        logger.info("**** End cleansing feed info.");

    }
}
