package com.w2120198.csa.cw.resource;

import com.w2120198.csa.cw.model.ErrorMessage;
import com.w2120198.csa.cw.model.Room;
import com.w2120198.csa.cw.service.RoomService;
import java.net.URI;
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

@Path("/rooms")
public class SensorRoom {

    private final RoomService roomService = new RoomService();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Room> getAllRooms() {
        return roomService.listAll();
    }

    @GET
    @Path("/{roomId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoomById(@PathParam("roomId") String roomId) {
        return roomService.find(roomId)
                .map(room -> Response.ok(room).build())
                .orElseGet(() -> notFound(roomId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        Room saved = roomService.create(room);
        URI location = uriInfo.getAbsolutePathBuilder().path(saved.getId()).build();
        return Response.created(location).entity(saved).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        if (!roomService.delete(roomId)) {
            return notFound(roomId);
        }
        return Response.noContent().build();
    }

    private Response notFound(String roomId) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage("Room with id '" + roomId + "' was not found.", 404))
                .build();
    }
}
