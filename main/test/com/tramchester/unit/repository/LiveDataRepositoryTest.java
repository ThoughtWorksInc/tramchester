package com.tramchester.unit.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.LiveDataObserver;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.*;

public class LiveDataRepositoryTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataParser mapper;
    private LiveDataRepository repository;
    private PlatformDTO platformDTO;

    @Before
    public void beforeEachTestRuns() {
        fetcher = createMock(LiveDataHTTPFetcher.class);
        mapper = createMock(LiveDataParser.class);
        repository = new LiveDataRepository(fetcher, mapper);
        platformDTO = new PlatformDTO(new Platform("platformId", "Platform name"));
    }

    @Test
    public void shouldGetDepartureInformationForSingleStation() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now();
        StationDepartureInfo departureInfo = addStationInfo(info, lastUpdate, "displayId", "platformId",
                "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        Station station = new Station("stationId", "area", "stopName", new LatLong(1,1), true);
        Platform platform = new Platform("platformId", "platformName");
        station.addPlatform(platform);

        replayAll();
        repository.refreshRespository();
        List<StationDepartureInfo> departures = repository.departuresFor(station);
        verifyAll();

        assertEquals(1, departures.size());
        assertEquals(departureInfo, departures.get(0));
    }

    @Test
    public void shouldExcludeMessageBoardTextForSomeDisplays() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        PlatformDTO platformA = new PlatformDTO(new Platform("platformIdA", "Platform name"));
        PlatformDTO platformB = new PlatformDTO(new Platform("platformIdB", "Platform name"));
        PlatformDTO platformC = new PlatformDTO(new Platform("platformIdC", "Platform name"));

        LocalDateTime lastUpdate = LocalDateTime.now();
        addStationInfo(info, lastUpdate, "yyy", "platformIdA", "some message", Stations.Altrincham);
        addStationInfo(info, lastUpdate, "42", "platformIdB", "^F0Next exclude message", Stations.Altrincham);
        addStationInfo(info, lastUpdate, "43", "platformIdC", "<no message>", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(3,repository.countEntries());
        TramServiceDate queryDate = new TramServiceDate(lastUpdate.toLocalDate());
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());
        repository.enrich(platformA, queryDate, queryTime);
        repository.enrich(platformB, queryDate, queryTime);
        repository.enrich(platformC, queryDate, queryTime);

        assertEquals("some message", platformA.getStationDepartureInfo().getMessage());
        assertEquals("", platformB.getStationDepartureInfo().getMessage());
        assertEquals("", platformC.getStationDepartureInfo().getMessage());
    }

    @Test
    public void shouldUpdateStatusWhenRefreshingDataOK() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now();

        addStationInfo(info, lastUpdate, "yyy", "platformIdA", "some message", Stations.Altrincham);
        addStationInfo(info, lastUpdate, "303", "platformIdB", "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(2,repository.countEntries());
        assertEquals(2,repository.countMessages());

        assertEquals(0, repository.staleDataCount());
        assertEquals(2, repository.upToDateEntries(TramTime.of(lastUpdate.toLocalTime())));
        assertEquals(2, repository.upToDateEntries(TramTime.of(lastUpdate.toLocalTime().plusMinutes(14))));
        assertEquals(0, repository.upToDateEntries(TramTime.of(lastUpdate.toLocalTime().plusMinutes(16))));
    }

    @Test
    public void shouldUpdateMessageCountWhenRefreshingDataOK() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now(); // up to date
        addStationInfo(info, lastUpdate, "yyy", "platformIdA", "some message", Stations.Altrincham);
        addStationInfo(info, lastUpdate, "303", "platformIdB", "<no message>", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(2,repository.countEntries());
        assertEquals(1,repository.countMessages());

    }

    @Test
    public void shouldUpdateStatusWhenRefreshingStaleData() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime current = LocalDateTime.now();
        LocalDateTime staleDate = current.minusDays(5).minusMinutes(60); // stale
        addStationInfo(info, staleDate, "yyy", "platformIdC", "some message", Stations.Altrincham);
        addStationInfo(info, staleDate, "303", "platformIdD", "some message", Stations.Altrincham);
        addStationInfo(info, current, "303", "platformIdF", "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(3, repository.countEntries());
        assertEquals(3, repository.countMessages());
        assertEquals(2, repository.staleDataCount());
        assertEquals(1, repository.upToDateEntries(TramTime.of(current.toLocalTime())));
    }

    @Test
    public void shouldEnrichAPlatformWhenDateAndTimeWithinTimeRange() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now();
        StationDepartureInfo departureInfo = addStationInfo(info, lastUpdate, "displayId", "platformId",
                "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        // observer
        List<StationDepartureInfo> observedUpdates = new LinkedList<>();
        LiveDataObserver observer = update -> observedUpdates.addAll(update);
        repository.observeUpdates(observer);

        replayAll();
        repository.refreshRespository();
        TramServiceDate queryDate = new TramServiceDate(lastUpdate.toLocalDate());
        repository.enrich(platformDTO, queryDate, TramTime.of(lastUpdate.toLocalTime()));
        verifyAll();

        StationDepartureInfoDTO expected = new StationDepartureInfoDTO(departureInfo);

        StationDepartureInfoDTO enriched = platformDTO.getStationDepartureInfo();

        assertEquals(expected, enriched);
        assertEquals(observedUpdates.size(),1);
        assertEquals(departureInfo, observedUpdates.get(0));
    }

    @Test
    public void shouldNotEnrichAPlatformWhenDateOutsideOfRange() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now();
        addStationInfo(info, lastUpdate, "displayId", "platformId", "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andStubReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andStubReturn(info);

        TramServiceDate queryDateA = new TramServiceDate(LocalDate.now().minusDays(1));
        TramServiceDate queryDateB = new TramServiceDate(LocalDate.now().plusDays(1));

        replayAll();
        repository.refreshRespository();
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());
        repository.enrich(platformDTO, queryDateA, queryTime);
        StationDepartureInfoDTO enriched = platformDTO.getStationDepartureInfo();
        assertNull(enriched);

        repository.enrich(platformDTO, queryDateB, queryTime);
        enriched = platformDTO.getStationDepartureInfo();
        assertNull(enriched);
        verifyAll();
    }

    @Test
    public void shouldNotEnrichAPlatformWhenTimeOutsideOfRange() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now();
        addStationInfo(info, lastUpdate, "displayId", "platformId", "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andStubReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andStubReturn(info);

        TramServiceDate queryDate = new TramServiceDate(LocalDate.now());

        replayAll();
        repository.refreshRespository();
        repository.enrich(platformDTO, queryDate, TramTime.of(lastUpdate.toLocalTime().plusMinutes(LiveDataRepository.TIME_LIMIT)));
        StationDepartureInfoDTO enriched = platformDTO.getStationDepartureInfo();
        assertNull(enriched);

        repository.enrich(platformDTO, queryDate, TramTime.of(lastUpdate.toLocalTime().minusMinutes(LiveDataRepository.TIME_LIMIT)));
        enriched = platformDTO.getStationDepartureInfo();
        assertNull(enriched);
        verifyAll();
    }

    @Test
    public void shouldEnrichLocationDTOIfHasPlatforms() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LatLong latLong = new LatLong(-1,2);
        Station station = new Station("id", "area", "|stopName", latLong, true);
        station.addPlatform(new Platform("platformId", "Platform name"));
        LocationDTO locationDTO = new LocationDTO(station);

        LocalDateTime lastUpdate = LocalDateTime.now();
        StationDepartureInfo departureInfo = addStationInfo(info, lastUpdate, "displayId",
                "platformId", "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        repository.enrich(locationDTO, LocalDateTime.now() );
        verifyAll();

        StationDepartureInfoDTO expected = new StationDepartureInfoDTO(departureInfo);
        StationDepartureInfoDTO result = locationDTO.getPlatforms().get(0).getStationDepartureInfo();
        assertNotNull(result);
        assertEquals(expected, result);
    }

    public static StationDepartureInfo addStationInfo(List<StationDepartureInfo> info, LocalDateTime lastUpdate,
                                                String displayId, String platformId, String message, Station location) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, "lineName", StationDepartureInfo.Direction.Incoming, platformId,
                location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(new DueTram(Stations.Bury, "Due", 42, "Single", lastUpdate.toLocalTime()));
        return departureInfo;
    }
}
