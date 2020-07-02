package com.tramchester.graph.search;

import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.search.states.TraversalState;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;

import java.util.Objects;

public class JourneyState implements ImmutableJourneyState {
    private TramTime journeyClock;
    private boolean onTram;
    private boolean onBus;

    private int journeyOffset;
    private TramTime boardingTime;
    private TraversalState traversalState;
    private int numberOfBoardings;

    public JourneyState(TramTime queryTime, TraversalState traversalState) {
        this.journeyClock = queryTime;
        journeyOffset = 0;
        onTram = false;
        onBus = false;
        this.traversalState = traversalState;
        numberOfBoardings = 0;
    }

    public static JourneyState fromPrevious(ImmutableJourneyState previousState) {
        return new JourneyState((JourneyState) previousState);
    }

    private JourneyState(JourneyState previousState) {
        this.journeyClock = previousState.journeyClock;
        this.onTram = previousState.onTram;
        this.onBus = previousState.onBus;
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
        return onTram || onBus;
    }

    public void leaveBus(int totalCost) throws TramchesterException {
        if (!onBus) {
            throw new TramchesterException("Not currently on a bus");
        }
        leave(totalCost);
        onBus = false;
    }

    public void leaveTram(int totalCost) throws TramchesterException {
        if (!onTram) {
            throw new TramchesterException("Not currently on a tram");
        }
        leave(totalCost);
        onTram = false;
    }

    private void leave(int currentTotalCost) {

        int tripCost = currentTotalCost - journeyOffset;
        journeyClock = boardingTime.plusMinutes(tripCost);

        journeyOffset = currentTotalCost;
        boardingTime = null;
    }

    @Override
    public boolean onTram() {
        return onTram;
    }

    @Override
    public boolean onBus() {
        return onBus;
    }

    @Override
    public int getNumberChanges() {
        if (numberOfBoardings==0) {
            return 0;
        }
        return numberOfBoardings-1; // initial boarding
    }

    public void boardTram() throws TramchesterException {
        guardAlreadyOnboard();
        numberOfBoardings = numberOfBoardings + 1;
        onTram = true;
    }

    public void boardBus() throws TramchesterException {
        guardAlreadyOnboard();
        numberOfBoardings = numberOfBoardings + 1;
        onBus = true;
    }

    private void guardAlreadyOnboard() throws TramchesterException {
        if (onTram) {
            throw new TramchesterException("Already on a tram");
        }
        if (onBus) {
            throw new TramchesterException("Already on a bus");
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
        return onTram == that.onTram && onBus == that.onBus &&
                Objects.equals(journeyClock, that.journeyClock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journeyClock, onTram, onBus);
    }

    @Override
    public String toString() {
        return "JourneyState{" +
                "journeyClock=" + journeyClock +
                ", onTram=" + onTram +
                ", onBus=" + onBus +
                ", journeyOffset=" + journeyOffset +
                ", boardingTime=" + boardingTime +
                ", traversalState=" + traversalState +
                '}';
    }

}
