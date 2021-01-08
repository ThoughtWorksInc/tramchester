package com.tramchester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataProvider;

public class Module extends AbstractModule {
    private final GuiceContainerDependencies parent;
    private final GraphFilter filter;
    private final TramchesterConfig config;
    private final CacheMetrics.RegistersMetrics registersMetrics;
    private static final CsvMapper csvMapper;
    private static final ObjectMapper objectMapper;

    static {
        csvMapper = CsvMapper.builder().addModule(new AfterburnerModule()).build();
        objectMapper = new ObjectMapper();
    }

    public Module(GuiceContainerDependencies parent, GraphFilter filter, TramchesterConfig config,
                  CacheMetrics.RegistersMetrics registersMetrics) {
        this.parent = parent;
        this.filter = filter;
        this.config = config;
        this.registersMetrics = registersMetrics;

    }

    @Override
    protected void configure() {
        parent.registerConfiguration(config, filter, registersMetrics);
        parent.registerLinkedComponents();
    }

    @SuppressWarnings("unused")
    @Provides
    TransportData providesTransportdata(TransportDataProvider provider) {
        return provider.getData();
    }

    @Provides
    StationsAndLinksGraphBuilder.Ready providesReadyToken(StationsAndLinksGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    StagedTransportGraphBuilder.Ready providesReadyToken(StagedTransportGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    CsvMapper providesCsvMapper() {
        return csvMapper;
    }

    @Provides
    ObjectMapper providesObjectMapper() {
        return objectMapper;
    }

    public <I, T extends I> void bindClass(Class<I> face, Class<T> klass) {
        bind(face).to(klass);
    }

    public <T> void bindInstance(Class<T> klass, T instance) {
        bind(klass).toInstance(instance);
    }

}
