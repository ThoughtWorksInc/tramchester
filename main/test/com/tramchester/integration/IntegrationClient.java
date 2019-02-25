package com.tramchester.integration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.junit.Assert.assertEquals;


public class IntegrationClient {

    public static Response getResponse(IntegrationTestRun testRule, String endPoint, Optional<Cookie> cookie) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:" + testRule.getLocalPort() + "/api/"+ endPoint);
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
        cookie.ifPresent(theCookie -> builder.cookie(theCookie));
        Response responce = builder.get();
        assertEquals(200, responce.getStatus());
        return responce;
    }
}
