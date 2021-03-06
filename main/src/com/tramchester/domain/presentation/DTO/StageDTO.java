package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StageDTO {
    private StationRefWithPosition firstStation;
    private StationRefWithPosition lastStation;
    private StationRefWithPosition actionStation;

    private boolean hasPlatform;
    private PlatformDTO platform;

    private LocalDate queryDate;
    private LocalDateTime firstDepartureTime;
    private LocalDateTime expectedArrivalTime;
    private int duration;

    private String headSign;

    private TransportMode mode;
    private int passedStops;
    private String action;

    private RouteRefDTO route;
    private String tripId;

    public StageDTO(StationRefWithPosition firstStation, StationRefWithPosition lastStation, StationRefWithPosition actionStation,
                    LocalDateTime firstDepartureTime, LocalDateTime expectedArrivalTime, int duration,
                    String headSign, TransportMode mode, int passedStops,
                    RouteRefDTO route, TravelAction action, LocalDate queryDate, String tripId) {
        this.firstStation = firstStation;
        this.lastStation = lastStation;
        this.actionStation = actionStation;
        this.hasPlatform = false;
        this.platform = null;
        this.firstDepartureTime = firstDepartureTime;
        this.expectedArrivalTime = expectedArrivalTime;
        this.duration = duration;
        this.headSign = headSign;
        this.mode = mode;
        this.passedStops = passedStops;
        this.route = route;
        this.action = action.toString();
        this.queryDate = queryDate;
        this.tripId = tripId;
    }

    public StageDTO(StationRefWithPosition firstStation, StationRefWithPosition lastStation, StationRefWithPosition actionStation,
                    PlatformDTO boardingPlatform, LocalDateTime firstDepartureTime, LocalDateTime expectedArrivalTime, int duration,
                    String headSign, TransportMode mode, int passedStops,
                    RouteRefDTO route, TravelAction action, LocalDate queryDate, String tripId) {
        this(firstStation, lastStation, actionStation, firstDepartureTime, expectedArrivalTime, duration, headSign, mode,
            passedStops, route, action, queryDate, tripId);

        this.hasPlatform = true;
        this.platform = boardingPlatform;
    }

    public StageDTO() {
        // deserialisation
    }

    public String getHeadSign() {
        return headSign;
    }

    public StationRefWithPosition getActionStation() {
        return actionStation;
    }

    public StationRefWithPosition getLastStation() {
        return lastStation;
    }

    public StationRefWithPosition getFirstStation() {
        return firstStation;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate getQueryDate() {
        return queryDate;
    }

    public int getDuration() {
        return duration;
    }

    public TransportMode getMode() {
        return mode;
    }

    public PlatformDTO getPlatform() {
        return platform;
    }

    // web site
    public boolean getHasPlatform() {
        return hasPlatform;
    }

    public int getPassedStops() {
        return passedStops;
    }

    public RouteRefDTO getRoute() {
        return route;
    }

    public String getAction() {
        return action;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public String getTripId() {
        return tripId;
    }

    @Override
    public String toString() {
        return "StageDTO{" +
                "firstStation=" + firstStation +
                ", lastStation=" + lastStation +
                ", actionStation=" + actionStation +
                ", hasPlatform=" + hasPlatform +
                ", platform=" + platform +
                ", queryDate=" + queryDate +
                ", firstDepartureTime=" + firstDepartureTime +
                ", expectedArrivalTime=" + expectedArrivalTime +
                ", duration=" + duration +
                ", headSign='" + headSign + '\'' +
                ", mode=" + mode +
                ", passedStops=" + passedStops +
                ", action='" + action + '\'' +
                ", route=" + route +
                ", tripId='" + tripId + '\'' +
                '}';
    }

}
