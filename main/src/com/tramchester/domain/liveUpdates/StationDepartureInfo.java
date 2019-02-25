package com.tramchester.domain.liveUpdates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializer;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class StationDepartureInfo {

    private String lineName;
    private String stationPlatform;
    private String message;
    private List<DueTram> dueTrams;
    private LocalDateTime lastUpdate;
    private String displayId;
    private String location;

    public StationDepartureInfo(String displayId, String lineName, String stationPlatform, String location,
                                String message, LocalDateTime lastUpdate) {
        this.displayId = displayId;
        this.lineName = lineName;
        this.stationPlatform = stationPlatform;
        this.location = location;
        this.message = message;
        this.lastUpdate = lastUpdate;
        dueTrams = new LinkedList<>();
    }

    public StationDepartureInfo() {
        // deserialisation
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

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @JsonIgnore
    public void addDueTram(DueTram dueTram) {
        dueTrams.add(dueTram);
    }

    public String getDisplayId() {
        return displayId;
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
                ", location='" + location + '\'' +
                '}';
    }

    public void clearMessage() {
        message="";
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationDepartureInfo that = (StationDepartureInfo) o;
        return Objects.equals(lineName, that.lineName) &&
                Objects.equals(stationPlatform, that.stationPlatform) &&
                Objects.equals(message, that.message) &&
                Objects.equals(dueTrams, that.dueTrams) &&
                Objects.equals(lastUpdate, that.lastUpdate) &&
                Objects.equals(displayId, that.displayId) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineName, stationPlatform, message, dueTrams, lastUpdate, displayId, location);
    }
}