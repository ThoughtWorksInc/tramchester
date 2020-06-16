package com.tramchester.domain;


import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static java.lang.String.format;

public class Service implements HasId {

    private final String serviceId;
    private final Route route;
    private final Set<Trip> trips;
    private TramServiceDate startDate;
    private TramServiceDate endDate;

    private final Set<DayOfWeek> days;
    private final Set<LocalDate> additional;
    private final Set<LocalDate> removed;

    private TramTime earliestDepart;
    private TramTime latestDepart;

    public Service(String serviceId, Route route) {
        this.serviceId = serviceId.intern();
        this.route = route;
        this.trips = new LinkedHashSet<>();
        this.days = new HashSet<>();
        this.additional = new HashSet<>();
        this.removed = new HashSet<>();
        earliestDepart = null;
        latestDepart = null;
    }

    public String getId() {
        return serviceId;
    }

    public Set<Trip> getTrips() {
        return trips;
    }

    public void addTrip(Trip trip) {
        trips.add(trip); // stop population not done at this stage, see updateTimings
    }

    public void updateTimings() {
        trips.forEach(this::updateEarliestAndLatest);
    }

    private void updateEarliestAndLatest(Trip trip) {
        TramTime tripEarliest = trip.earliestDepartTime();
        if (earliestDepart==null) {
            earliestDepart = tripEarliest;
        } else if (tripEarliest.isBefore(earliestDepart)) {
            earliestDepart = tripEarliest;
        }

        TramTime tripLatest = trip.latestDepartTime();
        if (latestDepart==null) {
            latestDepart = tripLatest;
        } else if (tripLatest.isAfter(latestDepart)) {
            latestDepart = tripLatest;
        }
    }

    public void setDays(boolean monday, boolean tuesday, boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday) {
        maybeDay(monday, DayOfWeek.MONDAY);
        maybeDay(tuesday, DayOfWeek.TUESDAY);
        maybeDay(wednesday, DayOfWeek.WEDNESDAY);
        maybeDay(thursday, DayOfWeek.THURSDAY);
        maybeDay(friday, DayOfWeek.FRIDAY);
        maybeDay(saturday, DayOfWeek.SATURDAY);
        maybeDay(sunday, DayOfWeek.SUNDAY);
    }

    private void maybeDay(boolean flag, DayOfWeek dayOfWeek) {
        if (flag) {
            days.add(dayOfWeek);
        }
    }

    @Override
    public String toString() {
        return "Service{" +
                "serviceId='" + serviceId + '\'' +
                ", route=" + route +
                ", trips=" + HasId.asIds(trips) +
                ", days=" + days +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", additional=" + additional +
                ", removed=" + removed +
                ", earliestDepart=" + earliestDepart +
                ", latestDepart=" + latestDepart +
                '}';
    }

    public Route getRoute() {
        return route;
    }

    public String getRouteId() {
        return route.getId();
    }

    public void setServiceDateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = new TramServiceDate(startDate);
        this.endDate = new TramServiceDate(endDate);
    }

    public boolean HasMissingDates() {
        if (!additional.isEmpty()) {
            return false;
        }
        if (startDate==null || endDate==null) {
            return true;
        }
        if (startDate.getDate().equals(LocalDate.MIN) && endDate.getDate().equals(LocalDate.MAX)) {
            return true;
        }
        return days.isEmpty();
    }

    public void addExceptionDate(LocalDate exceptionDate, int exceptionType) {
        if (exceptionType==CalendarDateData.ADDED) {
            additional.add(exceptionDate);
        } else if (exceptionType==CalendarDateData.REMOVED) {
            removed.add(exceptionDate);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return serviceId.equals(service.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }

    public boolean operatesOn(LocalDate queryDate) {
        if (additional.contains(queryDate)) {
            return true;
        }
        if (removed.contains(queryDate)) {
            return false;
        }

        LocalDate begin = startDate.getDate();
        LocalDate end = endDate.getDate();
        if  (queryDate.isAfter(begin) && queryDate.isBefore(end)) {
            return days.contains(queryDate.getDayOfWeek());
        }
        if (queryDate.equals(begin) || queryDate.equals(end)) {
            return true;
        }
        return false;
    }

    public TramTime earliestDepartTime() {
        return earliestDepart;
    }

    public TramTime latestDepartTime() {
        return latestDepart;
    }

    public void summariseDates(PrintStream printStream) {
        printStream.println(format("starts %s ends %s days %s",
                startDate.toDateString(), endDate.toDateString(), reportDays()));
        printStream.println(format("Earliest: %s Latest: %s", earliestDepartTime().toPattern(), latestDepartTime().toPattern()));
        if (!additional.isEmpty()) {
            printStream.println("Additional on: " + additional.toString());
        }
        if (!removed.isEmpty()) {
            printStream.println("Not running on: " + removed.toString());
        }
    }

    private String reportDays() {
        StringBuilder found = new StringBuilder();
        for (int i = 0; i < DayOfWeek.values().length; i++) {
            if (days.contains(DayOfWeek.values()[i])) {
                if (found.length()>0) {
                    found.append(",");
                }
                found.append(DayOfWeek.values()[i].name());
            }
        }
        if (found.length()==0) {
            return "SPECIAL";
        }
        return found.toString();
    }

}
