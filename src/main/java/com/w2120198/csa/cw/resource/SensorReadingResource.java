package com.w2120198.csa.cw.resource;

import com.w2120198.csa.cw.model.ErrorMessage;
import com.w2120198.csa.cw.model.SensorReading;
import com.w2120198.csa.cw.service.SensorReadingService;
import java.net.URI;
import java.util.List;
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
        List<SensorReading> history = readingService.historyFor(sensorId).orElse(null);
        if (history == null) {
            return notFound();
        }
        return Response.ok(history).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        SensorReading saved = readingService.record(sensorId, reading).orElse(null);
        if (saved == null) {
            return notFound();
        }
        URI location = uriInfo.getAbsolutePathBuilder().path(saved.getId()).build();
        return Response.created(location).entity(saved).build();
    }

    private Response notFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage("Sensor with id '" + sensorId + "' was not found.", 404))
                .build();
    }
}
