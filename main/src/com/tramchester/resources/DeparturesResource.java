package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.repository.LiveDataSource;
import com.tramchester.repository.StationRepository;
import com.tramchester.services.SpatialService;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Api
@Path("/departures")
@Produces(MediaType.APPLICATION_JSON)
public class DeparturesResource {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesResource.class);

    private final SpatialService spatialService;
    private final LiveDataSource liveDataSource;
    private final DeparturesMapper departuresMapper;
    private ProvidesNotes providesNotes;
    private final StationRepository stationRepository;

    public DeparturesResource(SpatialService spatialService, LiveDataSource liveDataSource,
                              DeparturesMapper departuresMapper, ProvidesNotes providesNotes, StationRepository stationRepository) {
        this.spatialService = spatialService;
        this.liveDataSource = liveDataSource;
        this.departuresMapper = departuresMapper;
        this.providesNotes = providesNotes;
        this.stationRepository = stationRepository;
    }

    private LocalDateTime getLocalNow() {
        return ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalDateTime();
    }

    @GET
    @Timed
    @Path("/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close departures", response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestDepartures(@PathParam("lat") double lat, @PathParam("lon") double lon) {
        LocalDateTime localNow = getLocalNow();
        LatLong latLong = new LatLong(lat,lon);
        List<Station> nearbyStations = spatialService.getNearestStations(latLong);

        SortedSet<DepartureDTO> departuresDTO = departuresMapper.createDeparturesFor(nearbyStations);
        TramServiceDate queryDate = new TramServiceDate(localNow.toLocalDate());

        List<String> notes = providesNotes.createNotesForStations(queryDate, nearbyStations);
        DepartureListDTO departureList = new DepartureListDTO(departuresDTO, notes);

        return Response.ok(departureList).build();
    }

    @GET
    @Timed
    @Path("/station/{station}")
    @ApiOperation(value= "All departures from given station ID, notes included by default. " +
            "Control presence of notes using query param ?notes=1 or 0",
            response = DepartureListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getDepartureForStation(@PathParam("station") String stationId,
                                           @DefaultValue("1") @QueryParam("notes") String notesParam) {

        logger.info("Get departs for station " + stationId);
        boolean includeNotes = true;
        if (!notesParam.isEmpty()) {
            includeNotes = !notesParam.equals("0");
        }

        Optional<Station> maybeStation = stationRepository.getStation(stationId);

        if (maybeStation.isPresent()) {
            logger.info("Found stations, now find departures");
            Station station = maybeStation.get();

            Set<DepartureDTO> unsortedDueTrams = liveDataSource.departuresFor(station).stream().
                    map(StationDepartureInfo::getDueTrams).flatMap(Collection::stream).
                    filter(dueTram -> DeparturesMapper.DUE.equals(dueTram.getStatus())).
                    map(dueTram -> new DepartureDTO(station.getName(),dueTram))
                    .collect(Collectors.toSet());

            TramServiceDate queryDate = new TramServiceDate(LocalDate.now());

            List<String> notes = Collections.emptyList();
            if (includeNotes) {
                notes = providesNotes.createNotesForStations(queryDate, Collections.singletonList(station));
            }
            SortedSet<DepartureDTO> dueTrams = new TreeSet<>();
            dueTrams.addAll(unsortedDueTrams);
            DepartureListDTO departureListDTO = new DepartureListDTO(dueTrams, notes);

            return Response.ok(departureListDTO).build();
        }

        logger.warn("Unable to find station " + stationId);
        return Response.status(Response.Status.NOT_FOUND).build();

    }
}
