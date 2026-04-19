package com.w2120198.csa.cw.service;

import com.w2120198.csa.cw.dao.RoomDAO;
import com.w2120198.csa.cw.dao.SensorDAO;
import com.w2120198.csa.cw.exception.LinkedResourceNotFoundException;
import com.w2120198.csa.cw.model.Room;
import com.w2120198.csa.cw.model.Sensor;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class SensorService {

    private final SensorDAO sensorDAO = new SensorDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    public List<Sensor> listAll() {
        return sensorDAO.getAll();
    }

    public List<Sensor> listByType(String type) {
        List<Sensor> filtered = new ArrayList<>();
        for (Sensor sensor : sensorDAO.getAll()) {
            if (type.equalsIgnoreCase(sensor.getType())) {
                filtered.add(sensor);
            }
        }
        return filtered;
    }

    public Sensor create(Sensor sensor) throws LinkedResourceNotFoundException, WebApplicationException {
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            throw new WebApplicationException("Sensor id is required.", Response.Status.BAD_REQUEST);
        }
        if (sensor.getType() == null || sensor.getType().trim().isEmpty()) {
            throw new WebApplicationException("Sensor type is required.", Response.Status.BAD_REQUEST);
        }
        // lock covers the parent check through the sensorIds append so a concurrent room delete can't orphan the sensor
        synchronized (RoomDAO.LINK_LOCK) {
            if (sensorDAO.getById(sensor.getId()) != null) {
                throw new WebApplicationException(
                        "Sensor with id '" + sensor.getId() + "' already exists.",
                        Response.Status.CONFLICT);
            }
            Room parent = roomDAO.getById(sensor.getRoomId());
            if (parent == null) {
                throw new LinkedResourceNotFoundException(
                        "Sensor cannot be created: roomId '" + sensor.getRoomId() + "' does not exist.");
            }
            sensorDAO.add(sensor);
            parent.getSensorIds().add(sensor.getId());
            roomDAO.update(parent);
            return sensor;
        }
    }
}
