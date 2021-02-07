package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class RouteTest {

    @Test
    void shouldHaveTramRoute() {
        Route route = new Route(StringIdFor.createId("idA"),"code","name", TestEnv.MetAgency(),
                TransportMode.Tram, RouteDirection.Inbound);
        Assertions.assertTrue(TransportMode.isTram(route));

        route = new Route(StringIdFor.createId("idB"),"code","name",
                new Agency(DataSourceID.TFGM(), "GMS", "agencyName"),
                TransportMode.Bus, RouteDirection.Outbound);
        Assertions.assertFalse(TransportMode.isTram(route));
    }

    @Test
    void shouldAddService() {
        Route  route = new Route(StringIdFor.createId("routeId"),"code","name", TestEnv.MetAgency(), TransportMode.Tram, RouteDirection.Inbound);

        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId", TestEnv.getTestRoute()));
        route.addService(new Service("serviceId2", TestEnv.getTestRoute()));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

    @Test
    void shouldAddHeadsign() {
        Route  route = new Route(StringIdFor.createId("id"),"code","name", TestEnv.MetAgency(), TransportMode.Tram, RouteDirection.Inbound);

        route.addHeadsign("hs1");
        route.addHeadsign("hs2");
        route.addHeadsign("hs1");

        Set<String> results = route.getHeadsigns();

        Assertions.assertEquals(2, results.size());
    }
}
