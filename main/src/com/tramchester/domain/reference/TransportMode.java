package com.tramchester.domain.reference;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.HasTransportModes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum TransportMode implements HasTransportMode {
    Bus((short)1),
    Tram((short)2),
    Train((short)3),
    Walk((short)4),
    Ferry((short)5),
    Subway((short)6),
    RailReplacementBus((short)7),

    Depart((short)50),
    Board((short)51),
    Connect((short)52),
    NotSet((short)53),
    Unknown((short)99);

    private static final Map<Short, TransportMode> theMap;

    static {
        theMap = new HashMap<>();
        for (int i = 0; i < values().length; i++) {
            TransportMode value = values()[i];
            theMap.put(value.number, value);
        }
    }

    private final short number;

    TransportMode(short number) {
        this.number = number;
    }

    public static TransportMode fromGTFS(GTFSTransportationType routeType) {
        switch (routeType) {
            case tram: return TransportMode.Tram;
            case bus: return TransportMode.Bus;
            case train: return TransportMode.Train;
            case ferry: return TransportMode.Ferry;
            case subway: return TransportMode.Subway;
            case replacementBus: return TransportMode.RailReplacementBus;
            default:
                throw new RuntimeException("Unexpected route type (check config?) " + routeType);
        }
    }

    public static boolean isTram(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Tram);
    }

    public static boolean isTram(HasTransportModes hasModes) {
        return hasModes.getTransportModes().contains(TransportMode.Tram);
    }

    public static boolean isBus(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Bus);
    }

    public static boolean isBus(HasTransportModes item) {
        return item.getTransportModes().contains(TransportMode.Bus);
    }

    public static boolean isTrain(HasTransportMode item) {
        return item.getTransportMode().equals(TransportMode.Train);
    }

    public static boolean isTrain(HasTransportModes item) {
        return item.getTransportModes().contains(TransportMode.Train);
    }

    public static Set<TransportMode> fromGTFS(Set<GTFSTransportationType> gtfsTransportationTypes) {
        Set<TransportMode> result = new HashSet<>();
        gtfsTransportationTypes.forEach(gtfsTransportationType -> result.add(fromGTFS(gtfsTransportationType)));
        return result;
    }

    public static TransportMode fromNumber(short number) {
        return theMap.get(number);
    }

    public static Set<TransportMode> fromNumbers(short[] numbers) {
        Set<TransportMode> result = new HashSet<>();
        for (short value : numbers) {
            result.add(theMap.get(value));
        }
        return result;
    }

    @Override
    public TransportMode getTransportMode() {
        return this;
    }

    public short getNumber() {
        return number;
    }
}
