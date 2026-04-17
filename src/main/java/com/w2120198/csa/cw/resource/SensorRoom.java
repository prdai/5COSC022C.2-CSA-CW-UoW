package com.w2120198.csa.cw.resource;

import com.w2120198.csa.cw.dao.GenericDAO;
import com.w2120198.csa.cw.dao.MockDatabase;
import com.w2120198.csa.cw.exception.DataNotFoundException;
import com.w2120198.csa.cw.exception.RoomNotEmptyException;
import com.w2120198.csa.cw.model.Room;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Room resource. Class name kept as {@code SensorRoom} to match the
 * coursework specification (Part 2.1). Handles collection and detail
 * views plus create and delete; updates are out of scope for the spec.
 */
@Path("/rooms")
public class SensorRoom {

    private final GenericDAO<Room> roomDAO = new GenericDAO<>(MockDatabase.ROOMS);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Room> getAllRooms() {
        return roomDAO.getAll();
    }

    @GET
    @Path("/{roomId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Room getRoomById(@PathParam("roomId") String roomId) {
        Room room = roomDAO.getById(roomId);
        if (room == null) {
            throw new DataNotFoundException("Room with id '" + roomId + "' was not found.");
        }
        return room;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"errorMessage\":\"Room id is required.\",\"errorCode\":400}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (roomDAO.getById(room.getId()) != null) {
            // RoomNotEmptyException is reserved for the "still has sensors"
            // case (Part 5.1). A duplicate id is a different 409 scenario,
            // handled inline to keep the exception hierarchy unambiguous.
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"errorMessage\":\"Room id '" + room.getId()
                            + "' is already registered.\",\"errorCode\":409}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        // sensorIds are maintained server-side as sensors are registered.
        room.setSensorIds(new ArrayList<>());
        roomDAO.add(room);

        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room existing = roomDAO.getById(roomId);
        if (existing == null) {
            throw new DataNotFoundException("Room with id '" + roomId + "' was not found.");
        }
        if (existing.getSensorIds() != null && !existing.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' still has " + existing.getSensorIds().size()
                    + " active sensor(s) assigned and cannot be deleted.");
        }
        roomDAO.delete(roomId);
        return Response.noContent().build();
    }
}
