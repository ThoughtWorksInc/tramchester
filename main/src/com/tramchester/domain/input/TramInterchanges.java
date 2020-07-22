package com.tramchester.domain.input;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.Station;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TramInterchanges {

    private enum Interchanges {
        // official
        CORNBROOK("9400ZZMACRN"),
        ST_PETERS_SQUARE("9400ZZMASTP"),
        PIC_GARDENS("9400ZZMAPGD"),
        TRAF_BAR("9400ZZMATRA"),
        ST_WS_ROAD("9400ZZMASTW"),
        VICTORIA("9400ZZMAVIC"),
        DEANSGATE("9400ZZMAGMX"),
        PICCADILLY("9400ZZMAPIC"),
        // additional route swap points, needed for journeys when restrict change-over points
        SHAW_AND_CROMPTON("9400ZZMASHA"),
        HARBOUR_CITY("9400ZZMAHCY");

        private final String stationId;

        Interchanges(String stationId) {
            this.stationId = stationId;
        }
    }

    private static final Set<IdFor<Station>> ids;

    static {
        ids = new HashSet<>();
        Arrays.asList(Interchanges.values()).forEach(interchange -> ids.add(IdFor.createId(interchange.stationId)));
    }

    public static boolean hasId(IdFor<Station> stationId) {
        return ids.contains(stationId);
    }

    public static boolean has(Station station) {
        return hasId(station.getId());
    }

    public static Set<IdFor<Station>> stations() {
        return ids;
    }
}
