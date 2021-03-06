package com.tramchester.graph.search;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.search.states.TraversalState;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;

import java.util.Objects;

public class JourneyState implements ImmutableJourneyState {
    private TramTime journeyClock;
    private TransportMode transportMode;

    private int journeyOffset;
    private TramTime boardingTime;
    private TraversalState traversalState;
    private int numberOfBoardings;
    private int numberOfConnections;

    public JourneyState(TramTime queryTime, TraversalState traversalState) {
        this.journeyClock = queryTime;
        journeyOffset = 0;
        transportMode = TransportMode.NotSet;
        this.traversalState = traversalState;
        numberOfBoardings = 0;
        numberOfConnections = 0;
    }

    public static JourneyState fromPrevious(ImmutableJourneyState previousState) {
        return new JourneyState((JourneyState) previousState);
    }

    private JourneyState(JourneyState previousState) {
        this.journeyClock = previousState.journeyClock;
        this.transportMode = previousState.transportMode;
        this.journeyOffset = previousState.journeyOffset;
        this.traversalState = previousState.traversalState;
        if (onBoard()) {
            this.boardingTime = previousState.boardingTime;
        }
        this.numberOfBoardings = previousState.numberOfBoardings;
    }

    public static InitialBranchState<JourneyState> initialState(TramTime queryTime,
                                                                TraversalState traversalState) {
        return new InitialBranchState<>() {
            @Override
            public JourneyState initialState(Path path) {
                return new JourneyState(queryTime, traversalState);
            }

            @Override
            public InitialBranchState<JourneyState> reverse() {
                return null;
            }
        };
    }

    public TramTime getJourneyClock() {
        return journeyClock;
    }

    public void updateJourneyClock(int currentTotalCost) {
        int costForTrip = currentTotalCost - journeyOffset;

        if (onBoard()) {
            journeyClock = boardingTime.plusMinutes(costForTrip);
        } else {
            journeyClock = journeyClock.plusMinutes(costForTrip);
        }
    }

    public void recordVehicleDetails(TramTime boardingTime, int currentCost) throws TramchesterException {
        if (! (onBoard()) ) {
            throw new TramchesterException("Not on a bus or tram");
        }
        this.journeyClock = boardingTime;
        this.boardingTime = boardingTime;
        this.journeyOffset = currentCost;
    }

    private boolean onBoard() {
        return !transportMode.equals(TransportMode.NotSet);
    }

    public void leave(TransportMode mode, int totalCost) throws TramchesterException {
        if (!transportMode.equals(mode)) {
            throw new TramchesterException("Not currently on " +mode+ " was " + transportMode);
        }
        leave(totalCost);
        transportMode = TransportMode.NotSet;
    }

    private void leave(int currentTotalCost) {

        int tripCost = currentTotalCost - journeyOffset;
        journeyClock = boardingTime.plusMinutes(tripCost);

        journeyOffset = currentTotalCost;
        boardingTime = null;
    }

    @Override
    public int getNumberChanges() {
        if (numberOfBoardings==0) {
            return 0;
        }
        return numberOfBoardings-1; // initial boarding
    }

    @Override
    public int getNumberConnections() {
        return numberOfConnections;
    }

    public void board(TransportMode mode) throws TramchesterException {
        guardAlreadyOnboard();
        numberOfBoardings = numberOfBoardings + 1;
        transportMode = mode;
    }

    private void guardAlreadyOnboard() throws TramchesterException {
        if (!transportMode.equals(TransportMode.NotSet)) {
            throw new TramchesterException("Already on a " + transportMode);
        }
    }

    public TraversalState getTraversalState() {
        return traversalState;
    }

    public void updateTraversalState(TraversalState traversalState) {
        this.traversalState = traversalState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JourneyState that = (JourneyState) o;
        return transportMode == that.transportMode &&
                Objects.equals(journeyClock, that.journeyClock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journeyClock, transportMode);
    }

    @Override
    public String toString() {
        return "JourneyState{" +
                "journeyClock=" + journeyClock +
                ", transportMode=" + transportMode +
                ", journeyOffset=" + journeyOffset +
                ", boardingTime=" + boardingTime +
                ", traversalState=" + traversalState +
                '}';
    }

    @Override
    public TransportMode getTransportMode() {
        return transportMode;
    }

    public void connection() {
        numberOfBoardings = numberOfConnections + 1;
    }
}
