package com.tramchester.dataimport.data;

import java.time.LocalDate;

// holds exceptions to main calendar
public class CalendarDateData {
    // https://developers.google.com/transit/gtfs/reference#calendar_datestxt
    public static final int ADDED = 1;
    public static final int REMOVED = 2;

    private final String serviceId;
    private final LocalDate date;
    private final int exceptionType;

    public CalendarDateData(String serviceId, LocalDate date, int exceptionType) {

        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType = exceptionType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getExceptionType() {
        return exceptionType;
    }
}