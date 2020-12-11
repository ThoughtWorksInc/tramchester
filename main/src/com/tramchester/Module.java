package com.tramchester;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataProvider;

public class Module extends AbstractModule {
    private final GuiceContainerDependencies parent;
    private final GraphFilter filter;
    private final TramchesterConfig config;

    public Module(GuiceContainerDependencies parent, GraphFilter filter, TramchesterConfig config) {
        this.parent = parent;
        this.filter = filter;
        this.config = config;
    }

    @Override
    protected void configure() {
        parent.registerConfiguration(config, filter);
        parent.registerLinkedComponents();
    }

    @SuppressWarnings("unused")
    @Provides
    TransportData providesTransportdata(TransportDataProvider provider) {
        return provider.getData();
    }

    public <I, T extends I> void bindClass(Class<I> face, Class<T> klass) {
        bind(face).to(klass);
    }

    public <T> void bindInstance(Class<T> klass, T instance) {
        bind(klass).toInstance(instance);
    }

}
