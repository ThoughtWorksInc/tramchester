package com.tramchester.domain;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Interchanges {

    public static final String CORNBROOK = "9400ZZMACRN";
    // public static final String ST_PETERS_SQUARE = "9400ZZMASTP"; // closed until August 2016
    public static final String PIC_GARDENS = "9400ZZMAPGD";
    public static final String TRAF_BAR = "9400ZZMATRA";
    public static final String ST_WS_ROAD = "9400ZZMASTW";
    public static final String VICTORIA = "9400ZZMAVIC";
    public static final String DEANSGATE = "9400ZZMAGMX";
    // not an official interchanges but several services terminate/branch here
    public static final String PICCADILLY = "9400ZZMAPIC";
    public static final String HARBOURCITY = "9400ZZMAHCY";


    private static final Set<String> interchanges = new HashSet<>(Arrays.asList(CORNBROOK,
            // ST_PETERS_SQUARE, closed until 2016
            PIC_GARDENS,
            TRAF_BAR,
            ST_WS_ROAD,
            VICTORIA,
            PICCADILLY,
            HARBOURCITY,
            DEANSGATE));

    public static boolean has(Station station) {
        // TODO changes for Buses
        return interchanges.contains(station.getId());
    }

    public static Set<String> stations() {
        // TODO changes for Buses
        return interchanges;
    }
}