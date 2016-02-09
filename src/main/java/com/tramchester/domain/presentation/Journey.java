package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;

public class Journey implements Comparable<Journey> {

    private List<StageWithTiming> stages;
    private String summary;

    public Journey(List<StageWithTiming> stages) {
        this.stages = stages;
    }

    // used front end
    public List<StageWithTiming> getStages() {
        return stages;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    // used front end
    public String getSummary() {
        return summary;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        SortedSet<ServiceTime> serviceTimes = getFirstStage().getServiceTimes();
        return serviceTimes.first().getDepartureTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getLastStage().getExpectedArrivalTime();
    }

    private StageWithTiming getLastStage() {
        int index = stages.size() - 1;
        return stages.get(index);
    }

    private StageWithTiming getFirstStage() {
        return stages.get(0);
    }

    @Override
    public String toString() {
        return  "Journey{" +
                "stages= [" +stages.size() +"] "+ stages +
                ", summary='" + summary + '\'' +
                '}';
    }

    // used front end
    public int getNumberOfTimes() {
        int minNumberOfTimes = Integer.MAX_VALUE;
        for (StageWithTiming stage : stages) {
            int size = stage.getServiceTimes().size();
            if (size < minNumberOfTimes) {
                minNumberOfTimes = size;
            }
        }
        return minNumberOfTimes;
    }

    @Override
    public int compareTo(Journey other) {
        // arrival first
        int compare = getExpectedArrivalTime().compareTo(other.getExpectedArrivalTime());
        // then departure time
        if (compare==0) {
            compare = getFirstDepartureTime().compareTo(other.getFirstDepartureTime());
        }
        // then number of stages
        if (compare==0) {
            // if arrival times match, put journeys with fewer stages first
            if (this.stages.size()<other.stages.size()) {
                compare = -1;
            } else if (other.stages.size()>stages.size()) {
                compare = 1;
            }
        }
        return compare;
    }

}
