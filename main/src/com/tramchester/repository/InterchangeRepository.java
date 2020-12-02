package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdMap;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

import static java.lang.String.format;

public class InterchangeRepository implements Disposable, Startable {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private final TransportData dataSource;
    private final Set<GTFSTransportationType> modes;

    private IdMap<Station> busInterchanges;
    private IdMap<Station> trainInterchanges;

    public InterchangeRepository(TransportData dataSource, TramchesterConfig config) {
        this.dataSource = dataSource;
        // both of these empty for trams
        busInterchanges = new IdMap<>();
        trainInterchanges = new IdMap<>();
        modes = config.getTransportModes();
    }

    @Override
    public void dispose() {
        trainInterchanges.clear();
        busInterchanges.clear();
    }

    @Override
    public void start() {
        if (modes.contains(GTFSTransportationType.bus)) {
            busInterchanges = createBusInterchangeList();
            logger.info(format("Added %s bus interchanges", busInterchanges.size()));
        }
        if (modes.contains(GTFSTransportationType.train)) {
            trainInterchanges = createTrainMultiAgencyStationList();
            logger.info(format("Added %s train interchanges", trainInterchanges.size()));
        }
    }

    private IdMap<Station> createTrainMultiAgencyStationList() {
        return dataSource.getStations().stream().
            filter(TransportMode::isTrain).
            filter(station -> station.getAgencies().size()>=2).
            collect(IdMap.collector());
    }

    @Override
    public void stop() {
        // no op
    }

    private IdMap<Station> createBusInterchangeList() {
        logger.info("Finding bus interchanges based on names");
        return dataSource.getStations().stream().
                filter(TransportMode::isBus).
                filter(station -> checkForBusInterchange(station.getName())).
                collect(IdMap.collector());
    }

    // TODO WIP
    // Very crude - need better way
    private boolean checkForBusInterchange(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("interchange")) {
            return true;
        }
        return lower.contains("bus station") && (!lower.contains("adj bus station"));
    }

    public Collection<Station> getBusInterchanges() {
        return busInterchanges.getValues();
    }

    public boolean isInterchange(Station station) {
        if (TransportMode.isTram(station)) {
            return TramInterchanges.has(station);
        }
        if (TransportMode.isBus(station)) {
            return busInterchanges.hasId(station.getId());
        }
        if (TransportMode.isTrain(station)) {
            return trainInterchanges.hasId(station.getId());
        }
        logger.warn("Interchanges not defined for station of type " +station.getTransportMode() + " id was " + station.getId());
        return false;
    }

    public boolean isInterchange(IdFor<Station> stationId) {
        if (TramInterchanges.hasId(stationId)) {
            return true;
        }
        if (busInterchanges.hasId(stationId)) {
            return true;
        }
        return trainInterchanges.hasId(stationId);
    }

}
