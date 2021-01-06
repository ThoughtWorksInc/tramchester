package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.cloud.data.DownloadsLiveData;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.healthchecks.LiveDataS3UploadHealthCheck;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestLiveDataConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveDataS3UploadHealthCheckTest extends EasyMockSupport {

    private final TramchesterConfig configuration = TestEnv.GET(new TestLiveDataConfig());
    private LocalDateTime localNow;
    private ProvidesLocalNow providesLocalNow;
    private DownloadsLiveData downloadsLiveData;
    private LiveDataS3UploadHealthCheck healthCheck;
    private Duration expectedDuration;

    @BeforeEach
    void beforeEachTest() {
        localNow = TestEnv.LocalNow();
        providesLocalNow = createMock(ProvidesLocalNow.class);
        downloadsLiveData = createMock(DownloadsLiveData.class);

        healthCheck = new LiveDataS3UploadHealthCheck(providesLocalNow, downloadsLiveData, configuration);
        expectedDuration = Duration.of(2 * configuration.getLiveDataConfig().getRefreshPeriodSeconds(), ChronoUnit.SECONDS);
    }

    @Test
    void shouldReportHealthIfUpToDateDataIsInS3() throws Exception {

        StationDepartureInfoDTO item = new StationDepartureInfoDTO();
        List<StationDepartureInfoDTO> liveData = Collections.singletonList(item);
        Stream<StationDepartureInfoDTO> liveDataSteam = liveData.stream();

        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(localNow);
        EasyMock.expect(downloadsLiveData.downloadFor(localNow.minus(expectedDuration), expectedDuration))
                .andReturn(liveDataSteam);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());

    }

    @Test
    void shouldReportUnhealthIfNoDataFound() throws Exception {
        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(localNow);
        EasyMock.expect(downloadsLiveData.downloadFor(localNow.minus(expectedDuration), expectedDuration))
                .andReturn(Stream.empty());

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
    }
}
