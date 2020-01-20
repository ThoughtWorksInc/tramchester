package com.tramchester.integration.livedata;

import com.tramchester.Dependencies;
import com.tramchester.LiveDataTestCategory;
import com.tramchester.domain.Station;
import com.tramchester.graph.TramRouteReachable;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.TramPosition;
import com.tramchester.livedata.TramPositionInference;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.repository.StationAdjacenyRepository;
import com.tramchester.repository.StationRepository;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TramPositionInferenceTest {

    private static Dependencies dependencies;

    private TramPositionInference positionInference;
    private StationRepository stationRepository;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @Before
    public void onceBeforeEachTestRuns() {
        LiveDataRepository liveDataSource = dependencies.get(LiveDataRepository.class);
        liveDataSource.refreshRespository();
        TramRouteReachable routeReachable = dependencies.get(TramRouteReachable.class);
        StationAdjacenyRepository adjacenyMatrix = dependencies.get(StationAdjacenyRepository.class);
        positionInference = new TramPositionInference(liveDataSource, adjacenyMatrix, routeReachable);
        stationRepository = dependencies.get(StationRepository.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldInferTramPosition() {
        // NOTE: costs are not symmetric between two stations, i.e. one direction might cost more than the other
        // Guess this is down to signalling, track, etc.
        int cost = 3; // cost between the stations, no due trams outside this limit should appear

        Station first = stationRepository.getStation(Stations.Deansgate.getId()).get();
        Station second = stationRepository.getStation(Stations.StPetersSquare.getId()).get();

        TramPosition between = positionInference.findBetween(first, second);
        assertEquals(first, between.getFirst());
        assertEquals(second, between.getSecond());
        assertTrue(between.getTrams().size()>=1);
        between.getTrams().forEach(dueTram -> assertFalse(Integer.toString(dueTram.getWait()), (dueTram.getWait())> cost));

        TramPosition otherDirection = positionInference.findBetween(second, first);
        assertTrue(otherDirection.getTrams().size()>=1);
        otherDirection.getTrams().forEach(dueTram -> assertFalse(Integer.toString(dueTram.getWait()), (dueTram.getWait())> cost));
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldInferAllTramPositions() {
        List<TramPosition> results = positionInference.inferWholeNetwork();
        long hasTrams = results.stream().filter(position -> !position.getTrams().isEmpty()).count();

        assertTrue(hasTrams>0);
    }
}