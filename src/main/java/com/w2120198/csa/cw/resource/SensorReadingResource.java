package com.w2120198.csa.cw.resource;

import com.w2120198.csa.cw.model.ErrorMessage;
import com.w2120198.csa.cw.model.SensorReading;
import com.w2120198.csa.cw.service.SensorReadingService;
import java.net.URI;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public class SensorReadingResource {

    private final String sensorId;
    private final SensorReadingService readingService = new SensorReadingService();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadings() {
        return readingService.historyFor(sensorId)
                .map(list -> Response.ok(list).build())
                .orElseGet(this::notFound);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        Optional<SensorReading> saved = readingService.record(sensorId, reading);
        if (!saved.isPresent()) {
            return notFound();
        }
        URI location = uriInfo.getAbsolutePathBuilder().path(saved.get().getId()).build();
        return Response.created(location).entity(saved.get()).build();
    }

    private Response notFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage("Sensor with id '" + sensorId + "' was not found.", 404))
                .build();
    }
}
