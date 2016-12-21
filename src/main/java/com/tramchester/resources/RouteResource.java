package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.repository.RoutesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
public class RouteResource {
    private static final Logger logger = LoggerFactory.getLogger(RouteResource.class);

    private RoutesRepository repository;

    public RouteResource(RoutesRepository repository) {
        this.repository = repository;
    }

    @GET
    @Timed
    public Response getAll() {
        logger.info("getAll routes");
        List<RouteDTO> routes = repository.getAllRoutes();

        Response response = Response.ok(routes).build();
        return response;
    }

}
