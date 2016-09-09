package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.*;
import com.tramchester.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public abstract class JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyResponseMapper.class);
    protected TransportDataFromFiles transportData;

    protected JourneyResponseMapper(TransportDataFromFiles transportData) {
        this.transportData = transportData;
    }

    protected abstract Optional<Journey> createJourney(RawJourney rawJourney, int withinMins);

    public JourneyPlanRepresentation map(Set<RawJourney> journeys, int withinMins) throws TramchesterException {
        logger.info(format("Mapping journey %s with window %s", journeys, withinMins));
        SortedSet<Journey> decoratedJourneys = decorateJourneys(journeys, withinMins);
        return new JourneyPlanRepresentation(decoratedJourneys);
    }

    protected SortedSet<Journey> decorateJourneys(Set<RawJourney> rawJourneys, int withinMins)
            throws TramchesterException {
        logger.info("Decorating the discovered journeys " + rawJourneys.size());
        SortedSet<Journey> journeys = new TreeSet<>();
        rawJourneys.forEach(rawJourney -> {
            logger.info("Decorating journey " + rawJourney);

            Optional<Journey> journey = createJourney(rawJourney, withinMins);
            if (journey.isPresent()) {
                journeys.add(journey.get());
                logger.info("Added journey " +journey);
            } else {
                logger.warn(format("Unable to map %s to journey", rawJourney));
            }
        });

        return journeys;
    }

    protected TravelAction decideAction(List<PresentableStage> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        if ((stagesSoFar.get(stagesSoFar.size()-1) instanceof WalkingStage)) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }

}
