package com.tramchester.domain.liveUpdates;

import com.tramchester.domain.Station;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StationDepartureInfo implements HasPlatformMessage {

    public enum Direction {
        Incoming, Outgoing, Unknown
    }

    private String lineName;
    private String stationPlatform;
    private String message;
    private List<DueTram> dueTrams;
    private LocalDateTime lastUpdate;
    private String displayId;
    private Station station;
    private Direction direction;

    public StationDepartureInfo(String displayId, String lineName, Direction direction, String stationPlatform,
                                Station station, String message, LocalDateTime lastUpdate) {
        this.displayId = displayId;
        this.lineName = lineName;
        this.direction = direction;
        this.stationPlatform = stationPlatform;
        this.station = station;
        this.message = message;
        this.lastUpdate = lastUpdate;
        dueTrams = new LinkedList<>();
    }

    public String getLineName() {
        return lineName;
    }

    public String getStationPlatform() {
        return stationPlatform;
    }

    public String getMessage() {
        return message;
    }

    public List<DueTram> getDueTrams() {
        return dueTrams;
    }

    public List<DueTram> getDueTramsWithinWindow(int minutes) {
        return dueTrams.stream().filter(dueTram -> dueTram.getWait()<=minutes).collect(Collectors.toList());
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void addDueTram(DueTram dueTram) {
        dueTrams.add(dueTram);
    }

    public boolean hasDueTram(DueTram dueTram) {
        return dueTrams.contains(dueTram);
    }

    public String getDisplayId() {
        return displayId;
    }

    public void clearMessage() {
        message="";
    }

    public String getLocation() {
        return station.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationDepartureInfo that = (StationDepartureInfo) o;
        return lineName.equals(that.lineName) &&
                stationPlatform.equals(that.stationPlatform) &&
                message.equals(that.message) &&
                dueTrams.equals(that.dueTrams) &&
                lastUpdate.equals(that.lastUpdate) &&
                displayId.equals(that.displayId) &&
                station.equals(that.station) &&
                direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineName, stationPlatform, message, dueTrams, lastUpdate, displayId, station, direction);
    }

    @Override
    public String toString() {
        return "StationDepartureInfo{" +
                "lineName='" + lineName + '\'' +
                ", stationPlatform='" + stationPlatform + '\'' +
                ", message='" + message + '\'' +
                ", dueTrams=" + dueTrams +
                ", lastUpdate=" + lastUpdate +
                ", displayId='" + displayId + '\'' +
                ", location='" + station + '\'' +
                ", direction=" + direction +
                '}';
    }
}
