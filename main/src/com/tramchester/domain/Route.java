package com.tramchester.domain;

import java.util.HashSet;
import java.util.Set;

public class Route {
    public static final String METROLINK = "MET";

    private final String id;
    private final String code;
    private final String name;
    private final String agency;

    private final Set<Service> services;
    private final Set<String> headsigns;

    public Route(String id, String code, String name, String agency) {
        this.id = id.intern();
        this.code = code.intern();
        this.name = name.intern();
        this.agency = agency.intern();
        services = new HashSet<>();
        headsigns = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Service> getServices() {
        return services;
    }

    public void addService(Service service) {
        services.add(service);
    }

    public String getAgency() {
        return agency;
    }

    public Set<String> getHeadsigns() {
        return headsigns;
    }

    public void addHeadsign(String headsign) {
        headsigns.add(headsign);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Route route = (Route) o;

        return !(id != null ? !id.equals(route.id) : route.id != null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public boolean isTram() {
        return agency.equals(METROLINK);
    }

    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", agency='" + agency + '\'' +
                '}';
    }
}
