package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.SortedSet;

import static java.lang.String.format;

public class StageWithTiming extends RawVehicleStage {
    public static final int SECONDS_IN_DAY = 24*60*60;
    private final TravelAction action;
    private SortedSet<ServiceTime> serviceTimes;

    public StageWithTiming(RawVehicleStage rawTravelStage, SortedSet<ServiceTime> serviceTimes, TravelAction action) {
        super(rawTravelStage);
        this.serviceTimes = serviceTimes;
        this.action = action;
    }

    public SortedSet<ServiceTime> getServiceTimes() {
        return serviceTimes;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        return serviceTimes.first().getArrivalTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
       return serviceTimes.first().getDepartureTime();
    }

    // this is wrong, duration varies, need to extract from servicetime instead and pass in index
    @Deprecated
    public int getDuration() {
        // likely this only works for Tram when duration between stops does not vary by time of day
        ServiceTime serviceTime = serviceTimes.first();
        LocalTime arrivalTime = serviceTime.getArrivalTime();
        LocalTime departureTime = serviceTime.getDepartureTime();
        int depSecs = departureTime.toSecondOfDay();

        int seconds;
        if (arrivalTime.isBefore(departureTime)) { // crosses midnight
            int secsBeforeMid = SECONDS_IN_DAY - depSecs;
            int secsAfterMid = arrivalTime.toSecondOfDay();
            seconds = secsBeforeMid + secsAfterMid;
        } else {
            seconds = arrivalTime.toSecondOfDay() - depSecs;
        }
        return seconds / 60;
    }

    public int findEarliestDepartureTime() {
        int earliest = Integer.MAX_VALUE;
        for (ServiceTime time : serviceTimes) {
            if (time.getFromMidnightLeaves() < earliest) {
                earliest = time.getFromMidnightLeaves();
            }
        }
        return earliest;
    }

    @Override
    public String toString() {
        return "StageWithTiming{" +
                "rawTravelStage=" + super.toString() +
                ", serviceTimes=" + serviceTimes +
                '}';
    }

    @Deprecated
    public int findDepartureTimeForEarliestArrival() {
        int depart = Integer.MAX_VALUE;
        int earlierArrive = Integer.MAX_VALUE;
        for (ServiceTime time : serviceTimes) {
            if (time.getFromMidnightArrives() < earlierArrive) {
                depart = time.getFromMidnightLeaves();
            }
        }
        return depart;
    }

    public String getSummary() throws TramchesterException {
        switch (mode) {
            case Bus : {
                return format("%s Bus route", routeName);
            }
            case Tram: {
                return format("%s Tram line", routeName);
            }
            default:
                throw new TramchesterException("Unknown transport mode " + mode);
        }
    }

    public String getPrompt() throws TramchesterException {
        String verb;
        switch (action) {
            case Board: verb = "Board";
                break;
            case Leave: verb = "Leave";
                break;
            case Change: verb = "Change";
                break;
            default:
                throw new TramchesterException("Unknown transport action " + action);
        }

        switch (mode) {
            case Bus : {
                return format("%s bus at", verb);
            }
            case Tram: {
                return format("%s tram at", verb);
            }
            default:
                throw new TramchesterException("Unknown transport mode " + mode);
        }
    }
}
