package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.domain.reference.KnownTramRoute.TheTraffordCentreCornbrook;
import static com.tramchester.testSupport.reference.RoutesForTesting.createTramRoute;
import static com.tramchester.testSupport.reference.TramStations.Intu;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteDTOTest {

    @Test
    void shouldUseRouteNameForEquality() {

        List<StationRefWithPosition> stations = new ArrayList<>();
        stations.add(new StationRefWithPosition(TramStations.of(Intu)));
        RouteDTO routeDTO = new RouteDTO(createTramRoute(TheTraffordCentreCornbrook), stations);

        assertEquals("METLRED:I:", routeDTO.getId());
        assertEquals("TheTraffordCentreCornbrook", routeDTO.getRouteName());
        assertEquals("Red Line", routeDTO.getShortName());
        assertEquals(TransportMode.Tram, routeDTO.getTransportMode());

        List<StationRefWithPosition> stationsDTO = routeDTO.getStations();
        assertEquals(1, stationsDTO.size());
        assertEquals(Intu.getId().forDTO(), stations.get(0).getId());

    }
}
