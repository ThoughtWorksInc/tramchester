package com.tramchester.dataimport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;

public class FetchFileModTime {

    public LocalDateTime getFor(Path filePath) {
        long localModMillis = filePath.toFile().lastModified();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis  / 1000), TramchesterConfig.TimeZone);
    }

    public LocalDateTime getFor(DataSourceConfig config) {
        Path dataPath = config.getDataPath();
        Path latestZipFile = dataPath.resolve(config.getZipFilename());
        return getFor(latestZipFile);
    }
}
