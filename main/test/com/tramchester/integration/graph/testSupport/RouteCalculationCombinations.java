package com.tramchester.integration.graph.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteEndRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.StationPair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RouteCalculationCombinations {

    private final TramchesterConfig config;
    private final GraphDatabase database;
    private final RouteCalculator calculator;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final RouteEndRepository routeEndRepository;

    public RouteCalculationCombinations(ComponentContainer componentContainer, TramchesterConfig config) {
        this.config = config;
        this.database = componentContainer.get(GraphDatabase.class);
        this.calculator = componentContainer.get(RouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.interchangeRepository = componentContainer.get(InterchangeRepository.class);
        routeEndRepository = componentContainer.get(RouteEndRepository.class);
    }

    @NotNull
    public Map<StationPair, JourneyOrNot> computeJourneys(LocalDate queryDate, Set<StationPair> combinations, TramTime queryTime) {
        return combinations.parallelStream().
                map(requested -> {
                    try (Transaction txn = database.beginTx()) {
                        JourneyRequest request = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false, 3,
                                config.getMaxJourneyDuration()); //.setDiag(diag);
                        Optional<Journey> optionalJourney = findJourneys(txn, requested.getBegin(), requested.getEnd(), request);

                        JourneyOrNot journeyOrNot = new JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    public Optional<Journey> findJourneys(Transaction txn, IdFor<Station> start, IdFor<Station> dest, JourneyRequest journeyRequest) {
        return calculator.calculateRoute(txn, stationRepository.getStationById(start),
                stationRepository.getStationById(dest), journeyRequest).limit(1).findAny();
    }

    public Map<StationPair, Optional<Journey>> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<StationPair> combinations, TramTime queryTime) {

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false,
                3, config.getMaxJourneyDuration());

        final ConcurrentMap<StationPair, Optional<Journey>> results = new ConcurrentHashMap<>(combinations.size());
        combinations.forEach(pair -> results.put(pair, Optional.empty()));

        combinations.parallelStream().
                map(journey -> {
                    try(Transaction txn=database.beginTx()) {
                        Station start = stationRepository.getStationById(journey.getBegin());
                        Station dest = stationRepository.getStationById(journey.getEnd());
                        return Pair.of(journey,
                                calculator.calculateRoute(txn, start, dest, journeyRequest)
                                        .limit(1).findAny());
                    }
                }).
                forEach(stationsJourneyPair -> results.put(stationsJourneyPair.getLeft(), stationsJourneyPair.getRight()));

        Assertions.assertEquals(combinations.size(), results.size(), "Not enough results");

        // check all results present, collect failures into a list
        List<StationPair> failed = results.
                entrySet().stream().
                filter(journey -> journey.getValue().isEmpty()).
                map(Map.Entry::getKey).
                //map(pair -> Pair.of(pair.getLeft(), pair.getRight())).
                collect(Collectors.toList());

        Assertions.assertEquals(
                0L, failed.size(), format("Failed some of %s (finished %s) combinations %s",
                        results.size(), combinations.size(), displayFailed(failed)));

        return results;
    }
    
    private String displayFailed(List<StationPair> pairs) {
        StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> {
            stringBuilder.append("[").
                    append(pair.getBegin()).
                    append(" to ").append(pair.getEnd()).
                    //append(" id=").append(dest.getId())
                    append("] "); });
        return stringBuilder.toString();
    }

    public Set<StationPair> EndOfRoutesToInterchanges(TransportMode mode) {
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(interchanges, endRoutes);
    }

    public Set<StationPair> InterchangeToInterchange(TransportMode mode) {
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(mode);
        return createJourneyPairs(interchanges, interchanges);
    }

    public Set<StationPair> InterchangeToEndRoutes(TransportMode mode) {
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, interchanges);
    }

    public Set<StationPair> EndOfRoutesToEndOfRoutes(TransportMode mode) {
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, endRoutes);
    }

    private Set<StationPair> createJourneyPairs(IdSet<Station> starts, IdSet<Station> ends) {
        Set<StationPair> combinations = new HashSet<>();
        for (IdFor<Station> start : starts) {
            for (IdFor<Station> dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(new StationPair(start, dest));
                }
            }
        }
        return combinations;
    }

    public boolean betweenInterchange(StationPair pair) {
        return interchangeRepository.isInterchange(pair.getBegin()) && interchangeRepository.isInterchange(pair.getEnd());
    }

    public boolean betweenEndsOfRoute(StationPair pair) {
        return routeEndRepository.isEndRoute(pair.getBegin()) && routeEndRepository.isEndRoute(pair.getEnd());
    }

    public static class JourneyOrNot {
        private final StationPair requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(StationPair requested, LocalDate queryDate, TramTime queryTime,
                            Optional<Journey> optionalJourney) {
            this.requested = requested;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.journey = optionalJourney.orElse(null);
        }

        public boolean missing() {
            return journey==null;
        }

        public void ifPresent(Consumer<Journey> action) {
            if (this.journey != null) {
                action.accept(this.journey);
            }
        }

        @Override
        public String toString() {
            return "JourneyOrNot{" +
                    " queryDate=" + queryDate +
                    ", queryTime=" + queryTime +
                    ", from=" + requested.getBegin() +
                    ", to=" + requested.getEnd() +
                    '}';
        }
    }
}
