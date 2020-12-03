package com.tramchester.testSupport;


import com.tramchester.domain.Agency;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.reference.KnownRoute;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import org.jetbrains.annotations.NotNull;

import static com.tramchester.domain.reference.KnownRoute.*;
import static java.lang.String.format;

public class RoutesForTesting {
    // TRAM

    // TODO Lockdown route 1 Gone during lockdown changes
    @Deprecated
    public static final Route ALTY_TO_BURY = createTramRoute("1", RouteDirection.Inbound, "Alty to Bury");
    @Deprecated
    public static final Route BURY_TO_ALTY = createTramRoute("1", RouteDirection.Outbound, "Bury to Alty");

    // BUS
    public static final Route ALTY_TO_STOCKPORT = new Route(IdFor.createId("GMS: 11A:I:"), "11A", "Alty to Stockport",
        new Agency("GMS", "agencyName"), TransportMode.Bus, RouteDirection.Inbound);
    public static final Route ALTY_TO_STOCKPORT_WBT = new Route(IdFor.createId("WBT:5A:I:"), "5A", "Alty to Stockport",
            new Agency("WBT", "Warrington's Own Buses"), TransportMode.Bus, RouteDirection.Inbound);

    public static Route createTramRoute(KnownRoute knownRoute) {
        return new Route(knownRoute.getId(), knownRoute.number(), knownRoute.name(), TestEnv.MetAgency(),
                TransportMode.Tram, knownRoute.direction());
    }

    @Deprecated
    private static Route createTramRoute(String shortName, RouteDirection direction, String longName) {
        String idText = format("MET:%s%s", shortName, direction.getSuffix());
        return new Route(IdFor.createId(idText), shortName, longName,
                TestEnv.MetAgency(), TransportMode.Tram, direction);
    }


}
