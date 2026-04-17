package com.w2120198.csa.cw.resource;

import com.w2120198.csa.cw.dao.GenericDAO;
import com.w2120198.csa.cw.dao.MockDatabase;
import com.w2120198.csa.cw.exception.DataNotFoundException;
import com.w2120198.csa.cw.exception.LinkedResourceNotFoundException;
import com.w2120198.csa.cw.model.Room;
import com.w2120198.csa.cw.model.Sensor;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Sensor resource. Accepts an optional {@code type} query parameter
 * on the collection endpoint for filtered retrieval, and delegates
 * reading history to a sub-resource locator.
 */
@Path("/sensors")
public class SensorResource {

    private final GenericDAO<Sensor> sensorDAO = new GenericDAO<>(MockDatabase.SENSORS);
    private final GenericDAO<Room> roomDAO = new GenericDAO<>(MockDatabase.ROOMS);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        List<Sensor> all = sensorDAO.getAll();
        if (type == null || type.trim().isEmpty()) {
            return all;
        }
        List<Sensor> filtered = new ArrayList<>();
        for (Sensor sensor : all) {
            if (type.equalsIgnoreCase(sensor.getType())) {
                filtered.add(sensor);
            }
        }
        return filtered;
    }

    @GET
    @Path("/{sensorId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Sensor getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorDAO.getById(sensorId);
        if (sensor == null) {
            throw new DataNotFoundException("Sensor with id '" + sensorId + "' was not found.");
        }
        return sensor;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()
                || sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"errorMessage\":\"Sensor id and roomId are required.\",\"errorCode\":400}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        Room parent = roomDAO.getById(sensor.getRoomId());
        if (parent == null) {
            throw new LinkedResourceNotFoundException(
                    "Sensor cannot be created: roomId '" + sensor.getRoomId() + "' does not exist.");
        }
        if (sensorDAO.getById(sensor.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"errorMessage\":\"Sensor id already registered.\",\"errorCode\":409}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus(Sensor.STATUS_ACTIVE);
        }
        sensorDAO.add(sensor);
        parent.getSensorIds().add(sensor.getId());
        roomDAO.update(parent);

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    /**
     * Sub-resource locator for historical readings of a given sensor.
     * Returning a dedicated resource class keeps the reading logic out
     * of this controller and narrows each class to a single concern.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor parent = sensorDAO.getById(sensorId);
        if (parent == null) {
            throw new DataNotFoundException("Sensor with id '" + sensorId + "' was not found.");
        }
        return new SensorReadingResource(parent, sensorDAO);
    }
}
