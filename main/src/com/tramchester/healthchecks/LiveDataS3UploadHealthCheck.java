package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.DownloadsLiveData;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.time.ProvidesLocalNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@LazySingleton
public class LiveDataS3UploadHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataS3UploadHealthCheck.class);

    private final ProvidesLocalNow providesLocalNow;
    private final DownloadsLiveData downloadsLiveData;
    private final Duration checkDuration;

    @Inject
    public LiveDataS3UploadHealthCheck(ProvidesLocalNow providesLocalNow, DownloadsLiveData downloadsLiveData) {
        checkDuration = Duration.of(10, ChronoUnit.MINUTES);
        this.providesLocalNow = providesLocalNow;
        this.downloadsLiveData = downloadsLiveData;
    }

    @Override
    public String getName() {
        return "liveDataS3Upload";
    }

    @Override
    public Result check() throws Exception {
        logger.info("Check for live data in S3");
        LocalDateTime checkTime = providesLocalNow.getDateTime().minus(checkDuration);

        Stream<StationDepartureInfoDTO> results = downloadsLiveData.downloadFor(checkTime, checkDuration);
        long number = results.count();
        results.close();

        if (number==0) {
            String msg = "No live data found in S3 at " + checkTime + " for " + checkDuration;
            logger.error(msg);
            return Result.unhealthy(msg);
        } else {
            String msg = "Found " + number + " records in S3 at " + checkTime + " and duration " + checkDuration;
            logger.info(msg);
            return Result.healthy(msg);
        }

    }
}