package com.tramchester.integration;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class TFGMTestDataSourceConfig implements DataSourceConfig {

    private final String dataFolder;
    private final Set<GTFSTransportationType> modes;

    public TFGMTestDataSourceConfig(String dataFolder, Set<GTFSTransportationType> modes) {
        this.dataFolder = dataFolder;
        this.modes = modes;
    }

    @Override
    public String getTramDataUrl() {
        return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
    }

    @Override
    public String getTramDataCheckUrl() {
        return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
    }

    @Override
    public Path getDataPath() {
        return Paths.get(dataFolder);
    }

    @Override
    public Path getUnzipPath() {
        return  Paths.get("gtdf-out");
    }

    @Override
    public String getZipFilename() {
        return "data.zip";
    }

    @Override
    public String getName() {
        return "tfgm";
    }

    @Override
    public boolean getHasFeedInfo() {
        return true;
    }

    @Override
    public Set<GTFSTransportationType> getTransportModes() {
        return modes;
    }
}