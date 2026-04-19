package com.w2120198.csa.cw.service;

import com.w2120198.csa.cw.dao.RoomDAO;
import com.w2120198.csa.cw.exception.RoomNotEmptyException;
import com.w2120198.csa.cw.model.Room;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomService {

    private final RoomDAO roomDAO = new RoomDAO();

    public List<Room> listAll() {
        return roomDAO.getAll();
    }

    public Optional<Room> find(String id) {
        return Optional.ofNullable(roomDAO.getById(id));
    }

    public Room create(Room room) {
        // ignore what the client sent for sensorIds, it's filled in when sensors register
        room.setSensorIds(new ArrayList<>());
        roomDAO.add(room);
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
