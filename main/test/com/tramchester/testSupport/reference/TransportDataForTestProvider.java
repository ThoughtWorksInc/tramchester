package com.tramchester.testSupport.reference;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.TransportDataProvider;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static com.tramchester.domain.places.Station.METROLINK_PREFIX;
import static com.tramchester.domain.reference.KnownTramRoute.EDidsburyManchesterRochdale;
import static com.tramchester.domain.reference.KnownTramRoute.RochdaleManchesterEDidsbury;
import static java.lang.String.format;

@LazySingleton
public class TransportDataForTestProvider implements TransportDataProvider {
    private final TestTransportData container;
    private boolean populated;

    @Inject
    public TransportDataForTestProvider(ProvidesNow providesNow) {
        container = new TestTransportData(providesNow);
        populated = false;
    }

    public TransportData getData() {
        return getTestData();
    }

    public TestTransportData getTestData() {
        if (!populated) {
            populateTestData(container);
            populated = true;
        }
        return container;
    }

    private void populateTestData(TransportDataContainer container) {
        Route routeA = RoutesForTesting.ALTY_TO_BURY; // TODO This route not present during lockdown
        Route routeB = RoutesForTesting.createTramRoute(RochdaleManchesterEDidsbury);
        Route routeC = RoutesForTesting.createTramRoute(EDidsburyManchesterRochdale);

        Agency agency = new Agency("MET", "agencyName");
        agency.addRoute(routeA);
        agency.addRoute(routeB);
        agency.addRoute(routeC);
        container.addAgency(agency);

        Service serviceA = new Service(TestTransportData.serviceAId, routeA);
        Service serviceB = new Service(TestTransportData.serviceBId, routeB);
        Service serviceC = new Service(TestTransportData.serviceCId, routeC);

        routeA.addService(serviceA);
        routeB.addService(serviceB);
        routeC.addService(serviceC);

        container.addRoute(routeA);
        container.addRoute(routeB);
        container.addRoute(routeC);

        serviceA.setDays(true, false, false, false, false, false, false);
        serviceB.setDays(true, false, false, false, false, false, false);
        serviceC.setDays(true, false, false, false, false, false, false);

        LocalDate startDate = LocalDate.of(2014, 2, 10);
        LocalDate endDate = LocalDate.of(2020, 8, 15);
        serviceA.setServiceDateRange(startDate, endDate);
        serviceB.setServiceDateRange(startDate, endDate);
        serviceC.setServiceDateRange(startDate, endDate);

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        Trip tripA = new Trip(TestTransportData.TRIP_A_ID, "headSign", serviceA, routeA);

        //LatLong latLong = new LatLong(latitude, longitude);
        Station first = new TestStation(TestTransportData.FIRST_STATION, "area1", "startStation",
                TestEnv.nearAltrincham, TestEnv.nearAltrinchamGrid, TransportMode.Tram);
        addAStation(container, first);
        addRouteStation(container, first, routeA);
        TramStopCall stopA = createStop(container, tripA, first, ServiceTime.of(8, 0), ServiceTime.of(8, 0), 1);
        tripA.addStop(stopA);

        Station second = new TestStation(TestTransportData.SECOND_STATION, "area2", "secondStation", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid, TransportMode.Tram);
        addAStation(container, second);
        addRouteStation(container, second, routeA);
        TramStopCall stopB = createStop(container, tripA, second, ServiceTime.of(8, 11), ServiceTime.of(8, 11), 2);
        tripA.addStop(stopB);

        Station interchangeStation = new TestStation(TestTransportData.INTERCHANGE, "area3", "cornbrookStation", TestEnv.nearShudehill,
                TestEnv.nearShudehillGrid, TransportMode.Tram);
        addAStation(container, interchangeStation);
        addRouteStation(container, interchangeStation, routeA);
        TramStopCall stopC = createStop(container, tripA, interchangeStation, ServiceTime.of(8, 20), ServiceTime.of(8, 20), 3);
        tripA.addStop(stopC);

        Station last = new TestStation(TestTransportData.LAST_STATION, "area4", "endStation", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid,  TransportMode.Tram);
        addAStation(container, last);
        addRouteStation(container, last, routeA);
        TramStopCall stopD = createStop(container, tripA, last, ServiceTime.of(8, 40), ServiceTime.of(8, 40), 4);
        tripA.addStop(stopD);

        // service A
        serviceA.addTrip(tripA);

        Station stationFour = new TestStation(TestTransportData.STATION_FOUR, "area4", "Station4", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid,  TransportMode.Tram);
        addAStation(container, stationFour);

        Station stationFive = new TestStation(TestTransportData.STATION_FIVE, "area5", "Station5", TestEnv.nearStockportBus,
                TestEnv.nearStockportBusGrid,  TransportMode.Tram);
        addAStation(container, stationFive);

        //
        Trip tripC = new Trip("tripCId", "headSignC", serviceC, routeC);
        TramStopCall stopG = createStop(container, tripC, interchangeStation, ServiceTime.of(8, 26), ServiceTime.of(8, 27), 1);
        addRouteStation(container, interchangeStation, routeC);
        TramStopCall stopH = createStop(container, tripC, stationFive, ServiceTime.of(8, 31), ServiceTime.of(8, 33), 2);
        addRouteStation(container, stationFive, routeC);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        serviceC.addTrip(tripC);

        // INTERCHANGE -> STATION_FOUR
        addRouteStation(container, stationFour, routeB);
        addRouteStation(container, interchangeStation, routeB);

        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(8, 26), "tripBId");
        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 10), "tripB2Id");
        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 20), "tripB3Id");

        container.addTrip(tripA);
        container.addTrip(tripC);

        container.addService(serviceA);
        container.addService(serviceB);
        container.addService(serviceC);
        container.updateTimesForServices();

    }

    private void addAStation(TransportDataContainer container, Station station) {
        container.addStation(station);
    }

    private static void addRouteStation(TransportDataContainer container, Station station, Route route) {
        RouteStation routeStation = new RouteStation(station, route);
        container.addRouteStation(routeStation);
        station.addRoute(route);
    }

    private static void createInterchangeToStation4Trip(TransportDataContainer container, Route route, Service service,
                                                        Station interchangeStation, Station station, LocalTime startTime, String tripId) {
        Trip trip = new Trip(tripId, "headSignTripB2", service, route);
        TramStopCall stop1 = createStop(container,trip, interchangeStation, ServiceTime.of(startTime),
                ServiceTime.of(startTime.plusMinutes(5)), 1);
        trip.addStop(stop1);
        TramStopCall stop2 = createStop(container,trip, station, ServiceTime.of(startTime.plusMinutes(5)),
                ServiceTime.of(startTime.plusMinutes(8)), 2);
        trip.addStop(stop2);
        service.addTrip(trip);
        container.addTrip(trip);
    }

    private static TramStopCall createStop(TransportDataContainer container, Trip trip, Station station, ServiceTime arrivalTime, ServiceTime departureTime, int sequenceNum) {
        String platformId = station.getId() + "1";
        Platform platform = new Platform(platformId, format("%s platform 1", station.getName()));
        container.addPlatform(platform);
        station.addPlatform(platform);
        StopTimeData stopTimeData = new StopTimeData(trip.getId().forDTO(), arrivalTime, departureTime, platformId,sequenceNum,
                GTFSPickupDropoffType.Regular, GTFSPickupDropoffType.Regular);
        return new TramStopCall(platform, station, stopTimeData);
    }


    public static class TestTransportData extends TransportDataContainer {

        private static final String serviceAId = "serviceAId";
        private static final String serviceBId = "serviceBId";
        private static final String serviceCId = "serviceCId";

        public static final String TRIP_A_ID = "tripAId";
        public static final String FIRST_STATION = METROLINK_PREFIX + "_ST_FIRST";
        public static final String SECOND_STATION = METROLINK_PREFIX + "_ST_SECOND";
        public static final String LAST_STATION = METROLINK_PREFIX + "_ST_LAST";
        public static final String INTERCHANGE = TramStations.Cornbrook.forDTO();
        private static final String STATION_FOUR = METROLINK_PREFIX + "_ST_FOUR";
        private static final String STATION_FIVE = METROLINK_PREFIX + "_ST_FIVE";

        public TestTransportData(ProvidesNow providesNow) {
            super(providesNow);
        }

        @Override
        public void addStation(Station station) {
            super.addStation(station);
        }

        public Station getFirst() {
            return getStationById(IdFor.createId(FIRST_STATION));
        }

        public Station getSecond() {
            return getStationById(IdFor.createId(SECOND_STATION));
        }

        public Station getInterchange() {
            return getStationById(IdFor.createId(INTERCHANGE));
        }

        public Station getLast() {
            return getStationById(IdFor.createId(LAST_STATION));
        }

        public Station getFifthStation() {
            return getStationById(IdFor.createId(STATION_FIVE));
        }

        public Station getFourthStation() {
            return getStationById(IdFor.createId(STATION_FOUR));
        }

        @Override
        public Map<String, FeedInfo> getFeedInfos() {
            FeedInfo info = new FeedInfo("publisherName", "publisherUrl", "timezone", "lang",
                    LocalDate.of(2016, 5, 25),
                    LocalDate.of(2016, 6, 30), "version");

            Map<String, FeedInfo> result = new HashMap<>();
            result.put("TransportDataForTest", info);
            return result;
        }

    }
}