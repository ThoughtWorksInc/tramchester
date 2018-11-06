package com.tramchester.unit.dataimport.datacleanse;

import com.tramchester.dataimport.ErrorCount;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.ServicesAndTrips;
import com.tramchester.dataimport.datacleanse.TransportDataWriter;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.TramTime;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDataCleanser extends EasyMockSupport {

    private TransportDataReader reader;
    private TransportDataWriter writer;
    private DataCleanser cleanser;
    private TransportDataWriterFactory factory;

    @Before
    public void beforeEachTestRuns() {
        reader = createMock(TransportDataReader.class);
        writer = createMock(TransportDataWriter.class);
        factory = createMock(TransportDataWriterFactory.class);
        cleanser = new DataCleanser(reader, factory, new ErrorCount());
    }

    @Test
    public void shouldCleanseRoutes() throws IOException {

        RouteData routeA = new RouteData("R2", "CODE2", "CtoD", "NOT");
        RouteData routeB = new RouteData("R1", "CODE1", "AtoB", "MET");
        Stream<RouteData> routes = Stream.of(routeA, routeB);

        EasyMock.expect(reader.getRoutes()).andReturn(routes);
        validateWriter("routes", "R1,MET,CODE1,AtoB,0");

        replayAll();
        List<String> routeCodes = cleanser.cleanseRoutes(new HashSet<>(Arrays.asList("MET")));
        verifyAll();

        assertEquals(1, routeCodes.size());
        assertEquals("R1", routeCodes.get(0));
    }

    @Test
    public void shouldCleanseRoutesWildcard() throws IOException {
        RouteData routeA = new RouteData("R2", "CODE2", "CtoD", "ANY");
        RouteData routeB = new RouteData("R1", "CODE1", "AtoB", "XYX");
        Stream<RouteData> routes = Stream.of(routeA, routeB);

        EasyMock.expect(reader.getRoutes()).andReturn(routes);
        validateWriter("routes", "R1,XYX,CODE1,AtoB,0", "R2,ANY,CODE2,CtoD,0");

        replayAll();
        List<String> routeCodes = cleanser.cleanseRoutes(new HashSet<>(Arrays.asList("*")));
        verifyAll();

        assertEquals(2, routeCodes.size());
    }

    @Test
    public void shouldCleanseStopsMet() throws IOException {

        StopData stopA = new StopData("1122IdA", "codeA", "areaA", "nameA", 0.11, 0.22, true);
        StopData stopB = new StopData("9400IdB", "codeB", "areaB", "nameB", 0.33, 0.44, true);
        Stream<StopData> stops = Stream.of(stopA, stopB);

        EasyMock.expect(reader.getStops()).andReturn(stops);
        validateWriter("stops", "9400IdB,codeB,\"areaB,nameB (Manchester Metrolink)\",0.33,0.44");

        Set<String> stopIds = new HashSet<>();
        stopIds.add("9400IdB");

        replayAll();
        cleanser.cleanseStops(stopIds);
        verifyAll();
    }


    @Test
    public void shouldCleanseStopMultiPart() throws IOException {
        StopData stopA = new StopData("1800EB05551", "mantdwgj", "Rusholme", "Anson Road/St. Anselm Hall (Stop B)",
                53.45412,-2.21209, false);
        Stream<StopData> stops = Stream.of(stopA);

        EasyMock.expect(reader.getStops()).andReturn(stops);
        validateWriter("stops",
                "1800EB05551,mantdwgj,\"Rusholme,Anson Road/St. Anselm Hall (Stop B)\",53.45412,-2.21209");

        Set<String> stopIds = new HashSet<>();
        stopIds.add("1800EB05551");

        replayAll();
        cleanser.cleanseStops(stopIds);
        verifyAll();
    }

    @Test
    public void shouldCleanseTrips() throws IOException {
        TripData tripA = new TripData("GMBrouteIdA", "svcIdA", "tripIdA","headsignA");
        TripData tripB = new TripData("METrouteIdB", "svcIdB", "tripIdB","headsignB");
        TripData tripC = new TripData("METrouteIdB", "svcIdB", "tripIdC","headsignC");
        Stream<TripData> trips = Stream.of(tripA, tripB, tripC);

        EasyMock.expect(reader.getTrips()).andReturn(trips);
        validateWriter("trips", "METrouteIdB,svcIdB,tripIdB,headsignB", "METrouteIdB,svcIdB,tripIdC,headsignC");

        replayAll();
        ServicesAndTrips servicesAndTrips = cleanser.cleanseTrips(Arrays.asList("METrouteIdB"));
        verifyAll();

        Set<String> serviceIds = servicesAndTrips.getServiceIds();
        assertEquals(1, serviceIds.size());
        assertTrue(serviceIds.contains("svcIdB"));
        Set<String> tripIds = servicesAndTrips.getTripIds();
        assertEquals(2, tripIds.size());
        assertTrue(tripIds.contains("tripIdB"));
        assertTrue(tripIds.contains("tripIdC"));
    }

    private Optional<TramTime> formTramTimeParam(LocalTime time) {
        return Optional.of(TramTime.of(time));
    }

    @Test
    public void shouldCleanseStopTimes() throws IOException {
        LocalTime now = LocalTime.now();
        LocalTime arrivalTime = LocalTime.parse("11:10:00");
        LocalTime departureTime = LocalTime.parse("12:09:00");

        StopTimeData stopTimeA = new StopTimeData("tripIdA", formTramTimeParam(now.minusHours(1)),
                formTramTimeParam(now.plusHours(1)), "1200stopIdA", "stopSeqA", "pickupA", "dropA");

        StopTimeData stopTimeB = new StopTimeData("tripIdB", formTramTimeParam(arrivalTime), formTramTimeParam(departureTime),
                "9400stopIdB", "stopSeqB", "pickupB", "dropB");

        Stream<StopTimeData> stopTimes = Stream.of(stopTimeA, stopTimeB);

        EasyMock.expect(reader.getStopTimes()).andReturn(stopTimes);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        validateWriter("stop_times", String.format("tripIdB,%s,%s,9400stopIdB,stopSeqB,pickupB,dropB",
                arrivalTime.format(formatter), departureTime.format(formatter)));

        replayAll();
        Set<String> trips = new HashSet<>();
        trips.add("tripIdB");
        Set<String> stopIds = cleanser.cleanseStoptimes(trips);
        verifyAll();
        assertEquals(1, stopIds.size());
        assertTrue(stopIds.contains("9400stopIdB"));
    }

    @Test
    public void shouldcleanseCalendar() throws IOException {
        Set<String> svcIds = new HashSet<>();
        svcIds.add("svcIDA");
        svcIds.add("svcIDB");
        svcIds.add("svcIDC");

        //DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(("yyyyMMdd"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate start = LocalDate.parse("20151025", dateFormatter);
        LocalDate end = LocalDate.parse("20151026", dateFormatter);
        CalendarData dayA = new CalendarData("svcIDA", false, false, false, false, false, false, false, start, end);
        CalendarData dayB = new CalendarData("svcIDB", true, true, true, true, true, true, true, start, end);
        CalendarData dayC = new CalendarData("svcIDC", false, true, false, false, false, false, false, start, end);
        CalendarData dayD = new CalendarData("svcIDD", true, true, true, true, true, true, true, start, end);

        Stream<CalendarData> calendar = Stream.of(dayA, dayB, dayC, dayD);
        EasyMock.expect(reader.getCalendar()).andReturn(calendar);
        validateWriter("calendar", "svcIDA,0,0,0,0,0,0,0,20151025,20151026",
                "svcIDB,1,1,1,1,1,1,1,20151025,20151026",
                "svcIDC,0,1,0,0,0,0,0,20151025,20151026");

        replayAll();
        cleanser.cleanseCalendar(svcIds);
        verifyAll();
    }

    @Test
    public void shouldLeaveFeedInfoLinesUntouched() throws IOException {

        FeedInfo lineA = new FeedInfo("pubA", "urlA", "tzA", "landA", LocalDate.of(2016,11,29),
                LocalDate.of(2016,11,30), "versionA");

        Stream<FeedInfo> feedInfoStream = Stream.of(lineA);

        EasyMock.expect(reader.getFeedInfo()).andReturn(feedInfoStream);
        validateWriter("feed_info",  "pubA,urlA,tzA,landA,20161129,20161130,versionA");

        replayAll();
        cleanser.cleanFeedInfo();
        verifyAll();
    }

    private void validateWriter(String filename, String... lines) throws IOException {
        EasyMock.expect(factory.getWriter(filename)).andReturn(writer);
        for (String line : lines) {
            writer.writeLine(line);
            EasyMock.expectLastCall();
        }
        writer.close();
        EasyMock.expectLastCall();
    }
}
