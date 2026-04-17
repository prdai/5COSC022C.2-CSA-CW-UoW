package com.w2120198.csa.cw.resource;

import com.w2120198.csa.cw.dao.GenericDAO;
import com.w2120198.csa.cw.dao.MockDatabase;
import com.w2120198.csa.cw.exception.SensorUnavailableException;
import com.w2120198.csa.cw.model.Sensor;
import com.w2120198.csa.cw.model.SensorReading;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Sub-resource handling the readings history for one specific sensor.
 * Instances are constructed by {@link SensorResource#readings(String)}
 * with the parent sensor already validated, so the methods here can
 * assume the context is non-null.
 */
public class SensorReadingResource {

    private final Sensor parent;
    private final GenericDAO<Sensor> sensorDAO;

    public SensorReadingResource(Sensor parent, GenericDAO<Sensor> sensorDAO) {
        this.parent = parent;
        this.sensorDAO = sensorDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorReading> getReadings() {
        List<SensorReading> history = MockDatabase.READINGS_BY_SENSOR.get(parent.getId());
        return (history != null) ? new ArrayList<>(history) : new ArrayList<>();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"errorMessage\":\"Request body is required.\",\"errorCode\":400}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (Sensor.STATUS_MAINTENANCE.equals(parent.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + parent.getId() + "' is under MAINTENANCE and cannot accept readings.");
        }
        reading.setId(UUID.randomUUID().toString());
        if (reading.getTimestamp() <= 0L) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        List<SensorReading> history = MockDatabase.READINGS_BY_SENSOR
                .computeIfAbsent(parent.getId(), k -> new ArrayList<>());
        synchronized (history) {
            history.add(reading);
        }

        // Side effect mandated by spec Part 4.2: the parent sensor's
        // currentValue must reflect the most recent reading immediately.
        parent.setCurrentValue(reading.getValue());
        sensorDAO.update(parent);

        URI location = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(location).entity(reading).build();
    }
}
