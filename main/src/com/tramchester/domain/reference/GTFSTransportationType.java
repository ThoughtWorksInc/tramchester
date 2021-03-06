package com.tramchester.domain.reference;

import java.util.HashMap;
import java.util.Map;

public enum GTFSTransportationType {

    tram("0"),
    subway("1"),
    train("2"),
    bus("3"),
    ferry("4"),
    cableTram("5"),
    aerialLift("6"),
    funicular("7"),
    trolleyBus("11"),
    monorail("12"),

    // NOTE: These are not official types
    replacementBus("98"),
    unknown("99");

    private static final Map<String, GTFSTransportationType> textMap;

    static {
        textMap = new HashMap<>();
        GTFSTransportationType[] valid = GTFSTransportationType.values();
        for (GTFSTransportationType value : valid) {
            textMap.put(value.getText(), value);
        }
    }

    private final String text;

    GTFSTransportationType(String theText) {
        this.text = theText;
    }

    public static GTFSTransportationType parse(String routeType) {
        if (textMap.containsKey(routeType)) {
            return textMap.get(routeType);
        }
        return unknown;
    }

    private String getText() {
        return text;
    }
}
