package com.tramchester.repository;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TransportMode;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class TransportModeRepository {
    private final TramchesterConfig config;

    public TransportModeRepository(TramchesterConfig config) {
        this.config = config;
    }

    public Set<TransportMode> getModes() {
        return config.getDataSourceConfig().stream().
                map(DataSourceConfig::getTransportModes).
                flatMap(Collection::stream).
                map(TransportMode::fromGTFS).collect(Collectors.toSet());
    }
}