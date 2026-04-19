package com.w2120198.csa.cw.service;

import com.w2120198.csa.cw.dao.RoomDAO;
import com.w2120198.csa.cw.exception.RoomNotEmptyException;
import com.w2120198.csa.cw.model.Room;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class RoomService {

    private final RoomDAO roomDAO = new RoomDAO();

    public List<Room> listAll() {
        return roomDAO.getAll();
    }

    public Optional<Room> find(String id) {
        return Optional.ofNullable(roomDAO.getById(id));
    }

    public Room create(Room room) throws WebApplicationException {
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            throw new WebApplicationException("Room id is required.", Response.Status.BAD_REQUEST);
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            throw new WebApplicationException("Room name is required.", Response.Status.BAD_REQUEST);
        }
        if (room.getCapacity() < 0) {
            throw new WebApplicationException("Room capacity cannot be negative.", Response.Status.BAD_REQUEST);
        }
        synchronized (RoomDAO.LINK_LOCK) {
            if (roomDAO.getById(room.getId()) != null) {
                throw new WebApplicationException(
                        "Room with id '" + room.getId() + "' already exists.",
                        Response.Status.CONFLICT);
            }
            // ignore what the client sent for sensorIds, it's filled in when sensors register
            room.setSensorIds(new ArrayList<>());
            roomDAO.add(room);
        }
        return room;
    }

    public boolean delete(String id) throws RoomNotEmptyException {
        // lock covers both the empty check and the delete so a concurrent sensor create can't slip in
        synchronized (RoomDAO.LINK_LOCK) {
            Room existing = roomDAO.getById(id);
            if (existing == null) {
                return false;
            }
            if (existing.getSensorIds() != null && !existing.getSensorIds().isEmpty()) {
                throw new RoomNotEmptyException(
                        "Room '" + id + "' still has " + existing.getSensorIds().size()
                        + " active sensor(s) assigned and cannot be deleted.");
            }
            roomDAO.delete(id);
            return true;
        }
    }
}
