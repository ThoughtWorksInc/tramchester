package com.tramchester;

import com.tramchester.config.TramchesterConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;


public abstract class TestConfig extends TramchesterConfig {
    private List<String> closedStations = asList("St Peters Square");
    private Path zipFilePath = Paths.get("data.zip");

    private boolean graphExists() {
        return new File(getGraphName()).exists();
    }

    @Override
    public boolean isRebuildGraph() {
        return !graphExists();
    }

    @Override
    public boolean isPullData() {
        File file = getDataFolder().resolve(zipFilePath).toFile();
        return !file.exists();
    }

    @Override
    public boolean isFilterData() {
        return !getDataFolder().resolve("feed_info.txt").toFile().exists() ;
    }

    @Override
    public List<String> getClosedStations() {
        return closedStations;
    }

    @Override
    public String getInstanceDataBaseURL() {
        return "http://localhost:8080";
    }

    @Override
    public String getTramDataUrl() {
        return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
    }

    @Override
    public Path getInputDataPath() {
        return getDataFolder();
    }

    @Override
    public Path getOutputDataPath() {
        return getDataFolder();
    }

    @Override
    public int getTimeWindow() { return 60; }

    public abstract Path getDataFolder();
}
