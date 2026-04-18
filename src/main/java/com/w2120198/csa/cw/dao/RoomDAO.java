package com.w2120198.csa.cw.dao;

import com.w2120198.csa.cw.model.Room;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomDAO {

    private static final List<Room> ROOMS = Collections.synchronizedList(new ArrayList<>());

    public List<Room> getAll() {
        synchronized (ROOMS) {
            return new ArrayList<>(ROOMS);
        }
    }

    public Room getById(String id) {
        if (id == null) {
            return null;
        }
        synchronized (ROOMS) {
            for (Room room : ROOMS) {
                if (id.equals(room.getId())) {
                    return room;
                }
            }
            return null;
        }
    }

    public void add(Room room) {
        synchronized (ROOMS) {
            ROOMS.add(room);
        }
    }

    public void update(Room room) {
        synchronized (ROOMS) {
            for (int i = 0; i < ROOMS.size(); i++) {
                if (ROOMS.get(i).getId().equals(room.getId())) {
                    ROOMS.set(i, room);
                    return;
                }
            }
        }
    }

    public boolean delete(String id) {
        synchronized (ROOMS) {
            return ROOMS.removeIf(r -> r.getId().equals(id));
        }
    }
}
